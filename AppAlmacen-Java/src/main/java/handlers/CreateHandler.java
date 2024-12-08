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

        System.out.println("HTTP Lider: Recibiendo peticion de creación de registro");

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String,String> params = parseParams(body);

        String id = service.createRecord(params);
        System.out.println("HTTP Lider: Registro creado con id: "+id);

        // Construir el comando completo
        String command = "create " +
                (params.get("NAME_PROD") == null ? "" : params.get("NAME_PROD")) + " " +
                (params.get("DETAIL") == null ? "" : params.get("DETAIL")) + " " +
                (params.get("UNIT") == null ? "" : params.get("UNIT")) + " " +
                (params.get("AMOUNT") == null ? "" : params.get("AMOUNT")) + " " +
                (params.get("COST") == null ? "" : params.get("COST"));

        int index = raft.appendLogEntry(command); // Agregar la operación al log
        raft.replicatedLogEntry(index); // Replicar la operación a otros nodos

        String response = "{\"status\":\"accepted\"}";
        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(202, respBytes.length);
        exchange.getResponseBody().write(respBytes);
        exchange.close();
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
}
