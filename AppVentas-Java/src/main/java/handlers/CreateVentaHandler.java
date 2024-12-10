package handlers;

import ventas.VentasService;
import ventas.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CreateVentaHandler implements HttpHandler {
    private VentasService service;
    private RaftNode raft;

    public CreateVentaHandler(VentasService service, RaftNode raft) {
        this.service = service;
        this.raft = raft;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        if(!raft.isLeader()) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int responseCode = raft.redirectToLeader("createVenta "+bodyToCommand(body));
            if (responseCode == -1) {
                String response = "Error redirigiendo al líder";
                exchange.sendResponseHeaders(500, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();
            } else {
                String response = "Redirigido al líder";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();
            }
            return;
        }

        // Si es líder:
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String,String> params = parseParams(body);

        // Esperamos RUC, NAME, COST_TOTAL
        String ruc = params.get("RUC");
        String name = params.get("NAME");
        String costTotal = params.get("COST_TOTAL");

        if(ruc==null || name==null || costTotal==null) {
            String response = "{\"error\":\"Faltan campos (RUC, NAME, COST_TOTAL)\"}";
            exchange.sendResponseHeaders(400, response.length());
            try(OutputStream os = exchange.getResponseBody()){
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            exchange.close();
            return;
        }

        String id = service.createVenta(params);
        // Comando para agregar al log
        String command = "createVenta "+ruc+" "+name+" "+costTotal;
        int index = raft.appendLogEntry(command);
        raft.replicatedLogEntry(index);

        String response = "{\"status\":\"accepted\", \"ID_SALES\":\""+id+"\"}";
        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(202, respBytes.length);
        exchange.getResponseBody().write(respBytes);
        exchange.close();
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

    private String bodyToCommand(String body) {
        // body contendrá algo como: RUC=RUC123&NAME=EmpresaABC&COST_TOTAL=1000.0
        // Debemos convertir esto a: "RUC123 EmpresaABC 1000.0"
        Map<String,String> p = parseParams(body);
        return p.get("RUC")+" "+p.get("NAME")+" "+p.get("COST_TOTAL");
    }
}

