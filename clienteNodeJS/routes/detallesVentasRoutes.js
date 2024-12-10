// routes/detallesVentasRoutes.js
const express = require('express');
const { body, validationResult } = require('express-validator');
//const fetch = require('node-fetch'); // Solo necesario si usas Node.js < 18

module.exports = (DETALLES_VENTAS_URL) => {
    const router = express.Router();

    // Crear un detalle de venta con validaciones
    router.post('/createDetalle', [
        body('ID_SALES').notEmpty().withMessage('ID_SALES es obligatorio'),
        body('ID_PROD').notEmpty().withMessage('ID_PROD es obligatorio'),
        body('NAME_PROD').notEmpty().withMessage('NAME_PROD es obligatorio'),
        body('UNIT').notEmpty().withMessage('UNIT es obligatorio'),
        body('AMOUNT').isInt({ min: 1 }).withMessage('AMOUNT debe ser un número entero mayor que 0'),
        body('COST').isFloat({ min: 0 }).withMessage('COST debe ser un número positivo'),
        body('TOTAL').isFloat({ min: 0 }).withMessage('TOTAL debe ser un número positivo')
    ], async (req, res) => {
        // Manejar errores de validación
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({ errors: errors.array() });
        }

        console.log("Creando detalle de venta...");
        const { ID_SALES, ID_PROD, NAME_PROD, UNIT, AMOUNT, COST, TOTAL } = req.body;

        const params = new URLSearchParams();
        params.append("ID_SALES", ID_SALES);
        params.append("ID_PROD", ID_PROD);
        params.append("NAME_PROD", NAME_PROD);
        params.append("UNIT", UNIT);
        params.append("AMOUNT", AMOUNT);
        params.append("COST", COST);
        params.append("TOTAL", TOTAL);

        console.log("Enviando datos a DetallesVentas:", params);

        try {
            const response = await fetch(`${DETALLES_VENTAS_URL}/createDetalle`, {
                method: 'POST',
                body: params,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });

            if (!response.ok) {
                const errorData = await response.json();
                return res.status(response.status).json(errorData);
            }

            const data = await response.json();
            console.log("Detalle de venta creado:", data);
            return res.json(data);
        } catch (err) {
            console.error(err);
            return res.status(500).json({ error: "Error al comunicarse con el servicio DetallesVentas" });
        }
    });

    // Leer información de un detalle de venta por ID_SALES e ID_PROD
    router.get('/detalle/:id_sales/:id_prod', async (req, res) => {
        const idSales = req.params.id_sales;
        const idProd = req.params.id_prod;

        // Validar que ambos parámetros no estén vacíos
        if (!idSales || !idProd) {
            return res.status(400).json({ error: "id_sales o id_prod no proporcionado" });
        }

        try {
            const response = await fetch(`${DETALLES_VENTAS_URL}/readDetalle?id_sales=${idSales}&id_prod=${idProd}`, {
                method: 'GET'
            });

            if (!response.ok) {
                const errorData = await response.json();
                return res.status(response.status).json(errorData);
            }

            const data = await response.json();
            return res.json(data);
        } catch (err) {
            console.error(err);
            return res.status(500).json({ error: "Error al comunicarse con el servicio DetallesVentas" });
        }
    });

    // Actualizar un detalle de venta con validaciones
    router.post('/updateDetalle', [
        body('ID_SALES').notEmpty().withMessage('ID_SALES es obligatorio'),
        body('ID_PROD').notEmpty().withMessage('ID_PROD es obligatorio'),
        // Puedes agregar más validaciones para otros campos si es necesario
    ], async (req, res) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({ errors: errors.array() });
        }

        const params = new URLSearchParams();
        for (const [key, value] of Object.entries(req.body)) {
            params.append(key, value);
        }

        try {
            const response = await fetch(`${DETALLES_VENTAS_URL}/updateDetalle`, {
                method: 'POST',
                body: params,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });

            if (!response.ok) {
                const errorData = await response.json();
                return res.status(response.status).json(errorData);
            }

            const data = await response.json();
            return res.json(data);
        } catch (err) {
            console.error(err);
            return res.status(500).json({ error: "Error al comunicarse con el servicio DetallesVentas" });
        }
    });

    // Eliminar un detalle de venta
    router.delete('/deleteDetalle/:id_sales/:id_prod', async (req, res) => {
        const idSales = req.params.id_sales;
        const idProd = req.params.id_prod;

        // Validar que ambos parámetros no estén vacíos
        if (!idSales || !idProd) {
            return res.status(400).json({ error: "id_sales o id_prod no proporcionado" });
        }

        try {
            const params = new URLSearchParams();
            params.append('ID_SALES', idSales);
            params.append('ID_PROD', idProd);

            const response = await fetch(`${DETALLES_VENTAS_URL}/deleteDetalle`, {
                method: 'POST',
                body: params,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });

            if (!response.ok) {
                const errorData = await response.json();
                return res.status(response.status).json(errorData);
            }

            const data = await response.json();
            return res.json(data);
        } catch (err) {
            console.error(err);
            return res.status(500).json({ error: "Error al comunicarse con el servicio DetallesVentas" });
        }
    });

    return router;
};
