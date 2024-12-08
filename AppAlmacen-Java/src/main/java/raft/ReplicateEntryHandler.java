package raft;

import almacen.AlmacenService;
import almacen.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handler que recibe la replicación de una entrada del log desde el líder.
 * Aplica el comando en la base de datos local.
 */
public class ReplicateEntryHandler implements HttpHandler {
    private final RaftNode raftNode;
    private final AlmacenService service;

    public ReplicateEntryHandler(RaftNode raftNode, AlmacenService service) {
        this.raftNode = raftNode;
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            System.out.println("ReplicateEntryHandler: Request received");
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("ReplicateEntryHandler: Request body - " + requestBody);

            String command = parseCommandFromRequestBody(requestBody);
            int index = raftNode.appendLogEntry(command);

            // Aplicar el comando en el seguidor
            service.applyCommand(command);

            String response = String.valueOf(index);
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            System.out.println("ReplicateEntryHandler: Response sent");
            exchange.close();
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }
    }

    private String parseCommandFromRequestBody(String requestBody) {
        // Buscamos el campo "entry":"algo"
        String entryKey = "\"entry\":\"";
        int start = requestBody.indexOf(entryKey);
        if (start < 0) return "";
        start += entryKey.length();
        int end = requestBody.indexOf("\"", start);
        if (end < 0) return "";
        return requestBody.substring(start, end);
    }
}
