import requests
import threading
import time
import json

class RaftRole:
    FOLLOWER = "FOLLOWER"
    CANDIDATE = "CANDIDATE"
    LEADER = "LEADER"

class RaftNode:
    def __init__(self, port, peers):
        self.port = port
        self.peers = peers
        self.role = RaftRole.FOLLOWER
        self.currentTerm = 0
        self.votedFor = None
        self.log = []
        self.leaderId = None
        self.lastHeartbeat = time.time()
        self.lock = threading.Lock()

        self.election_timeout = 3.0
        self.heartbeat_interval = 1.0

        self.start_follower_timeout()

    def start_follower_timeout(self):
        def follower_check():
            while True:
                time.sleep(1)
                with self.lock:
                    if self.role == RaftRole.FOLLOWER and (time.time() - self.lastHeartbeat > self.election_timeout):
                        self.start_election()

        t = threading.Thread(target=follower_check, daemon=True)
        t.start()

    def start_election(self):
        with self.lock:
            print("RAFT: Iniciando elección")
            self.role = RaftRole.CANDIDATE
            self.currentTerm += 1
            self.votedFor = "self"
            votesGranted = 1
            majority = (len(self.peers)//2)+1

        for peer in self.peers:
            if self.ask_for_vote(peer):
                votesGranted += 1

        with self.lock:
            if votesGranted >= majority:
                print("RAFT: Elección ganada")
                self.become_leader()
            else:
                print("RAFT: Elección perdida")
                self.role = RaftRole.FOLLOWER

    def ask_for_vote(self, peer):
        try:
            body = {"term": self.currentTerm, "candidateId": "self"}
            r = requests.post(peer+"/requestVote", json=body, timeout=2)
            if r.status_code == 200:
                resp = r.json()
                return resp.get("voteGranted", False)
        except:
            pass
        return False

    def become_leader(self):
        self.role = RaftRole.LEADER
        self.leaderId = f"http://127.0.0.1:{self.port}"
        self.start_heartbeats()

    def start_heartbeats(self):
        def send_heartbeats():
            while True:
                time.sleep(self.heartbeat_interval)
                with self.lock:
                    if self.role != RaftRole.LEADER:
                        break
                    self.send_heartbeats_to_peers()

        t = threading.Thread(target=send_heartbeats, daemon=True)
        t.start()

    def send_heartbeats_to_peers(self):
        for p in self.peers:
            try:
                body = {"term": self.currentTerm, "leaderId": self.leaderId, "entries":[]}
                requests.post(p+"/appendEntries", json=body, timeout=1)
            except:
                pass

    def handle_append_entries(self, term, leaderId):
        with self.lock:
            if term >= self.currentTerm:
                self.currentTerm = term
                self.leaderId = leaderId
                self.role = RaftRole.FOLLOWER
                self.lastHeartbeat = time.time()

    def handle_request_vote(self, term, candidateId):
        with self.lock:
            if term > self.currentTerm:
                self.currentTerm = term
                self.votedFor = None
                self.role = RaftRole.FOLLOWER

            if self.votedFor is None or self.votedFor == candidateId:
                self.votedFor = candidateId
                return True
            return False

    def append_log_entry(self, command):
        with self.lock:
            entry = {"term": self.currentTerm, "command": command}
            self.log.append(entry)
            return len(self.log)-1

    def replicate_log_entry(self, index, command):
        # Aquí se enviaría el comando a los seguidores
        def replicate():
            with self.lock:
                if self.role != RaftRole.LEADER:
                    return
            for p in self.peers:
                try:
                    body = {
                        "term": self.currentTerm,
                        "leaderId": self.leaderId,
                        "index": index,
                        "entry": command
                    }
                    requests.post(p+"/replicateEntry", json=body, timeout=2)
                except:
                    pass
        t = threading.Thread(target=replicate, daemon=True)
        t.start()

    def is_leader(self):
        with self.lock:
            return self.role == RaftRole.LEADER

    def get_leader(self):
        with self.lock:
            return self.leaderId

    def redirect_to_leader(self, command):
        leader = self.get_leader()
        if leader is None:
            return -1
        try:
            body = {"entry": command}
            r = requests.post(leader+"/appendLogEntry", json=body, timeout=2)
            if r.status_code == 200:
                return int(r.text)
            else:
                return -1
        except:
            return -1

    def get_status(self):
        with self.lock:
            return f"Role: {self.role}, Term: {self.currentTerm}, Leader: {self.leaderId}, LogSize: {len(self.log)}"


