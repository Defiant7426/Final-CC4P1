package almacen;


public class MainAlmacen {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Antes hacer: ./gradlew build");
            System.out.println("Luego usar: java -jar build/libs/AppAlmacen-Java-1.0-SNAPSHOT.jar 8080 http://127.0.0.1:8081,http://127.0.0.1:8082");
            return;
        }

        int port = Integer.parseInt(args[0]); // Puerto del servidor HTTP del nodo actual
        String peersArg = args[1];
        String[] peers = peersArg.split(",");

        AlmacenDB db = new AlmacenDB("src/main/resources/BD_ALMACEN.txt"); // Creamos la base de datos
        db.load(); // Cargamos la base de datos desde el archivo
        RaftNode raftNode = new RaftNode(port, peers);
        AlmacenService service = new AlmacenService(db, raftNode); // Inyectamos la base de datos y el nodo Raft

        HttpServerAlmacen server = new HttpServerAlmacen(port, service, raftNode); // Inyectamos el servicio y el nodo Raft
        server.start(); // Inicia el servidor HTTP

        System.out.println("Servicio Almacén corriendo en puerto " + port);
    }
}