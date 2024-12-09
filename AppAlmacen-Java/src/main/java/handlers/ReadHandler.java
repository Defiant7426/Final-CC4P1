package handlers;

import almacen.AlmacenService;
import almacen.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ReadHandler implements HttpHandler {
    private AlmacenService service;
    private RaftNode raft;

    public ReadHandler(AlmacenService service, RaftNode raft) {
        this.service = service;
        this.raft = raft;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Método no permitido
            exchange.close();
            return;
        }

        // Parsear los parámetros de la consulta
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String id = params.get("id");

        if (id == null || id.isEmpty()) {
            String response = "{\"error\":\"ID_PROD no proporcionado\"}";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            exchange.close();
            return;
        }

        if (!raft.isLeader()) {
            String leader = raft.getLeader();
            if (leader == null) {
                String response = "{\"error\":\"No hay líder actualmente\"}";
                exchange.sendResponseHeaders(503, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();
                return;
            }

            // Construir la URL del líder para reenviar la solicitud
            String redirectUrl = leader + "/read?id=" + URLEncoder.encode(id, StandardCharsets.UTF_8);

            try {
                // Realizar una solicitud GET al líder
                URL url = new URL(redirectUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(2000);
                con.setReadTimeout(2000);

                int responseCode = con.getResponseCode();
                if (responseCode == 200) {
                    String leaderResponse = new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, leaderResponse.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(leaderResponse.getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    String response = "{\"error\":\"Error al obtener respuesta del líder\"}";
                    exchange.sendResponseHeaders(502, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception e) {
                String response = "{\"error\":\"No se pudo conectar con el líder\"}";
                exchange.sendResponseHeaders(502, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
            exchange.close();
            return;
        }

        // Si es líder, procesar la solicitud de lectura
        Map<String, String> record = service.readRecord(id);
        if (record == null) {
            String response = "{\"error\":\"Registro no encontrado\"}";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            // Convertir el registro a JSON
            String jsonResponse = mapToJson(record);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            }
        }
        exchange.close();
    }

    /**
     * Parsear la cadena de consulta de la URL a un mapa.
     */
    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
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

    /**
     * Convertir un mapa a una cadena JSON simple.
     * Nota: Para una implementación más robusta, considera usar una librería JSON como Gson o Jackson.
     */
    private String mapToJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int count = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append("\"").append(entry.getValue()).append("\"");
            count++;
            if (count < map.size()) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
