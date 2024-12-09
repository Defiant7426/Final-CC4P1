class AppVentas:
    def __init__(self, db, raft_node):
        self.db = db
        self.raft = raft_node

    def create_sale(self, data):
        # data: {"RUC":..., "NAME":..., "COST_TOTAL":...}
        sale_id = self.db.create_sale(data)
        return sale_id

    def read_sale(self, sale_id):
        return self.db.read_sale(sale_id)

    def update_sale(self, data):
        # data debe incluir ID_SALES
        sale_id = data.get("ID_SALES")
        if not sale_id:
            return False
        return self.db.update_sale(sale_id, data)

    def delete_sale(self, sale_id):
        return self.db.delete_sale(sale_id)

    def apply_command(self, command):
        # Ejemplo command: "create_sale RUC=123 NAME=Test COST_TOTAL=500"
        parts = command.strip().split()
        action = parts[0]

        if action == "create_sale":
            # parsear campos
            fields = {}
            for kv in parts[1:]:
                k,v = kv.split("=")
                fields[k] = v
            sid = self.create_sale(fields)
            print(f"Sale creada con ID: {sid}")

        elif action == "update_sale":
            # update_sale ID_SALES=XX campo=valor ...
            fields = {}
            for kv in parts[1:]:
                k,v = kv.split("=")
                fields[k] = v
            res = self.update_sale(fields)
            if res:
                print(f"Sale {fields.get('ID_SALES')} actualizada")
            else:
                print("Error al actualizar venta")

        elif action == "delete_sale":
            # delete_sale ID_SALES
            if len(parts) < 2:
                print("Comando delete_sale inválido")
                return
            sid = parts[1]
            res = self.delete_sale(sid)
            if res:
                print(f"Sale {sid} eliminada")
            else:
                print("Error al eliminar venta")

        else:
            print("Comando desconocido:", action)
