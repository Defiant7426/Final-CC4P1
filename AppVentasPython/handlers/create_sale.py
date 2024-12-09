from flask import request, jsonify
from functools import wraps

def create_sale_handler(app, service, raft):
    @app.route("/createSale", methods=["POST"])
    def create_sale():
        if not raft.is_leader():
            # Redirigir al líder
            fields = request.form.to_dict()
            # Crear comando
            command = "create_sale " + " ".join(f"{k}={v}" for k,v in fields.items())
            res = raft.redirect_to_leader(command)
            if res == -1:
                return jsonify({"error":"Error redirigiendo al líder"}), 500
            return jsonify({"status":"redirected"}), 200

        # Si es líder
        fields = request.form.to_dict()
        sale_id = service.create_sale(fields)
        # Construir comando
        command = "create_sale " + " ".join(f"{k}={v}" for k,v in fields.items())
        index = raft.append_log_entry(command)
        raft.replicate_log_entry(index, command)

        return jsonify({"status":"accepted","id":sale_id}), 202
    return app
