package raft;

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

            // entry viene como "NAME_PROD=EstoEsNuevo&DETAIL=OtroLaptop&UNIT=200&AMOUNT=100&COST=0.5"
            // Parsear a Map
            Map<String,String> params = parseParams(entry);

            // Construir el comando con el formato que espera applyCommand
            String command = "create "
                    + (params.get("NAME_PROD") == null ? "" : params.get("NAME_PROD")) + " "
                    + (params.get("DETAIL") == null ? "" : params.get("DETAIL")) + " "
                    + (params.get("UNIT") == null ? "" : params.get("UNIT")) + " "
                    + (params.get("AMOUNT") == null ? "" : params.get("AMOUNT")) + " "
                    + (params.get("COST") == null ? "" : params.get("COST"));

            int index = raftNode.appendLogEntry(command);
            raftNode.replicatedLogEntry(index);

            // **Aplicar el comando en el líder**
            service.applyCommand(command);

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
