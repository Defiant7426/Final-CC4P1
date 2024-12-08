package almacen;

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
        String[] parts = command.split(" ");
        String action = parts[0];
        if (action.equals("create")) {
            Map<String,String> data = Map.of(
                "NAME_PROD", parts[1],
                "DETAIL", parts[2],
                "UNIT", parts[3],
                "AMOUNT", parts[4],
                "COST", parts[5]
            );
            createRecord(data);
        } else if (action.equals("update")) {
            Map<String,String> data = Map.of(
                "ID_PROD", parts[1],
                "NAME_PROD", parts[2],
                "DETAIL", parts[3],
                "UNIT", parts[4],
                "AMOUNT", parts[5],
                "COST", parts[6]
            );
            updateRecord(data);
        } else if (action.equals("delete")) {
            deleteRecord(parts[1]);
        }
    }
}
