package raft;

import almacen.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
    * Clase que maneja las peticiones de AppendEntries.
 */

public class AppendEntriesHandler implements HttpHandler {
    private RaftNode raftNode;

    public AppendEntriesHandler(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        InputStream in = exchange.getRequestBody();
        String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        // body ejemplo: {"term":1,"leaderId":"self","entries":[]}
        // Parse sencillo (no robusto)
        int term = parseInt(body, "\"term\":", ",");
        String leaderId = parseString(body, "\"leaderId\":\"", "\"");
        raftNode.handleAppendEntriesRequest(term, leaderId);

        String response = "{\"success\":true}";
        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, respBytes.length);
        exchange.getResponseBody().write(respBytes);
        exchange.close();
    }

    private int parseInt(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start < 0) return 0;
        start += startMarker.length();
        int end = text.indexOf(endMarker, start);
        if (end < 0) end = text.length();
        String num = text.substring(start, end).trim();
        return Integer.parseInt(num);
    }

    private String parseString(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start < 0) return "";
        start += startMarker.length();
        int end = text.indexOf(endMarker, start);
        if (end < 0) return "";
        return text.substring(start, end);
    }
}