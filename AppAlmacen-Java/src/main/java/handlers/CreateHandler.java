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
            return;
        }

        // Lectura de parámetros (x-www-form-urlencoded)
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String,String> params = parseParams(body);

        // Aquí en una implementación RAFT real, el líder replicaría esta operación.
        // Suponiendo que este nodo es el líder (no se verifica en este ejemplo),
        // podríamos simplemente aplicar la operación:
        String id = service.createRecord(params);

        String response = "{\"status\":\"ok\",\"id\":\""+id+"\"}";
        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, respBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(respBytes);
        os.close();
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
