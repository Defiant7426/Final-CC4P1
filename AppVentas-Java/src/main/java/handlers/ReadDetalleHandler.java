package handlers;

import ventas.VentasService;
import ventas.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ReadDetalleHandler implements HttpHandler {
    private VentasService service;
    private RaftNode raft;

    public ReadDetalleHandler(VentasService service, RaftNode raft) {
        this.service = service;
        this.raft = raft;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405,-1);
            exchange.close();
            return;
        }

        Map<String,String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String idSales = params.get("id_sales");
        String idProd = params.get("id_prod");
        if(idSales==null||idProd==null||idSales.isEmpty()||idProd.isEmpty()) {
            String response = "{\"error\":\"id_sales o id_prod no proporcionado\"}";
            exchange.sendResponseHeaders(400,response.length());
            try(OutputStream os=exchange.getResponseBody()){
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            exchange.close();
            return;
        }

        if(!raft.isLeader()) {
            String leader = raft.getLeader();
            if(leader==null) {
                String response = "{\"error\":\"No hay líder actualmente\"}";
                exchange.sendResponseHeaders(503,response.length());
                try(OutputStream os=exchange.getResponseBody()){
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();
                return;
            }

            String redirectUrl = leader+"/readDetalle?id_sales="+URLEncoder.encode(idSales,StandardCharsets.UTF_8)+"&id_prod="+URLEncoder.encode(idProd,StandardCharsets.UTF_8);
            try{
                URL url = new URL(redirectUrl);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(2000);
                con.setReadTimeout(2000);

                int code = con.getResponseCode();
                if(code==200) {
                    String resp = new String(con.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200,resp.length());
                    try(OutputStream os=exchange.getResponseBody()){
                        os.write(resp.getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    String response = "{\"error\":\"Error al obtener respuesta del líder\"}";
                    exchange.sendResponseHeaders(502,response.length());
                    try(OutputStream os=exchange.getResponseBody()){
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }catch(Exception e) {
                String response = "{\"error\":\"No se pudo conectar con el líder\"}";
                exchange.sendResponseHeaders(502,response.length());
                try(OutputStream os=exchange.getResponseBody()){
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
            exchange.close();
            return;
        }

        // Es líder
        Map<String,String> detalle = service.readDetalleVenta(idSales, idProd);
        if(detalle==null) {
            String response = "{\"error\":\"Detalle no encontrado\"}";
            exchange.sendResponseHeaders(404,response.length());
            try(OutputStream os=exchange.getResponseBody()){
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            exchange.close();
            return;
        }

        String json = mapToJson(detalle);
        exchange.getResponseHeaders().add("Content-Type","application/json");
        exchange.sendResponseHeaders(200,json.getBytes(StandardCharsets.UTF_8).length);
        try(OutputStream os=exchange.getResponseBody()){
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        exchange.close();
    }

    private Map<String,String> parseQuery(String query) {
        Map<String,String> map = new HashMap<>();
        if(query==null||query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for(String pair : pairs){
            String[] kv = pair.split("=");
            if(kv.length==2) {
                String key=URLDecoder.decode(kv[0],StandardCharsets.UTF_8);
                String val=URLDecoder.decode(kv[1],StandardCharsets.UTF_8);
                map.put(key,val);
            }
        }
        return map;
    }

    private String mapToJson(Map<String,String> map){
        StringBuilder sb=new StringBuilder();
        sb.append("{");
        int count=0;
        for(Map.Entry<String,String> e: map.entrySet()){
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            count++;
            if(count<map.size()) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }
}
