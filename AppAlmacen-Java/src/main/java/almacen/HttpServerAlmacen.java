package almacen;

import com.sun.net.httpserver.HttpServer;
import handlers.*;
import raft.AppendEntriesHandler;
import raft.RequestVoteHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerAlmacen {
    private HttpServer server;

    public HttpServerAlmacen(int port, AlmacenService service, RaftNode raftNode) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Handlers CRUD
        server.createContext("/create", new CreateHandler(service, raftNode));
        server.createContext("/read", new ReadHandler(service));
        server.createContext("/update", new UpdateHandler(service, raftNode));
        server.createContext("/delete", new DeleteHandler(service, raftNode));

        // RAFT Handlers
        server.createContext("/appendEntries", new AppendEntriesHandler(raftNode));
        server.createContext("/requestVote", new RequestVoteHandler(raftNode));

        // Estado del nodo
        server.createContext("/status", new StatusHandler(raftNode));
    }

    public void start() {
        server.start();
    }
}
