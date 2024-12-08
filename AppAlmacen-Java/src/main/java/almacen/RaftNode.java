package almacen;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementación simplificada de RAFT
 */
public class RaftNode {
    private String[] peers;
    private RaftRole role = RaftRole.FOLLOWER;
    private int currentTerm = 0;
    private String votedFor = null;
    private List<LogEntry> log = new ArrayList<>();
    private int commitIndex = -1;
    private int lastApplied = -1;

    // Líder actual conocido
    private String leaderId = null; // No implementado a fondo, aquí podrías guardar una identificación del líder

    private final Random random = new Random();

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private long lastHeartbeat = System.currentTimeMillis();

    public RaftNode(String[] peers) {
        this.peers = peers;
        // Start follower timeout
        startFollowerTimeout();
    }

    private void startFollowerTimeout() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            // Si no recibimos heartbeat en, digamos, 3000ms, iniciamos elección
            if (role == RaftRole.FOLLOWER && (now - lastHeartbeat > 3000)) {
                startElection();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private void startElection() {
        role = RaftRole.CANDIDATE;
        currentTerm++;
        votedFor = "self"; // nos votamos a nosotros mismos
        int votesGranted = 1;
        int majority = (peers.length / 2) + 1;

        // Petición de voto a los demás nodos
        for (String peer : peers) {
            if (askForVote(peer)) {
                votesGranted++;
            }
        }

        if (votesGranted >= majority) {
            becomeLeader();
        } else {
            // Si no ganamos, volveremos a ser follower o reintentar
            role = RaftRole.FOLLOWER;
        }
    }

    private boolean askForVote(String peer) {
        try {
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
                // Leemos la respuesta
                try (InputStream in = con.getInputStream()) {
                    String resp = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    // Esperamos algo tipo {"voteGranted":true}
                    return resp.contains("\"voteGranted\":true");
                }
            }
        } catch (Exception e) {
            // Si falla el peer, no contamos el voto
        }
        return false;
    }

    private void becomeLeader() {
        role = RaftRole.LEADER;
        leaderId = "self";
        startHeartbeats();
    }

    private void startHeartbeats() {
        scheduler.scheduleAtFixedRate(() -> {
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
            URL url = new URL(peer+"/appendEntries");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type","application/json");
            String body = "{\"term\":"+currentTerm+", \"leaderId\":\"self\", \"entries\":[]}";
            try(OutputStream os = con.getOutputStream()){
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            con.getResponseCode(); // ignoramos respuesta
        } catch (Exception e) {
            // Peer no disponible, ignoramos por ahora
        }
    }

    public synchronized String getStatus() {
        return "Role: "+role+", Term: "+currentTerm+", Leader: "+leaderId+", LogSize: "+log.size();
    }

    public synchronized void handleAppendEntriesRequest(int term, String leaderId) {
        // Heartbeat recibido, nos mantenemos follower
        if (term >= currentTerm) {
            currentTerm = term;
            this.leaderId = leaderId;
            role = RaftRole.FOLLOWER;
            lastHeartbeat = System.currentTimeMillis();
        }
        // Ignoramos entradas en esta versión simplificada
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

    // En un líder, al recibir una nueva operación (ej: create), la agregaríamos al log y luego replicaríamos.
    // Aquí omitimos esa lógica detallada.
}
