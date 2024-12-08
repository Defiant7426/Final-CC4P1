package handlers;

import almacen.AlmacenService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class ReadHandler implements HttpHandler {
    private AlmacenService service;

    public ReadHandler(AlmacenService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String id = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && kv[0].equals("id")) {
                    id = kv[1];
                    break;
                }
            }
        }

        if (id == null) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        Map<String,String> record = service.readRecord(id);
        String response;
        if (record == null) {
            response = "{\"status\":\"not_found\"}";
        } else {
            // Simplemente convertimos el map a JSON (básico)
            StringBuilder sb = new StringBuilder("{");
            boolean first=true;
            for (var e : record.entrySet()) {
                if(!first) sb.append(",");
                sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
                first=false;
            }
            sb.append("}");
            response = sb.toString();
        }

        byte[] respBytes = response.getBytes();
        exchange.sendResponseHeaders(200, respBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(respBytes);
        os.close();
    }
}