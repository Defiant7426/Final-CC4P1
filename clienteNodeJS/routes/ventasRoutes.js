// routes/ventasRoutes.js
const express = require('express');
const { body, validationResult } = require('express-validator');
//const fetch = require('node-fetch'); // Solo necesario si usas Node.js < 18

module.exports = (VENTAS_URL) => {
    const router = express.Router();   

    // Crear una venta en AppVentas con validaciones
    router.post('/createSale', [
        body('RUC').notEmpty().withMessage('RUC es obligatorio'),
        body('NAME').notEmpty().withMessage('NAME es obligatorio'),
        body('COST_TOTAL').isFloat({ min: 0 }).withMessage('COST_TOTAL debe ser un número positivo')
    ], async (req, res) => {
        // Manejar errores de validación
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({ errors: errors.array() });
        }

        console.log("Creando venta en AppVentas...");
        const { RUC, NAME, COST_TOTAL } = req.body;

        const params = new URLSearchParams();
        params.append("RUC", RUC);
        params.append("NAME", NAME);
        params.append("COST_TOTAL", COST_TOTAL);

        console.log("Enviando datos a Ventas:", params);

        try {
            const response = await fetch(`${VENTAS_URL}/createVenta`, {
                method: 'POST',
                body: params,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });

            if (!response.ok) {
                const errorData = await response.json();
                return res.status(response.status).json(errorData);
            }

            const data = await response.json();
            console.log("Venta creada en AppVentas:", data);
            return res.json(data);
        } catch (err) {
            console.error(err);
            return res.status(500).json({ error: "Error al comunicarse con el servicio Ventas" });
        }
    });

    // Leer información de una venta por ID_SALES
router.get('/sale/:id', async (req, res) => {
    const id = req.params.id;

    try {
        const response = await fetch(`${VENTAS_URL}/readVenta?id=${id}`, {
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
        return res.status(500).json({ error: "Error al comunicarse con el servicio Ventas" });
    }
});

    // Actualizar una venta con validaciones
    router.post('/updateSale', [
        // Validación para asegurar que ID_SALES está presente y no está vacío
        body('ID_SALES')
            .notEmpty()
            .withMessage('ID_SALES es obligatorio')
            .isInt({ min: 1 })
            .withMessage('ID_SALES debe ser un número entero válido'),
    
        // Validaciones opcionales para otros campos que puedan actualizarse
        body('RUC')
            .optional()
            .isString()
            .withMessage('RUC debe ser una cadena de texto válida'),
    
        body('NAME')
            .optional()
            .isString()
            .withMessage('NAME debe ser una cadena de texto válida'),
    
        body('COST_TOTAL')
            .optional()
            .isFloat({ min: 0 })
            .withMessage('COST_TOTAL debe ser un número positivo')
    ], async (req, res) => {
        // Manejar errores de validación
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            // Retornar errores de validación al cliente
            return res.status(400).json({ errors: errors.array() });
        }
    
        // Extraer los campos necesarios del cuerpo de la solicitud
        const { ID_SALES, RUC, NAME, COST_TOTAL } = req.body;
    
        // Crear los parámetros para enviar al servidor en formato x-www-form-urlencoded
        const params = new URLSearchParams();
        params.append('ID_SALES', ID_SALES);
        
        // Agregar campos opcionales si están presentes
        if (RUC !== undefined) params.append('RUC', RUC);
        if (NAME !== undefined) params.append('NAME', NAME);
        if (COST_TOTAL !== undefined) params.append('COST_TOTAL', COST_TOTAL);
    
        try {
            // Realizar la solicitud POST al servidor para actualizar la venta
            const response = await fetch(`${process.env.VENTAS_URL}/updateVenta`, { // Asegúrate de tener VENTAS_URL definida en tus variables de entorno
                method: 'POST',
                body: params,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });
    
            // Verificar si la respuesta del servidor no es exitosa
            if (!response.ok) {
                // Intentar obtener los detalles del error del servidor
                let errorData;
                try {
                    errorData = await response.json();
                } catch (e) {
                    errorData = { error: 'Error desconocido del servidor' };
                }
                return res.status(response.status).json(errorData);
            }
    
            // Obtener los datos de la respuesta exitosa
            const data = await response.json();
            return res.json(data);
        } catch (err) {
            // Manejar errores de comunicación con el servidor
            console.error('Error al actualizar la venta:', err);
            return res.status(500).json({ error: "Error al comunicarse con el servicio Ventas" });
        }
    });

    // Eliminar una venta
    router.delete('/deleteSale/:id', async (req, res) => {
        const id = req.params.id;
        try {
            const params = new URLSearchParams();
            params.append('ID_SALES', id);

            const response = await fetch(`${VENTAS_URL}/deleteVenta`, {
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
            return res.status(500).json({ error: "Error al comunicarse con el servicio Ventas" });
        }
    });

    return router;
};
