package handlers;

import ventas.VentasService;
import ventas.RaftNode;
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

public class ReadVentaHandler implements HttpHandler {
    private VentasService service;
    private RaftNode raft;

    public ReadVentaHandler(VentasService service, RaftNode raft) {
        this.service = service;
        this.raft = raft;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String id = params.get("id");
        if (id == null || id.isEmpty()) {
            String response = "{\"error\":\"ID_SALES no proporcionado\"}";
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

            // Redirigir petición al líder
            String redirectUrl = leader + "/readVenta?id=" + URLEncoder.encode(id, StandardCharsets.UTF_8);
            try {
                URL url = new URL(redirectUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(2000);
                con.setReadTimeout(2000);

                int code = con.getResponseCode();
                if (code == 200) {
                    String resp = new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, resp.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp.getBytes(StandardCharsets.UTF_8));
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

        // Si es líder
        Map<String,String> venta = service.readVenta(id);
        if (venta == null) {
            String response = "{\"error\":\"Venta no encontrada\"}";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            String json = mapToJson(venta);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
        exchange.close();
    }

    private Map<String,String> parseQuery(String query) {
        Map<String,String> map = new HashMap<>();
        if(query==null||query.isEmpty())return map;
        String[] pairs = query.split("&");
        for(String pair : pairs) {
            String[] kv = pair.split("=");
            if(kv.length==2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                map.put(key,val);
            }
        }
        return map;
    }

    private String mapToJson(Map<String,String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int count=0;
        for (Map.Entry<String,String> e : map.entrySet()) {
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            count++;
            if(count<map.size()) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }
}
