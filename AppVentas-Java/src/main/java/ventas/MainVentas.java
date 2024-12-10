package ventas;

import ventas.RaftNode;

public class MainVentas {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: java -jar ServicioVentas.jar <puerto> <peers_coma_separados>");
            System.out.println("Ejemplo: java -jar ServicioVentas.jar 9090 http://127.0.0.1:9091,http://127.0.0.1:9092");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String peersArg = args[1];
        String[] peers = peersArg.split(",");

        VentasDB db = new VentasDB("src/main/resources/BD_VENTAS.txt","src/main/resources/BD_DETALLEVENTAS.txt");
        db.load();
        RaftNode raftNode = new RaftNode(port, peers);
        VentasService service = new VentasService(db, raftNode);

        HttpServerVentas server = new HttpServerVentas(port, service, raftNode);
        server.start();

        System.out.println("Servicio Ventas corriendo en puerto " + port);
    }
}
