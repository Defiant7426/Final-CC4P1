package handlers;

import almacen.AlmacenService;
import almacen.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class DeleteHandler implements HttpHandler {
    private AlmacenService service;
    private RaftNode raft;

    public DeleteHandler(AlmacenService service, RaftNode raft) {
        this.service = service;
        this.raft = raft;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
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

        boolean ok = service.deleteRecord(id);
        String response = ok ? "{\"status\":\"ok\"}" : "{\"status\":\"not_found\"}";
        byte[] respBytes = response.getBytes();
        exchange.sendResponseHeaders(200, respBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(respBytes);
        os.close();
    }
}
