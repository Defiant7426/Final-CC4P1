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
        String id = "P" + System.currentTimeMillis();
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
}
