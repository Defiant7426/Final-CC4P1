package almacen;

import java.io.*;
import java.util.*;

/**
 * Clase que implementa un almacén de datos simple usando un archivo CSV.
 * Cada registro es un mapa con los campos: ID_PROD| NAME_PROD| DETAIL| UNIT| AMOUNT| COST.
 */

public class AlmacenDB {
    private File file;
    // Usamos un Map simple: key=ID_PROD, value=map con campos
    private Map<String, Map<String,String>> records = new HashMap<>();

    public AlmacenDB(String filename) {
        System.out.println("BD: Creando base de datos en "+filename);
        this.file = new File(filename);
    }

    public synchronized void load() { // Carga la base de datos desde el archivo
        if (!file.exists()) return;
        System.out.println("BD: Cargando base de datos desde "+file);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Formato CSV simple con encabezado: ID_PROD,NAME_PROD,DETAIL,UNIT,AMOUNT,COST
            String header = br.readLine();
            if (header == null) return;
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
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
            System.out.println("BD: Base de datos cargada con "+records.size()+" registros");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void save() { // Guarda la base de datos en el archivo
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("ID_PROD|NAME_PROD|DETAIL|UNIT|AMOUNT|COST");
            for (Map<String,String> rec : records.values()) {
                pw.println(rec.get("ID_PROD")+"|"+rec.get("NAME_PROD")+"|"+rec.get("DETAIL")+"|"+
                        rec.get("UNIT")+"|"+rec.get("AMOUNT")+"|"+rec.get("COST"));
            }
            System.out.println("BD: Base de datos guardada en "+file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void create(Map<String,String> data) { // Crea un nuevo registro
        // data debe contener: ID_PROD|NAME_PROD|DETAIL|UNIT|AMOUNT|COST
        records.put(data.get("ID_PROD"), data);
        System.out.println("BD: Registro creado: "+data);
        save();
    }

    public synchronized Map<String,String> read(String id) { // Lee un registro
        return records.get(id);
    }

    public synchronized boolean update(String id, Map<String,String> data) { // Actualiza un registro
        if (!records.containsKey(id)) return false;
        Map<String,String> existing = records.get(id);
        existing.putAll(data);
        System.out.println("BD: Registro actualizado: "+existing);
        save();
        return true;
    }

    public synchronized boolean delete(String id) { // Borra un registro
        if (records.remove(id) != null) {
            save();
            System.out.println("BD: Registro borrado: "+id);
            return true;
        }
        System.out.println("BD: Registro no encontrado: "+id);
        return false;
    }
}
