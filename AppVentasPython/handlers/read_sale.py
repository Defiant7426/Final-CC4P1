from flask import request, jsonify
def read_sale_handler(app, service, raft):
    @app.route("/readSale", methods=["GET"])
    def read_sale():
        sale_id = request.args.get("id")
        if not sale_id:
            return jsonify({"error":"ID_SALES requerido"}),400

        if not raft.is_leader():
            leader = raft.get_leader()
            if leader is None:
                return jsonify({"error":"No leader"}),503
            # Redirigir
            import requests
            try:
                r = requests.get(leader+"/readSale?id="+sale_id, timeout=2)
                return (r.text, r.status_code, r.headers.items())
            except:
                return jsonify({"error":"No se pudo conectar con el líder"}), 502

        # Es líder
        record = service.read_sale(sale_id)
        if record is None:
            return jsonify({"error":"No encontrado"}),404
        return jsonify(record),200
    return app
