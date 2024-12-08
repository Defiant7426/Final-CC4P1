package almacen;

import java.io.*;
import java.util.*;

public class AlmacenDB {
    private File file;
    // Usamos un Map simple: key=ID_PROD, value=map con campos
    private Map<String, Map<String,String>> records = new HashMap<>();

    public AlmacenDB(String filename) {
        System.out.println("Creando base de datos en "+filename);
        this.file = new File(filename);
    }

    public synchronized void load() {
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Formato CSV simple con encabezado: ID_PROD,NAME_PROD,DETAIL,UNIT,AMOUNT,COST
            String header = br.readLine();
            if (header == null) return;
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 6) continue;
                Map<String,String> record = new HashMap<>();
                record.put("ID_PROD", parts[0]);
                record.put("NAME_PROD", parts[1]);
                record.put("DETAIL", parts[2]);
                record.put("UNIT", parts[3]);
                record.put("AMOUNT", parts[4]);
                record.put("COST", parts[5]);
                records.put(parts[0], record);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("ID_PROD,NAME_PROD,DETAIL,UNIT,AMOUNT,COST");
            for (Map<String,String> rec : records.values()) {
                pw.println(rec.get("ID_PROD")+","+rec.get("NAME_PROD")+","+rec.get("DETAIL")+","+
                        rec.get("UNIT")+","+rec.get("AMOUNT")+","+rec.get("COST"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void create(Map<String,String> data) {
        // data debe contener: ID_PROD, NAME_PROD, DETAIL, UNIT, AMOUNT, COST
        records.put(data.get("ID_PROD"), data);
        save();
    }

    public synchronized Map<String,String> read(String id) {
        return records.get(id);
    }

    public synchronized boolean update(String id, Map<String,String> data) {
        if (!records.containsKey(id)) return false;
        Map<String,String> existing = records.get(id);
        existing.putAll(data);
        save();
        return true;
    }

    public synchronized boolean delete(String id) {
        if (records.remove(id) != null) {
            save();
            return true;
        }
        return false;
    }
}
