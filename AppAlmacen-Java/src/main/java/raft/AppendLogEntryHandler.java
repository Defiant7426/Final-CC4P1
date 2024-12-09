package raft;

import almacen.AlmacenService;
import almacen.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AppendLogEntryHandler implements HttpHandler {
    private final RaftNode raftNode;
    private final AlmacenService service; // Añadimos la referencia al servicio

    public AppendLogEntryHandler(RaftNode raftNode, AlmacenService service) {
        this.raftNode = raftNode;
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("AppendLogEntryHandler: Respuesta recibida");
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("AppendLogEntryHandler: Cuerpo - " + requestBody);

            // Parsear el campo "entry"
            String entry = parseEntryFromRequestBody(requestBody);

            // Crear una entrada de log
            int index = raftNode.appendLogEntry(entry);
            raftNode.replicatedLogEntry(index);

            // Aplicar el comando en el líder
            service.applyCommand(entry);

            String response = String.valueOf(index);
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            System.out.println("AppendLogEntryHandler: Respuesta enviada");
            exchange.close();
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }
    }

    private String parseEntryFromRequestBody(String requestBody) {
        String entryKey = "\"entry\":\"";
        int start = requestBody.indexOf(entryKey);
        if (start < 0) return "";
        start += entryKey.length();
        int end = requestBody.indexOf("\"", start);
        if (end < 0) return "";
        return requestBody.substring(start, end);
    }
}
