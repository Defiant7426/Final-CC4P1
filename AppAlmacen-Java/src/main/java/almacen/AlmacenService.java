package almacen;

import java.util.HashMap;
import java.util.Map;

public class AlmacenService {
    private AlmacenDB db;
    private RaftNode raft;

    public AlmacenService(AlmacenDB db, RaftNode raftNode) {
        this.db = db;
        this.raft = raftNode;
    }

    public synchronized String createRecord(Map<String,String> data) {
        // data: NAME_PROD, DETAIL, UNIT, AMOUNT, COST
        // Generar un ID_PROD único
        int maxId = db.getMaxId();
        String id = String.valueOf(maxId + 1);
        data.put("ID_PROD", id);
        db.create(data);
        return id;
    }

    public synchronized Map<String,String> readRecord(String id) {
        return db.read(id);
    }

    public synchronized boolean updateRecord(Map<String,String> data) {
        // Debe incluir ID_PROD
        String id = data.get("ID_PROD");
        if (id == null) return false;
        return db.update(id, data);
    }

    public synchronized boolean deleteRecord(String id) {
        return db.delete(id);
    }

    public void applyCommand(String command) {
    System.out.println("Aplicando comando: " + command);
    String[] parts = command.split(" ");
    String action = parts[0];

    if (action.equals("create")) {
        if (parts.length < 6) {
            System.err.println("Comando create inválido, se necesitan 6 partes, comando: " + command);
            return;
        }
        Map<String,String> data = new HashMap<>();
        data.put("NAME_PROD", parts[1]);
        data.put("DETAIL", parts[2]);
        data.put("UNIT", parts[3]);
        data.put("AMOUNT", parts[4]);
        data.put("COST", parts[5]);

        String id = createRecord(data);
        System.out.println("Registro creado con ID: " + id);
    } else if (action.equals("update")) {
        if (parts.length < 2) {
            System.err.println("Comando update inválido, se necesita al menos ID_PROD, comando: " + command);
            return;
        }
        String id = parts[1];
        Map<String, String> data = new HashMap<>();
        for (int i = 2; i < parts.length; i++) {
            String[] kv = parts[i].split("=");
            if (kv.length == 2) {
                data.put(kv[0], kv[1]);
            }
        }
        boolean success = updateRecord(data);
        if (success) {
            System.out.println("Registro actualizado con ID: " + id);
        } else {
            System.err.println("Fallo al actualizar el registro con ID: " + id);
        }
    } else if (action.equals("delete")) {
        // ...
    } else {
        System.err.println("Acción desconocida: " + action);
    }
}

}
