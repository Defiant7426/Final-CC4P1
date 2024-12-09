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

public class DeleteHandler implements HttpHandler {
    private AlmacenService service;
    private RaftNode raft;

    public DeleteHandler(AlmacenService service, RaftNode raft) {
        this.service = service;
        this.raft = raft;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Método no permitido
            exchange.close();
            return;
        }

        if (!raft.isLeader()) {
            // Leer el cuerpo de la solicitud
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseParams(body);

            String id = params.get("ID_PROD");
            if (id == null || id.isEmpty()) {
                String response = "{\"error\":\"ID_PROD no proporcionado\"}";
                exchange.sendResponseHeaders(400, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();
                return;
            }

            // Construir el comando de eliminación
            String command = "delete " + id;

            // Enviar el comando al líder
            int responseCode = raft.redirectToLeader(command);
            if (responseCode == -1) {
                String response = "{\"error\":\"Error redirigiendo al líder\"}";
                System.out.println(response);
                exchange.sendResponseHeaders(500, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();
            } else {
                String response = "{\"status\":\"Redirigido al líder\"}";
                System.out.println(response);
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();
            }
            return;
        }

        // Si es líder, procesar la solicitud de eliminación
        System.out.println("HTTP Líder: Recibiendo petición de eliminación de registro");

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseParams(body);

        String id = params.get("ID_PROD");
        if (id == null || id.isEmpty()) {
            String response = "{\"error\":\"ID_PROD no proporcionado\"}";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            exchange.close();
            return;
        }

        boolean deleteSuccess = service.deleteRecord(id);
        if (!deleteSuccess) {
            String response = "{\"error\":\"Registro no encontrado o fallo en la eliminación\"}";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            exchange.close();
            return;
        }

        // Construir el comando con el formato que espera applyCommand
        // Formato: "delete ID_PROD"
        String command = "delete " + id;

        int index = raft.appendLogEntry(command); // Agregar la operación al log
        raft.replicatedLogEntry(index); // Replicar la operación

        String response = "{\"status\":\"accepted\"}";
        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(202, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
        exchange.close();
    }

    /**
     * Parsear los parámetros URL-encoded del cuerpo de la solicitud a un mapa.
     */
    private Map<String, String> parseParams(String body) {
        Map<String, String> map = new HashMap<>();
        if (body.isEmpty()) return map;
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }
}
