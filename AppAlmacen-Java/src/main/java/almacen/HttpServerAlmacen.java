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
        server.createContext("/create", new CreateHandler(service, raftNode)); // Para crear un nuevo registro
        server.createContext("/read", new ReadHandler(service)); // Para leer un registro
        server.createContext("/update", new UpdateHandler(service, raftNode)); // Para actualizar un registro
        server.createContext("/delete", new DeleteHandler(service, raftNode)); // Para eliminar un registro

        // RAFT Handlers
        server.createContext("/appendEntries", new AppendEntriesHandler(raftNode)); // Para replicar logs
        server.createContext("/requestVote", new RequestVoteHandler(raftNode)); // Para elecciones de líder

        // Estado del nodo
        server.createContext("/status", new StatusHandler(raftNode)); // Para obtener el estado del nodo
    }

    public void start() {
        server.start();
    }
}
