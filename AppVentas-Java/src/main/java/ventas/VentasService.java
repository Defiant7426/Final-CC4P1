package ventas;

import java.util.HashMap;
import java.util.Map;

public class VentasService {
    private VentasDB db;
    private RaftNode raft;

    public VentasService(VentasDB db, RaftNode raftNode) {
        this.db = db;
        this.raft = raftNode;
    }

    public synchronized String createVenta(Map<String,String> data) {
        int maxId = db.getMaxVentaId();
        String id = String.valueOf(maxId+1);
        data.put("ID_SALES", id);
        db.createVenta(data);
        return id;
    }

    public synchronized Map<String,String> readVenta(String id) {
        return db.readVenta(id);
    }

    public synchronized boolean updateVenta(Map<String,String> data) {
        String id = data.get("ID_SALES");
        if (id == null) return false;
        return db.updateVenta(id, data);
    }

    public synchronized boolean deleteVenta(String id) {
        return db.deleteVenta(id);
    }

    public synchronized String createDetalleVenta(String idSales, Map<String,String> data) {
        int maxIdProd = db.getMaxDetalleProdId(idSales);
        String idProd = String.valueOf(maxIdProd+1);
        data.put("ID_SALES", idSales);
        data.put("ID_PROD", idProd);
        db.createDetalleVenta(data);
        return idProd;
    }

    public synchronized Map<String,String> readDetalleVenta(String idSales, String idProd) {
        return db.readDetalleVenta(idSales, idProd);
    }

    public synchronized boolean updateDetalleVenta(String idSales, String idProd, Map<String,String> data) {
        return db.updateDetalleVenta(idSales, idProd, data);
    }

    public synchronized boolean deleteDetalleVenta(String idSales, String idProd) {
        return db.deleteDetalleVenta(idSales, idProd);
    }

    public void applyCommand(String command) {
        System.out.println("Aplicando comando Ventas: " + command);
        String[] parts = command.split(" ");
        String action = parts[0];

        if(action.equals("createVenta")) {
            if(parts.length < 4) {
                System.err.println("Comando createVenta inválido");
                return;
            }
            // createVenta RUC NAME COST_TOTAL
            String ruc = parts[1];
            String name = parts[2];
            String costTotal = parts[3];
            Map<String,String> data = new HashMap<>();
            data.put("RUC", ruc);
            data.put("NAME", name);
            data.put("COST_TOTAL", costTotal);
            String id = createVenta(data);
            System.out.println("Venta creada con ID: " + id);

        } else if(action.equals("updateVenta")) {
            // updateVenta ID_SALES campo=valor campo=valor ...
            if(parts.length < 2) {
                System.err.println("Comando updateVenta inválido");
                return;
            }
            String id = parts[1];
            Map<String,String> data = new HashMap<>();
            data.put("ID_SALES", id);

            for(int i=2; i<parts.length; i++) {
                String[] kv = parts[i].split("=");
                if(kv.length==2) data.put(kv[0], kv[1]);
            }
            boolean success = updateVenta(data);
            if(success) System.out.println("Venta actualizada ID: " + id);
            else System.err.println("Fallo actualizando venta ID: "+id);

        } else if(action.equals("deleteVenta")) {
            if(parts.length<2) {
                System.err.println("Comando deleteVenta inválido");
                return;
            }
            String id = parts[1];
            boolean success = deleteVenta(id);
            if(success) System.out.println("Venta eliminada ID: " + id);
            else System.err.println("Fallo eliminando venta ID:" + id);

        } else if(action.equals("createDetalle")) {
            // createDetalle ID_SALES campo=valor ...
            if(parts.length<2) {
                System.err.println("Comando createDetalle inválido");
                return;
            }
            String idSales = parts[1];
            Map<String,String> data = new HashMap<>();
            for(int i=2; i<parts.length; i++) {
                String[] kv = parts[i].split("=");
                if(kv.length==2) data.put(kv[0], kv[1]);
            }
            String idProd = createDetalleVenta(idSales, data);
            System.out.println("Detalle creado ID_SALES: " + idSales + " ID_PROD: " + idProd);

        } else if(action.equals("updateDetalle")) {
            // updateDetalle ID_SALES ID_PROD campo=valor ...
            if(parts.length<3) {
                System.err.println("Comando updateDetalle inválido");
                return;
            }
            String idSales = parts[1];
            String idProd = parts[2];
            Map<String,String> data = new HashMap<>();
            for(int i=3; i<parts.length; i++) {
                String[] kv = parts[i].split("=");
                if(kv.length==2) data.put(kv[0], kv[1]);
            }
            boolean success = updateDetalleVenta(idSales, idProd, data);
            if(success) System.out.println("Detalle actualizado ID_SALES: " + idSales + " ID_PROD: " + idProd);
            else System.err.println("Fallo actualizando detalle");

        } else if(action.equals("deleteDetalle")) {
            // deleteDetalle ID_SALES ID_PROD
            if(parts.length<3) {
                System.err.println("Comando deleteDetalle inválido");
                return;
            }
            String idSales = parts[1];
            String idProd = parts[2];
            boolean success = deleteDetalleVenta(idSales, idProd);
            if(success) System.out.println("Detalle eliminado ID_SALES: " + idSales + " ID_PROD: " + idProd);
            else System.err.println("Fallo eliminando detalle");

        } else {
            System.err.println("Acción desconocida: " + action);
        }
    }
}
