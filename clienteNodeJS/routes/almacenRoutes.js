// routes/almacenRoutes.js
const express = require('express');
const { body, validationResult } = require('express-validator');
//const fetch = require('node-fetch'); // Solo necesario si usas Node.js < 18

module.exports = (ALMACEN_URL) => {
    const router = express.Router();

    // Crear un producto en el almacén con validaciones
    router.post('/createProduct', [
        body('NAME_PROD').notEmpty().withMessage('NAME_PROD es obligatorio'),
        body('DETAIL').notEmpty().withMessage('DETAIL es obligatorio'),
        body('UNIT').notEmpty().withMessage('UNIT es obligatorio'),
        body('AMOUNT').isInt({ min: 1 }).withMessage('AMOUNT debe ser un número entero mayor que 0'),
        body('COST').isFloat({ min: 0 }).withMessage('COST debe ser un número positivo')
    ], async (req, res) => {
        // Manejar errores de validación
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({ errors: errors.array() });
        }

        console.log("Creando producto en el almacén...");
        const { NAME_PROD, DETAIL, UNIT, AMOUNT, COST } = req.body;

        const params = new URLSearchParams();
        params.append("NAME_PROD", NAME_PROD);
        params.append("DETAIL", DETAIL);
        params.append("UNIT", UNIT);
        params.append("AMOUNT", AMOUNT);
        params.append("COST", COST);

        console.log("Enviando datos al almacén:", params);

        try {
            const response = await fetch(`${ALMACEN_URL}/create`, {
                method: 'POST',
                body: params,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });

            if (!response.ok) {
                const errorData = await response.json();
                return res.status(response.status).json(errorData);
            }

            const data = await response.json();
            console.log("Producto creado en el almacén:", data);
            return res.json(data);
        } catch (err) {
            console.error(err);
            return res.status(500).json({ error: "Error al comunicarse con el servicio Almacén" });
        }
    });

    // Leer información de un producto por ID_PROD
    router.get('/product/:id', async (req, res) => {
        const id = req.params.id;

        try {
            const response = await fetch(`${ALMACEN_URL}/read?id=${id}`, {
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
            return res.status(500).json({ error: "Error al comunicarse con el servicio Almacén" });
        }
    });

    // Actualizar producto con validaciones
    router.post('/updateProduct', [
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
            const response = await fetch(`${ALMACEN_URL}/update`, {
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
            return res.status(500).json({ error: "Error al comunicarse con el servicio Almacén" });
        }
    });

    // Eliminar producto
    router.delete('/deleteProduct/:id', async (req, res) => {
        const id = req.params.id;
        try {
            const params = new URLSearchParams();
            params.append('ID_PROD', id);

            const response = await fetch(`${ALMACEN_URL}/delete`, {
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
            return res.status(500).json({ error: "Error al comunicarse con el servicio Almacén" });
        }
    });

    return router;
};
