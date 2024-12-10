package ventas;

import com.sun.net.httpserver.HttpServer;
import handlers.*;
import raft.*;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerVentas {
    private HttpServer server;

    public HttpServerVentas(int port, VentasService service, RaftNode raftNode) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Handlers CRUD Ventas
        server.createContext("/createVenta", new CreateVentaHandler(service, raftNode));
        server.createContext("/readVenta", new ReadVentaHandler(service, raftNode));
        server.createContext("/updateVenta", new UpdateVentaHandler(service, raftNode));
        server.createContext("/deleteVenta", new DeleteVentaHandler(service, raftNode));

        // Handlers CRUD DetalleVentas
        server.createContext("/createDetalle", new CreateDetalleHandler(service, raftNode));
        server.createContext("/readDetalle", new ReadDetalleHandler(service, raftNode));
        server.createContext("/updateDetalle", new UpdateDetalleHandler(service, raftNode));
        server.createContext("/deleteDetalle", new DeleteDetalleHandler(service, raftNode));

        // RAFT Handlers (reutilizados sin cambios en su lógica interna)
        server.createContext("/appendEntries", new AppendEntriesHandler(raftNode));
        server.createContext("/requestVote", new RequestVoteHandler(raftNode));
        server.createContext("/appendLogEntry", new AppendLogEntryHandler(raftNode, service));
        server.createContext("/replicateEntry", new ReplicateEntryHandler(raftNode, service));
        server.createContext("/status", new StatusHandler(raftNode));
    }

    public void start() {
        server.start();
    }
}
