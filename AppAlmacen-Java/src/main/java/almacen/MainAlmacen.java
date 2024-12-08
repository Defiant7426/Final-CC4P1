package almacen;


public class MainAlmacen {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: java -jar ServicioAlmacen.jar <puerto> <peers>");
            System.out.println("Ejemplo: java -jar ServicioAlmacen.jar 8080 http://ip_nodo2:8080,http://ip_nodo3:8080");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String peersArg = args[1];
        String[] peers = peersArg.split(",");

        AlmacenDB db = new AlmacenDB("src/main/resources/BD_ALMACEN.txt"); // Creamos la base de datos
        db.load(); // Cargamos la base de datos desde el archivo
        RaftNode raftNode = new RaftNode(peers);
        AlmacenService service = new AlmacenService(db, raftNode); // Inyectamos la base de datos y el nodo Raft

        HttpServerAlmacen server = new HttpServerAlmacen(port, service, raftNode); // Inyectamos el servicio y el nodo Raft
        server.start(); // Inicia el servidor HTTP

        System.out.println("Servicio Almacén corriendo en puerto " + port);
    }
}