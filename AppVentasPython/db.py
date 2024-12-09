import os

class VentasDB:
    def __init__(self, sales_file, details_file):
        self.sales_file = sales_file
        self.details_file = details_file
        self.ventas = {}
        self.detalles = []

    def load(self):
        # Cargar ventas
        if os.path.exists(self.sales_file):
            with open(self.sales_file, "r", encoding='utf-8') as f:
                for line in f:
                    id_sales, ruc, name, cost_total = line.strip().split('|')
                    self.ventas[id_sales] = {"ID_SALES": id_sales, "RUC": ruc, "NAME": name, "COST_TOTAL": cost_total}

        # Cargar detalles
        if os.path.exists(self.details_file):
            with open(self.details_file, "r", encoding='utf-8') as f:
                for line in f:
                    id_sales, id_prod, name_prod, unit, amount, cost, total = line.strip().split('|')
                    self.detalles.append({"ID_SALES": id_sales, "ID_PROD": id_prod, "NAME_PROD": name_prod, "UNIT": unit, "AMOUNT": amount, "COST": cost, "TOTAL": total})

        print(f"DB: Cargadas {len(self.ventas)} ventas y {len(self.detalles)} detalles.")

    def save_ventas(self):
        with open(self.sales_file, "w", encoding='utf-8') as f:
            for v in self.ventas.values():
                f.write(f"{v['ID_SALES']}|{v['RUC']}|{v['NAME']}|{v['COST_TOTAL']}\n")
        print("DB: Ventas guardadas.")

    def save_detalles(self):
        with open(self.details_file, "w", encoding='utf-8') as f:
            for d in self.detalles:
                f.write(f"{d['ID_SALES']}|{d['ID_PROD']}|{d['NAME_PROD']}|{d['UNIT']}|{d['AMOUNT']}|{d['COST']}|{d['TOTAL']}\n")
        print("DB: Detalles guardados.")

    def get_max_sale_id(self):
        if not self.ventas:
            return 0
        return max(int(k) for k in self.ventas.keys())

    def create_sale(self, data):
        new_id = str(self.get_max_sale_id() + 1)
        data["ID_SALES"] = new_id
        self.ventas[new_id] = data
        self.save_ventas()
        print("se retorna")
        return new_id

    def read_sale(self, sale_id):
        return self.ventas.get(sale_id)

    def update_sale(self, sale_id, data):
        if sale_id in self.ventas:
            self.ventas[sale_id].update(data)
            self.save_ventas()
            return True
        return False

    def delete_sale(self, sale_id):
        if sale_id in self.ventas:
            del self.ventas[sale_id]
            self.save_ventas()
            # Eliminar detalles asociados:
            self.detalles = [d for d in self.detalles if d["ID_SALES"] != sale_id]
            self.save_detalles()
            return True
        return False

    # CRUD Detalles
    def add_detail(self, sale_id, detail_data):
        detail_data["ID_SALES"] = sale_id
        self.detalles.append(detail_data)
        self.save_detalles()

    def get_details(self, sale_id):
        return [d for d in self.detalles if d["ID_SALES"] == sale_id]

    def update_detail(self, sale_id, id_prod, fields):
        found = False
        for d in self.detalles:
            if d["ID_SALES"] == sale_id and d["ID_PROD"] == id_prod:
                d.update(fields)
                found = True
                break
        if found:
            self.save_detalles()
        return found

    def delete_detail(self, sale_id, id_prod):
        original_len = len(self.detalles)
        self.detalles = [d for d in self.detalles if not (d["ID_SALES"] == sale_id and d["ID_PROD"] == id_prod)]
        if len(self.detalles) != original_len:
            self.save_detalles()
            return True
        return False