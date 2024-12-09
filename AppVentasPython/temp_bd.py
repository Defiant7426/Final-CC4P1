import os
from db import VentasDB
from app_ventas import AppVentas

# Crear archivos de prueba con datos ficticios
sales_file = "test_ventas.txt"
details_file = "test_detalle_ventas.txt"

ventas_data = [
    {"ID_SALES": "1", "RUC": "123456789", "NAME": "Cliente A", "COST_TOTAL": "100.00"},
    {"ID_SALES": "2", "RUC": "987654321", "NAME": "Cliente B", "COST_TOTAL": "200.00"},
    {"ID_SALES": "3", "RUC": "456789123", "NAME": "Cliente C", "COST_TOTAL": "300.00"},
    {"ID_SALES": "4", "RUC": "654321987", "NAME": "Cliente D", "COST_TOTAL": "400.00"},
    {"ID_SALES": "5", "RUC": "789123456", "NAME": "Cliente E", "COST_TOTAL": "500.00"}
]

detalles_data = [
    {"ID_SALES": "1", "ID_PROD": "1", "NAME_PROD": "Producto A", "UNIT": "Unidad", "AMOUNT": "1", "COST": "100.00", "TOTAL": "100.00"},
    {"ID_SALES": "2", "ID_PROD": "2", "NAME_PROD": "Producto B", "UNIT": "Unidad", "AMOUNT": "2", "COST": "100.00", "TOTAL": "200.00"},
    {"ID_SALES": "3", "ID_PROD": "3", "NAME_PROD": "Producto C", "UNIT": "Unidad", "AMOUNT": "3", "COST": "100.00", "TOTAL": "300.00"},
    {"ID_SALES": "4", "ID_PROD": "4", "NAME_PROD": "Producto D", "UNIT": "Unidad", "AMOUNT": "4", "COST": "100.00", "TOTAL": "400.00"},
    {"ID_SALES": "5", "ID_PROD": "5", "NAME_PROD": "Producto E", "UNIT": "Unidad", "AMOUNT": "5", "COST": "100.00", "TOTAL": "500.00"}
]

with open(sales_file, "w", encoding='utf-8') as f:
    for venta in ventas_data:
        f.write(f"{venta['ID_SALES']}|{venta['RUC']}|{venta['NAME']}|{venta['COST_TOTAL']}\n")

with open(details_file, "w", encoding='utf-8') as f:
    for detalle in detalles_data:
        f.write(f"{detalle['ID_SALES']}|{detalle['ID_PROD']}|{detalle['NAME_PROD']}|{detalle['UNIT']}|{detalle['AMOUNT']}|{detalle['COST']}|{detalle['TOTAL']}\n")

# Crear una instancia de VentasDB y cargar los datos
db = VentasDB(sales_file=sales_file, details_file=details_file)
db.load()

# Crear una instancia de AppVentas
app = AppVentas(db=db, raft_node=None)

# Probar la creación de una nueva venta
new_sale = {"RUC": "123456789", "NAME": "Cliente F", "COST_TOTAL": "600.00"}
sale_id = app.create_sale(new_sale)
print(f"Nueva venta creada con ID: {sale_id}")

# Probar la lectura de una venta
sale = app.read_sale(sale_id)
print(f"Venta leída: {sale}")

# Probar la actualización de una venta
update_data = {"ID_SALES": sale_id, "NAME": "Cliente F Actualizado", "COST_TOTAL": "650.00"}
app.update_sale(update_data)
print(f"Venta actualizada: {app.read_sale(sale_id)}")

# Probar la eliminación de una venta
app.delete_sale(sale_id)
print(f"Venta después de eliminar: {app.read_sale(sale_id)}")

# Limpiar archivos de prueba
os.remove(sales_file)
os.remove(details_file)