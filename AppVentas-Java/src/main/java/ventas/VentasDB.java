package ventas;

import java.io.*;
import java.util.*;

/**
 * Clase que implementa la base de datos de Ventas usando archivos CSV.
 * Tendremos dos tablas:
 * - Ventas: Campos: ID_SALES|RUC|NAME|COST_TOTAL
 * - DetalleVentas: Campos: ID_SALES|ID_PROD|NAME_PROD|UNIT|AMOUNT|COST|TOTAL
 */

public class VentasDB {
    private File ventasFile;
    private File detalleFile;

    // Mapas en memoria
    // Para Ventas: key = ID_SALES, value = map con campos de la venta
    private Map<String, Map<String,String>> ventasRecords = new HashMap<>();
    // Para DetalleVentas: key = un identificador único (ej. ID_SALES+":"+ID_PROD),
    // value = map con campos del detalle.
    private Map<String, Map<String,String>> detalleRecords = new HashMap<>();

    public VentasDB(String ventasFilename, String detalleFilename) {
        System.out.println("BD Ventas: Creando base de datos en " + ventasFilename + " y " + detalleFilename);
        this.ventasFile = new File(ventasFilename);
        this.detalleFile = new File(detalleFilename);
    }

    public synchronized void load() {
        loadVentas();
        loadDetalleVentas();
    }

    private void loadVentas() {
        if (!ventasFile.exists()) return;
        System.out.println("BD Ventas: Cargando ventas desde " + ventasFile);
        try (BufferedReader br = new BufferedReader(new FileReader(ventasFile))) {
            String header = br.readLine(); // encabezado
            if (header == null) return;
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 4) continue;
                Map<String,String> record = new HashMap<>();
                record.put("ID_SALES", parts[0]);
                record.put("RUC", parts[1]);
                record.put("NAME", parts[2]);
                record.put("COST_TOTAL", parts[3]);
                ventasRecords.put(parts[0], record);
            }
            System.out.println("BD Ventas: Ventas cargadas: " + ventasRecords.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDetalleVentas() {
        if (!detalleFile.exists()) return;
        System.out.println("BD Ventas: Cargando detalle ventas desde " + detalleFile);
        try (BufferedReader br = new BufferedReader(new FileReader(detalleFile))) {
            String header = br.readLine(); // encabezado
            if (header == null) return;
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 7) continue;
                Map<String,String> record = new HashMap<>();
                record.put("ID_SALES", parts[0]);
                record.put("ID_PROD", parts[1]);
                record.put("NAME_PROD", parts[2]);
                record.put("UNIT", parts[3]);
                record.put("AMOUNT", parts[4]);
                record.put("COST", parts[5]);
                record.put("TOTAL", parts[6]);
                String key = parts[0] + ":" + parts[1];
                detalleRecords.put(key, record);
            }
            System.out.println("BD Ventas: DetalleVentas cargadas: " + detalleRecords.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveVentas() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ventasFile))) {
            pw.println("ID_SALES|RUC|NAME|COST_TOTAL");
            for (Map<String,String> rec : ventasRecords.values()) {
                pw.println(rec.get("ID_SALES")+"|"+rec.get("RUC")+"|"+rec.get("NAME")+"|"+rec.get("COST_TOTAL"));
            }
            System.out.println("BD Ventas: Ventas guardadas en "+ventasFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveDetalleVentas() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(detalleFile))) {
            pw.println("ID_SALES|ID_PROD|NAME_PROD|UNIT|AMOUNT|COST|TOTAL");
            for (Map<String,String> rec : detalleRecords.values()) {
                pw.println(rec.get("ID_SALES")+"|"+rec.get("ID_PROD")+"|"+rec.get("NAME_PROD")+"|"+
                           rec.get("UNIT")+"|"+rec.get("AMOUNT")+"|"+rec.get("COST")+"|"+rec.get("TOTAL"));
            }
            System.out.println("BD Ventas: DetalleVentas guardadas en "+detalleFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void createVenta(Map<String,String> data) {
        // data: ID_SALES, RUC, NAME, COST_TOTAL
        ventasRecords.put(data.get("ID_SALES"), data);
        saveVentas();
    }

    public synchronized Map<String,String> readVenta(String id) {
        return ventasRecords.get(id);
    }

    public synchronized boolean updateVenta(String id, Map<String,String> data) {
        if(!ventasRecords.containsKey(id)) return false;
        Map<String,String> existing = ventasRecords.get(id);
        existing.putAll(data);
        saveVentas();
        return true;
    }

    public synchronized boolean deleteVenta(String id) {
        if(ventasRecords.remove(id) != null) {
            // Borrar también detalles asociados a esa venta
            detalleRecords.keySet().removeIf(key -> key.startsWith(id+":"));
            saveVentas();
            saveDetalleVentas();
            return true;
        }
        return false;
    }

    public synchronized void createDetalleVenta(Map<String,String> data) {
        // data: ID_SALES, ID_PROD, NAME_PROD, UNIT, AMOUNT, COST, TOTAL
        String key = data.get("ID_SALES")+":"+data.get("ID_PROD");
        detalleRecords.put(key, data);
        saveDetalleVentas();
    }

    public synchronized Map<String,String> readDetalleVenta(String idSales, String idProd) {
        String key = idSales + ":" + idProd;
        return detalleRecords.get(key);
    }

    public synchronized boolean updateDetalleVenta(String idSales, String idProd, Map<String,String> data) {
        String key = idSales + ":" + idProd;
        if(!detalleRecords.containsKey(key)) return false;
        Map<String,String> existing = detalleRecords.get(key);
        existing.putAll(data);
        saveDetalleVentas();
        return true;
    }

    public synchronized boolean deleteDetalleVenta(String idSales, String idProd) {
        String key = idSales + ":" + idProd;
        if(detalleRecords.remove(key) != null) {
            saveDetalleVentas();
            return true;
        }
        return false;
    }

    public int getMaxVentaId() {
        return ventasRecords.keySet().stream().mapToInt(Integer::parseInt).max().orElse(0);
    }

    public int getMaxDetalleProdId(String idSales) {
        // Buscamos el max ID_PROD para la venta dada
        // Filtramos keys que empiecen con "idSales:"
        int max = 0;
        for (String key: detalleRecords.keySet()) {
            if(key.startsWith(idSales+":")) {
                String[] parts = key.split(":");
                int prodId = Integer.parseInt(parts[1]);
                if(prodId > max) max = prodId;
            }
        }
        return max;
    }
}
