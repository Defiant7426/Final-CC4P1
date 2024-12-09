import requests
import threading
import time
import json
from flask import Flask, request, jsonify

app = Flask(__name__)

class RaftRole:
    FOLLOWER = "FOLLOWER"
    CANDIDATE = "CANDIDATE"
    LEADER = "LEADER"

class RaftNode:
    def __init__(self, port, peers):
        self.port = port
        self.peers = peers  # Lista de URLs de los peers
        self.role = RaftRole.FOLLOWER
        self.currentTerm = 0
        self.votedFor = None
        self.log = []
        self.leaderId = None
        self.lastHeartbeat = time.time()
        self.lock = threading.RLock()

        self.election_timeout = 3.0  # Tiempo en segundos
        self.heartbeat_interval = 1.0  # Tiempo en segundos

        print(f"[{self._current_time()}] [Nodo {self.port}] Inicializado como FOLLOWER con peers: {self.peers}")
        self.start_follower_timeout()

    def _current_time(self):
        return time.strftime("%d/%b/%Y %H:%M:%S", time.localtime())

    def start_follower_timeout(self):
        def follower_check():
            while True:
                time.sleep(1)
                with self.lock:
                    print(f"[{self._current_time()}] [Nodo {self.port}] Verificando seguidor...")
                    if self.role == RaftRole.FOLLOWER and (time.time() - self.lastHeartbeat > self.election_timeout):
                        print(f"[{self._current_time()}] [Nodo {self.port}] Temporizador de seguidor expirado.")
                        self.start_election()

        t = threading.Thread(target=follower_check, daemon=True)
        t.start()

    def start_election(self):
        with self.lock:
            self.role = RaftRole.CANDIDATE
            self.currentTerm += 1
            self.votedFor = f"http://127.0.0.1:{self.port}"  # ID único
            majority = (len(self.peers) // 2) + 1
            print(f"[{self._current_time()}] [Nodo {self.port}] Iniciando elección para term {self.currentTerm}. Buscando mayoría: {majority}")

        votesReceived = 1  # Cuenta el voto propio
        anyPeerAvailable = False

        def request_vote_from_peer(peer):
            nonlocal votesReceived, anyPeerAvailable
            try:
                body = {"term": self.currentTerm, "candidateId": self.votedFor}
                r = requests.post(peer + "/requestVote", json=body, timeout=2)
                if r.status_code == 200:
                    resp = r.json()
                    if resp.get("voteGranted", False):
                        with self.lock:
                            votesReceived += 1
                            anyPeerAvailable = True
                            print(f"[{self._current_time()}] [Nodo {self.port}] Voto recibido de {peer}. Total votos: {votesReceived}")
            except Exception as e:
                print(f"[{self._current_time()}] [Nodo {self.port}] Error al solicitar voto de {peer}: {e}")

        threads = []
        for peer in self.peers:
            # Pasar correctamente el argumento 'peer' usando args
            t = threading.Thread(target=request_vote_from_peer, args=(peer,), daemon=True)
            t.start()
            threads.append(t)

        for t in threads:
            t.join()

        with self.lock:
            if not anyPeerAvailable:
                print(f"[{self._current_time()}] [Nodo {self.port}] No hay otros nodos disponibles. Convirtiéndose en líder por defecto.")
                self.become_leader()
            elif votesReceived >= majority:
                print(f"[{self._current_time()}] [Nodo {self.port}] Ganó la elección con {votesReceived} votos.")
                self.become_leader()
            else:
                print(f"[{self._current_time()}] [Nodo {self.port}] Perdió la elección con {votesReceived} votos.")
                self.role = RaftRole.FOLLOWER

    def become_leader(self):
        self.role = RaftRole.LEADER
        self.leaderId = f"http://127.0.0.1:{self.port}"
        self.lastHeartbeat = time.time()
        print(f"[{self._current_time()}] [Nodo {self.port}] Se ha convertido en LEADER para term {self.currentTerm}. Iniciando heartbeats.")
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
                body = {"term": self.currentTerm, "leaderId": self.leaderId, "entries": []}
                r = requests.post(p + "/appendEntries", json=body, timeout=1)
                if r.status_code == 200:
                    print(f"[{self._current_time()}] [Nodo {self.port}] Heartbeat enviado a {p}")
            except requests.exceptions.Timeout:
                print(f"[{self._current_time()}] [Nodo {self.port}] Heartbeat timeout con {p}")
            except Exception as e:
                print(f"[{self._current_time()}] [Nodo {self.port}] Error al enviar heartbeat a {p}: {e}")

    def handle_append_entries(self, term, leaderId):
        with self.lock:
            if term >= self.currentTerm:
                if term > self.currentTerm or self.leaderId != leaderId:
                    self.leaderId = leaderId
                    self.role = RaftRole.FOLLOWER
                    self.currentTerm = term
                    self.votedFor = None
                    print(f"[{self._current_time()}] [Nodo {self.port}] Recibió appendEntries de {leaderId} con term {term}. Convirtiéndose en FOLLOWER.")
                self.lastHeartbeat = time.time()
            else:
                print(f"[{self._current_time()}] [Nodo {self.port}] Recibió appendEntries con term {term} menor al actual {self.currentTerm}. Ignorando.")

    def handle_request_vote(self, term, candidateId):
        with self.lock:
            if term > self.currentTerm:
                print(f"[{self._current_time()}] [Nodo {self.port}] Term {term} recibido es mayor que el actual {self.currentTerm}. Actualizando term y convirtiéndose en FOLLOWER.")
                self.currentTerm = term
                self.votedFor = None
                self.role = RaftRole.FOLLOWER

            voteGranted = False
            if (self.votedFor is None or self.votedFor == candidateId) and term >= self.currentTerm:
                self.votedFor = candidateId
                voteGranted = True
                print(f"[{self._current_time()}] [Nodo {self.port}] Votando por {candidateId} en term {term}")
            else:
                print(f"[{self._current_time()}] [Nodo {self.port}] No vota por {candidateId} en term {term}. Votado por: {self.votedFor}")

            return {"voteGranted": voteGranted}

    def append_log_entry(self, command):
        with self.lock:
            entry = {"term": self.currentTerm, "command": command}
            self.log.append(entry)
            print(f"[{self._current_time()}] [Nodo {self.port}] Añadida entrada al log: {entry}")
            return len(self.log) - 1

    def replicate_log_entry(self, index, command):
        def replicate():
            with self.lock:
                if self.role != RaftRole.LEADER:
                    print(f"[{self._current_time()}] [Nodo {self.port}] No es líder. No puede replicar entradas.")
                    return
            for p in self.peers:
                try:
                    body = {
                        "term": self.currentTerm,
                        "leaderId": self.leaderId,
                        "index": index,
                        "entry": command
                    }
                    r = requests.post(p + "/replicateEntry", json=body, timeout=2)
                    if r.status_code == 200:
                        print(f"[{self._current_time()}] [Nodo {self.port}] Replicada entrada a {p}")
                except Exception as e:
                    print(f"[{self._current_time()}] [Nodo {self.port}] Error al replicar entrada a {p}: {e}")

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
            print(f"[{self._current_time()}] [Nodo {self.port}] No hay líder actual para redirigir la solicitud.")
            return -1
        try:
            body = {"entry": command}
            r = requests.post(leader + "/appendLogEntry", json=body, timeout=2)
            if r.status_code == 200:
                print(f"[{self._current_time()}] [Nodo {self.port}] Redirigido a líder {leader} para añadir entrada.")
                return int(r.text)
            else:
                print(f"[{self._current_time()}] [Nodo {self.port}] Error al redirigir a líder {leader}: Status {r.status_code}")
                return -1
        except Exception as e:
            print(f"[{self._current_time()}] [Nodo {self.port}] Error al redirigir a líder {leader}: {e}")
            return -1

    def get_status(self):
        with self.lock:
            status = f"Role: {self.role}, Term: {self.currentTerm}, Leader: {self.leaderId}, LogSize: {len(self.log)}"
            print(f"[{self._current_time()}] [Nodo {self.port}] Estado actual: {status}")
            return status