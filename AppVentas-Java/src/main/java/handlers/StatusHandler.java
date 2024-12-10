package handlers;

import ventas.RaftNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class StatusHandler implements HttpHandler {
    private RaftNode raftNode;

    public StatusHandler(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String status = raftNode.getStatus();
        byte[] respBytes = ("{\"status\":\""+status+"\"}").getBytes();
        exchange.sendResponseHeaders(200, respBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(respBytes);
        os.close();
    }
}
