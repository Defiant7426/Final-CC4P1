package raft;

import almacen.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AppendLogEntryHandler implements HttpHandler {
    private final RaftNode raftNode;

    public AppendLogEntryHandler(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("AppendLogEntryHandler: Respuesta recibida");
        if ("POST".equals(exchange.getRequestMethod())) {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("AppendLogEntryHandler: Cuerpo - " + requestBody);
            // Parse the request body to extract the log entry
            String entry = parseEntryFromRequestBody(requestBody);
            int index = raftNode.appendLogEntry(entry);
            String response = String.valueOf(index);
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            System.out.println("AppendLogEntryHandler: Respuesta enviada");
            exchange.close();
        } else {
            exchange.sendResponseHeaders(405, -1); // Método no permitido
            exchange.close();
        }
    }

    private String parseEntryFromRequestBody(String requestBody) {
        // Implementa la lógica para parsear la entrada del cuerpo de la solicitud
        // Este es un ejemplo simplificado asumiendo que el cuerpo de la solicitud es un objeto JSON con un campo "entry"
        return requestBody.substring(requestBody.indexOf(":") + 2, requestBody.length() - 2);
    }
}