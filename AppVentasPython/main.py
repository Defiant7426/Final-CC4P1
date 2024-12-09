from flask import Flask
from db import VentasDB
from app_ventas import AppVentas
from raft_node import RaftNode

# Importar handlers
from handlers.create_sale import create_sale_handler
from handlers.read_sale import read_sale_handler
# (Crear de forma similar update_sale, delete_sale)
# Igualmente los handlers de appendEntries, requestVote, appendLogEntry, replicateEntry, status

# from handlers.update_sale import update_sale_handler
# from handlers.delete_sale import delete_sale_handler
from handlers.append_entries_handler import append_entries_handler
from handlers.request_vote_handler import request_vote_handler
from handlers.append_log_entry_handler import append_log_entry_handler
from handlers.replicate_entry_handler import replicate_entry_handler
from handlers.status_handler import status_handler

import sys

def main():
    if len(sys.argv) < 3:
        print("Uso: python main.py <puerto> <peers separados por coma>")
        return

    port = int(sys.argv[1])
    peers_arg = sys.argv[2]
    peers = [p.strip() for p in peers_arg.split(",")]

    db = VentasDB("test_ventas.txt", "test_detalle_ventas.txt")
    db.load()
    raft_node = RaftNode(port, peers)
    service = AppVentas(db, raft_node)

    app = Flask(__name__)

    # Registrar rutas
    create_sale_handler(app, service, raft_node)
    read_sale_handler(app, service, raft_node)
    # update_sale_handler(app, service, raft_node)
    # delete_sale_handler(app, service, raft_node)
    append_entries_handler(app, raft_node)
    request_vote_handler(app, raft_node)
    append_log_entry_handler(app, raft_node, service)
    replicate_entry_handler(app, raft_node, service)
    status_handler(app, raft_node)

    app.run(host='0.0.0.0', port=port)

if __name__ == "__main__":
    main()
