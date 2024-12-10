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

public class DeleteDetalleHandler implements HttpHandler {
    private VentasService service;
    private RaftNode raft;

    public DeleteDetalleHandler(VentasService service, RaftNode raft) {
        this.service = service;
        this.raft = raft;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405,-1);
            exchange.close();
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
        Map<String,String> params = parseParams(body);
        String idSales = params.get("ID_SALES");
        String idProd = params.get("ID_PROD");
        if(idSales==null||idProd==null||idSales.isEmpty()||idProd.isEmpty()){
            String response = "{\"error\":\"ID_SALES o ID_PROD no proporcionado\"}";
            exchange.sendResponseHeaders(400,response.length());
            try(OutputStream os=exchange.getResponseBody()){
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            exchange.close();
            return;
        }

        if(!raft.isLeader()) {
            String command = "deleteDetalle "+idSales+" "+idProd;
            int code = raft.redirectToLeader(command);
            if(code==-1) {
                String response = "{\"error\":\"Error redirigiendo al líder\"}";
                exchange.sendResponseHeaders(500,response.length());
                try(OutputStream os=exchange.getResponseBody()){
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                String response = "{\"status\":\"Redirigido al líder\"}";
                exchange.sendResponseHeaders(200,response.length());
                try(OutputStream os=exchange.getResponseBody()){
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
            exchange.close();
            return;
        }

        boolean success = service.deleteDetalleVenta(idSales, idProd);
        if(!success) {
            String response = "{\"error\":\"Detalle no encontrado\"}";
            exchange.sendResponseHeaders(404,response.length());
            try(OutputStream os=exchange.getResponseBody()){
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            exchange.close();
            return;
        }

        String command = "deleteDetalle "+idSales+" "+idProd;
        int index = raft.appendLogEntry(command);
        raft.replicatedLogEntry(index);

        String response = "{\"status\":\"accepted\"}";
        exchange.sendResponseHeaders(202,response.length());
        try(OutputStream os=exchange.getResponseBody()){
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
        exchange.close();
    }

    private Map<String,String> parseParams(String body){
        Map<String,String> map = new HashMap<>();
        if(body.isEmpty())return map;
        String[] pairs=body.split("&");
        for(String pair : pairs){
            String[] kv = pair.split("=");
            if(kv.length==2){
                String key= URLDecoder.decode(kv[0],StandardCharsets.UTF_8);
                String val= URLDecoder.decode(kv[1],StandardCharsets.UTF_8);
                map.put(key,val);
            }
        }
        return map;
    }
}
