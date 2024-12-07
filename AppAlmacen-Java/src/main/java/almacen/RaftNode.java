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
 * Implementación simplificada de RAFT.
 * Los mensajes con términos más altos son considerados más recientes y tienen mayor autoridad.
 * nodo incrementa su currentTerm cuando inicia una elección.
 */
public class RaftNode {
    private final String[] peers; // URLs de los otros nodos
    private RaftRole role = RaftRole.FOLLOWER; // Inicialmente somos seguidores
    private int currentTerm = 0; // Término actual (el nodo con mayor término es líder)
    private String votedFor = null; // Voto actual
    private List<LogEntry> log = new ArrayList<>();  // Log de operaciones
    //private final int commitIndex = -1; // Índice de log aplicado
    //private final int lastApplied = -1; // Último índice aplicado

    // Líder actual conocido
    private String leaderId = null;

    //private final Random random = new Random();

    // Scheduler para tareas periódicas
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private long lastHeartbeat = System.currentTimeMillis(); // Último heartbeat recibido

    public RaftNode(String[] peers) {
        this.peers = peers;
        // Iniciamos el timeout de seguidor
        startFollowerTimeout();
        System.out.println("RAFT: Nodo creado con "+peers.length+" peers ("+String.join(", ",peers)+")");
    }

    private void startFollowerTimeout() { // Inicia el timeout de seguidor
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            // Si no recibimos heartbeat en, 3000ms, iniciamos elección
            System.out.println("RAFT: Role="+role+", LastHeartbeat="+(now-lastHeartbeat) + " Term="+currentTerm);
            if (role == RaftRole.FOLLOWER && (now - lastHeartbeat > 3000)) {
                startElection();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS); // Cada segundo
    }

    private void startElection() {
        System.out.println("RAFT: Iniciando elección");
        role = RaftRole.CANDIDATE; // Cambiamos a candidato
        currentTerm++;
        votedFor = "self"; // nos votamos a nosotros mismos
        int votesGranted = 1;
        int majority = (peers.length / 2) + 1;

        // Petición de voto a los demás nodos
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

        else if (votesGranted >= majority) { // Ganamos la elección
            System.out.println("RAFT: Elección ganada, votes="+votesGranted);
            becomeLeader(); // Nos convertimos en líder
        } else {
            // Si no ganamos, volveremos a ser follower o reintentar
            role = RaftRole.FOLLOWER;
            System.out.println("RAFT: Elección perdida");
        }
    }

    private boolean askForVote(String peer) {
        try {
            System.out.println("RAFT: Pidiendo voto a "+peer);
            // Aqui se hace la petición HTTP al endpoint /requestVote para pedir el voto
            URL url = new URL(peer+"/requestVote"); // URL del endpoint
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type","application/json");

            String body = "{\"term\":"+currentTerm+", \"candidateId\":\"self\"}";
            try(OutputStream os = con.getOutputStream()){
                os.write(body.getBytes(StandardCharsets.UTF_8)); // Enviamos la petición
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

    private void becomeLeader() { // Nos convertimos en líder
        role = RaftRole.LEADER;
        leaderId = "self";
        startHeartbeats();
    }

    private void startHeartbeats() { // Inicia el envío de heartbeats
        scheduler.scheduleAtFixedRate(() -> {
            if (role == RaftRole.LEADER) {
                sendHeartbeats();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeats() { // Envía heartbeats a los seguidores
        for (String peer : peers) {
            sendAppendEntries(peer);
        }
    }

    private void sendAppendEntries(String peer) { // Envía un heartbeat a un seguidor
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

    public synchronized String getStatus() { // Devuelve el estado del nodo
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

    public synchronized boolean handleRequestVote(int term, String candidateId) { // Maneja petición de voto
        if (term > currentTerm) { // Si el término es mayor, nos convertimos en seguidores
            currentTerm = term;
            votedFor = null;
            role = RaftRole.FOLLOWER;
        }

        if (votedFor == null || votedFor.equals(candidateId)) { //  Votamos por el candidato
            votedFor = candidateId;
            return true;
        }
        return false;
    }

    // En un líder, al recibir una nueva operación (ej: create), la agregaríamos al log y luego replicaríamos.

    public synchronized int appendLogEntry(String entry) {
        if (role != RaftRole.LEADER) return -1; // Solo el líder puede agregar entradas
        LogEntry logEntry = new LogEntry(currentTerm, entry);
        log.add(logEntry);
        return log.size()-1; // Devolvemos el índice de la nueva entrada
    }

    public synchronized void replicatedLogEntry(int index) { // Marca una entrada como replicada
        if (role != RaftRole.LEADER) return; // Solo el líder puede replicar
        if (index < 0 || index >= log.size()) return; // Índice inválido
        String entry = log.get(index).entry;
        for (String peer : peers) {
            replicateEntry(peer, index, entry);
        }
    }

    private void replicateEntry(String peer, int index, String entry) {
        try {
            URL url = new URL(peer+"/replicateEntry");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type","application/json");
            String body = "{\"term\":"+currentTerm+", \"leaderId\":\"self\", \"index\":"+index+", \"entry\":\""+entry+"\"}";
            try(OutputStream os = con.getOutputStream()){
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            con.getResponseCode(); // ignoramos respuesta
        } catch (Exception e) {
            // Peer no disponible, ignoramos por ahora
        }
    }

    public boolean isLeader() {
        return role == RaftRole.LEADER;
    }

    public String getLeader() {
        return leaderId;
    }
}
