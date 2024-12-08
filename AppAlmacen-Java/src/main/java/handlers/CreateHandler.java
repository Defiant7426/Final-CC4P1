package handlers;

import almacen.AlmacenService;
import almacen.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CreateHandler implements HttpHandler {
    private AlmacenService service;
    private RaftNode raft;

    public CreateHandler(AlmacenService service, RaftNode raft) {
        this.service = service;
        this.raft = raft;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        if(!raft.isLeader()) {
            // Si no somos líder, redirigimos la petición al líder
            // Redirigir al líder
            String entry = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int responseCode = raft.redirectToLeader(entry);
            if (responseCode == -1) {
                String response = "Error redirigiendo al líder";
                System.out.println(response);
                exchange.sendResponseHeaders(500, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();
            } else {
                String response = "Redirigido al líder";
                System.out.println(response);
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();
            }
            return;
        }

        // Estamos en el líder, procesamos la petición

        System.out.println("HTTP Lider: Recibiendo peticion de creación de registro");

        // Lectura de parámetros (x-www-form-urlencoded)
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String,String> params = parseParams(body);

        String id = service.createRecord(params);

        int index = raft.appendLogEntry(id); // Agregar la operación al log
        raft.replicatedLogEntry(index); // Replicar la operación
        String response = "{\"status\":\"accepted\"}"; // Respuesta aceptada
        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(202, respBytes.length);
        exchange.getResponseBody().write(respBytes);
        exchange.close();


//        String response = "{\"status\":\"ok\",\"id\":\""+id+"\"}";
//        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
//        exchange.sendResponseHeaders(200, respBytes.length);
//        OutputStream os = exchange.getResponseBody();
//        os.write(respBytes);
//        os.close();
    }

    private Map<String,String> parseParams(String body) {
        Map<String,String> map = new HashMap<>();
        if (body.isEmpty()) return map;
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length==2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                map.put(key,val);
            }
        }
        return map;
    }

    private String createCommandJson(String op, Map<String,String> data) {
        // Convertir data a JSON (simple)
        // Por ejemplo:
        StringBuilder sb = new StringBuilder();
        sb.append("{\"op\":\"").append(op).append("\", \"data\":{");
        boolean first = true;
        for (var e : data.entrySet()) {
            if(!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            first=false;
        }
        sb.append("}}");
        return sb.toString();
    }
}
