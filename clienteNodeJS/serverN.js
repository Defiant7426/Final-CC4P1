// server.js
const express = require('express');
const dotenv = require('dotenv');
const cors = require('cors'); // Opcional, para manejar CORS
const almacenRoutes = require('./routes/almacenRoutes');
const ventasRoutes = require('./routes/ventasRoutes');
const detallesVentasRoutes = require('./routes/detallesVentasRoutes');

dotenv.config();

const app = express();

// Middleware
app.use(cors()); // Opcional
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Rutas
app.use('/almacen', almacenRoutes(process.env.ALMACEN_URL));
app.use('/ventas', ventasRoutes(process.env.VENTAS_URL));
app.use('/detallesVentas', detallesVentasRoutes(process.env.DETALLES_VENTAS_URL));

// Ruta de prueba
app.get('/', (req, res) => {
    res.send("Cliente Node.js para AppAlmacen, AppVentas y DetallesVentas está funcionando.");
});

// Middleware de manejo de errores
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Algo salió mal!' });
});

// Iniciar el servidor
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Cliente Node.js corriendo en http://localhost:${PORT}`);
});
