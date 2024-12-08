// server.js
const express = require('express');
//const fetch = require('node-fetch'); // Para Node.js <=17, en Node.js 18+ fetch nativo está disponible.

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// URL del servicio almacen en java:
const ALMACEN_URL = "http://localhost:8082";

// Rutas del cliente:

// Crear un producto en el almacén
// Se espera que el cliente envíe por POST los datos: NAME_PROD, DETAIL, UNIT, AMOUNT, COST
app.post('/createProduct', async (req, res) => {
    const { NAME_PROD, DETAIL, UNIT, AMOUNT, COST } = req.body;

    // Realizar la petición al servicio Almacén (Java)
    // POST /create con application/x-www-form-urlencoded
    const params = new URLSearchParams();
    params.append("NAME_PROD", NAME_PROD);
    params.append("DETAIL", DETAIL);
    params.append("UNIT", UNIT);
    params.append("AMOUNT", AMOUNT);
    params.append("COST", COST);

    try {
        const response = await fetch(`${ALMACEN_URL}/create`, {
            method: 'POST',
            body: params,
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });

        const data = await response.json();
        return res.json(data);
    } catch (err) {
        console.error(err);
        return res.status(500).json({error:"Error al comunicarse con el servicio Almacén"});
    }
});

// Leer información de un producto por ID_PROD
// GET /product/:id
app.get('/product/:id', async (req, res) => {
    const id = req.params.id;

    try {
        const response = await fetch(`${ALMACEN_URL}/read?id=${id}`, {
            method: 'GET'
        });
        const data = await response.json();
        return res.json(data);
    } catch (err) {
        console.error(err);
        return res.status(500).json({error:"Error al comunicarse con el servicio Almacén"});
    }
});

// Actualizar producto
// POST /updateProduct con ID_PROD y campos a actualizar
app.post('/updateProduct', async (req, res) => {
    // Ejemplo body: { ID_PROD: "P123456", NAME_PROD: "Nuevo Nombre", AMOUNT: "50" }
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
        const data = await response.json();
        return res.json(data);
    } catch (err) {
        console.error(err);
        return res.status(500).json({error:"Error al comunicarse con el servicio Almacén"});
    }
});

// Eliminar producto
// DELETE /deleteProduct/:id
app.delete('/deleteProduct/:id', async (req, res) => {
    const id = req.params.id;
    try {
        const response = await fetch(`${ALMACEN_URL}/delete?id=${id}`, {
            method: 'DELETE'
        });
        const data = await response.json();
        return res.json(data);
    } catch (err) {
        console.error(err);
        return res.status(500).json({error:"Error al comunicarse con el servicio Almacén"});
    }
});

// Endpoint para ver el estado del nodo RAFT en Java (opcional)
app.get('/status', async (req,res) => {
    try {
        const response = await fetch(`${ALMACEN_URL}/status`);
        const data = await response.json();
        return res.json(data);
    } catch (err) {
        console.error(err);
        return res.status(500).json({error:"Error al comunicarse con el servicio Almacén"});
    }
});

// Iniciar el servidor Node.js
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Cliente Node.js corriendo en http://localhost:${PORT}`);
});
