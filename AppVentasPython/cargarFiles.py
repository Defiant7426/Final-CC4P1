import os

# Datos ficticios para ventas
ventas_data = [
    {"ID_SALES": "1", "RUC": "123456789", "NAME": "Cliente A", "COST_TOTAL": "100.00"},
    {"ID_SALES": "2", "RUC": "987654321", "NAME": "Cliente B", "COST_TOTAL": "200.00"},
    {"ID_SALES": "3", "RUC": "456789123", "NAME": "Cliente C", "COST_TOTAL": "300.00"},
    {"ID_SALES": "4", "RUC": "654321987", "NAME": "Cliente D", "COST_TOTAL": "400.00"},
    {"ID_SALES": "5", "RUC": "789123456", "NAME": "Cliente E", "COST_TOTAL": "500.00"}
]

# Datos ficticios para detalles de ventas
detalles_data = [
    {"ID_SALES": "1", "ID_PROD": "1", "NAME_PROD": "Producto A", "UNIT": "Unidad", "AMOUNT": "1", "COST": "100.00", "TOTAL": "100.00"},
    {"ID_SALES": "2", "ID_PROD": "2", "NAME_PROD": "Producto B", "UNIT": "Unidad", "AMOUNT": "2", "COST": "100.00", "TOTAL": "200.00"},
    {"ID_SALES": "3", "ID_PROD": "3", "NAME_PROD": "Producto C", "UNIT": "Unidad", "AMOUNT": "3", "COST": "100.00", "TOTAL": "300.00"},
    {"ID_SALES": "4", "ID_PROD": "4", "NAME_PROD": "Producto D", "UNIT": "Unidad", "AMOUNT": "4", "COST": "100.00", "TOTAL": "400.00"},
    {"ID_SALES": "5", "ID_PROD": "5", "NAME_PROD": "Producto E", "UNIT": "Unidad", "AMOUNT": "5", "COST": "100.00", "TOTAL": "500.00"}
]

# Crear archivos de prueba
sales_file = "test_ventas.txt"
details_file = "test_detalle_ventas.txt"

# Guardar datos ficticios en archivos .txt
with open(sales_file, "w", encoding='utf-8') as f:
    for venta in ventas_data:
        f.write(f"{venta['ID_SALES']}|{venta['RUC']}|{venta['NAME']}|{venta['COST_TOTAL']}\n")

with open(details_file, "w", encoding='utf-8') as f:
    for detalle in detalles_data:
        f.write(f"{detalle['ID_SALES']}|{detalle['ID_PROD']}|{detalle['NAME_PROD']}|{detalle['UNIT']}|{detalle['AMOUNT']}|{detalle['COST']}|{detalle['TOTAL']}\n")