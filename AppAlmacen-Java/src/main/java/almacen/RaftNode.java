package almacen;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;

public class RaftNode {
    private final String[] peers;
    private RaftRole role = RaftRole.FOLLOWER;
    private int currentTerm = 0;
    private String votedFor = null;
    private List<LogEntry> log = new ArrayList<>();

    private String leaderId = null;
    private final String ip = "http://127.0.0.1:";
    private int port = -1;

    // Scheduler para heartbeats
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
    // Executor separado para replicación
    private final ExecutorService replicationExecutor = Executors.newFixedThreadPool(3);

    private long lastHeartbeat = System.currentTimeMillis();

    public RaftNode(int port, String[] peers) {
        this.peers = peers;
        this.port = port;
        startFollowerTimeout();
        System.out.println("RAFT: Nodo creado con "+peers.length+" peers ("+String.join(", ",peers)+")");
    }

    private void startFollowerTimeout() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            System.out.println("RAFT: Role="+role+", LastHeartbeat="+(now-lastHeartbeat)+" Term="+currentTerm);
            if (role == RaftRole.FOLLOWER && (now - lastHeartbeat > 3000)) {
                startElection();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private void startElection() {
        System.out.println("RAFT: Iniciando elección");
        role = RaftRole.CANDIDATE;
        currentTerm++;
        votedFor = "self";
        int votesGranted = 1;
        int majority = (peers.length / 2) + 1;

        boolean anyPeerAvailable = false;
        for (String peer : peers) {
            if (askForVote(peer)) {
                votesGranted++;
                anyPeerAvailable = true;
            }
        }
        if(!anyPeerAvailable) {
            System.out.println("RAFT: No hay otros nodos disponibles, convirtiéndonos en lider");
            becomeLeader();
        }
        else if (votesGranted >= majority) {
            System.out.println("RAFT: Elección ganada, votes="+votesGranted);
            becomeLeader();
        } else {
            role = RaftRole.FOLLOWER;
            System.out.println("RAFT: Elección perdida");
        }
    }

    private boolean askForVote(String peer) {
        try {
            System.out.println("RAFT: Pidiendo voto a "+peer);
            URL url = new URL(peer+"/requestVote");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type","application/json");

            String body = "{\"term\":"+currentTerm+", \"candidateId\":\"self\"}";
            try(OutputStream os = con.getOutputStream()){
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = con.getResponseCode();
            if (code == 200) {
                try (InputStream in = con.getInputStream()) {
                    String resp = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    return resp.contains("\"voteGranted\":true");
                }
            }
        } catch (Exception e) {
            // peer no disponible
        }
        return false;
    }

    private void becomeLeader() {
        role = RaftRole.LEADER;
        leaderId = this.ip + this.port;
        startHeartbeats();
    }

    private void startHeartbeats() {
        // El envío de heartbeats permanece en un hilo separado e independiente de la replicación
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (role == RaftRole.LEADER) {
                sendHeartbeats();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeats() {
        for (String peer : peers) {
            sendAppendEntries(peer);
        }
    }

    private void sendAppendEntries(String peer) {
        try {
            URL url = new URL(peer + "/appendEntries");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            String body = "{\"term\":" + currentTerm + ", \"leaderId\":\"" + leaderId + "\", \"entries\":[]}";
            try (OutputStream os = con.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            con.getResponseCode();
        } catch (Exception e) {
            // Peer no disponible
        }
    }

    public synchronized String getStatus() {
        return "Role: "+role+", Term: "+currentTerm+", Leader: "+leaderId+", LogSize: "+log.size();
    }

    public synchronized void handleAppendEntriesRequest(int term, String leaderId) {
        if (term >= currentTerm) {
            currentTerm = term;
            this.leaderId = leaderId;
            role = RaftRole.FOLLOWER;
            lastHeartbeat = System.currentTimeMillis();
        }
    }

    public synchronized boolean handleRequestVote(int term, String candidateId) {
        if (term > currentTerm) {
            currentTerm = term;
            votedFor = null;
            role = RaftRole.FOLLOWER;
        }

        if (votedFor == null || votedFor.equals(candidateId)) {
            votedFor = candidateId;
            return true;
        }
        return false;
    }

    public synchronized int appendLogEntry(String command) {
        if (command == null) {
            System.err.println("appendLogEntry: 'command' es null, no se puede agregar al log");
            return -1;
        }
        LogEntry logEntry = new LogEntry(currentTerm, command);
        System.out.println("RAFT: Agregando entrada al log: " + logEntry);
        log.add(logEntry);
        int index = log.size() - 1;
        return index;
    }

    public void replicatedLogEntry(int index) {
        // Llamamos en un hilo separado para no bloquear el hilo de heartbeats
        replicationExecutor.submit(() -> {
            synchronized (this) {
                System.out.println("RAFT: Replicando entrada "+index);
                if (role != RaftRole.LEADER) return;
                if (index < 0 || index >= log.size()) return;
                String command = log.get(index).getCommand();

                for (String peer : peers) {
                    System.out.println("RAFT: Replicando entrada "+index+" a "+peer);
                    System.out.println("command: "+command);
                    replicateEntry(peer, index, command);
                }
            }
        });
    }

    private void replicateEntry(String peer, int index, String command) {
        try {
            URL url = new URL(peer+"/replicateEntry");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type","application/json");
            String body = "{\"term\":" + currentTerm + ", \"leaderId\":\"" + leaderId + "\", \"index\":" + index + ", \"entry\":\"" + command + "\"}";
            try(OutputStream os = con.getOutputStream()){
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            con.getResponseCode();
        } catch (Exception e) {
            // Peer no disponible
        }
    }

    public int redirectToLeader(String command) {
    System.out.println("RAFT: Redirigiendo petición al líder, que es " + leaderId);
    if (role == RaftRole.LEADER) return -1;
    try {
        URL url = new URL(leaderId + "/appendLogEntry");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");
        String body = "{\"entry\":\"" + command + "\"}";
        try (OutputStream os = con.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = con.getResponseCode();
        if (code == 200) {
            try (InputStream in = con.getInputStream()) {
                String resp = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("RAFT: Respuesta del líder: " + resp);
                return Integer.parseInt(resp);
            }
        } else {
            System.err.println("RAFT: Error redirigiendo al líder, código de respuesta: " + code);
        }
    } catch (Exception e) {
        System.err.println("RAFT: Excepción redirigiendo al líder: " + e.getMessage());
        e.printStackTrace();
    }
    return -1;
}


    public boolean isLeader() {
        return role == RaftRole.LEADER;
    }

    public String getLeader() {
        return leaderId;
    }
}
