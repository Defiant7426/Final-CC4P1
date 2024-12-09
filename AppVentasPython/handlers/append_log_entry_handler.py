from flask import request, jsonify

def append_log_entry_handler(app, raft, service):
    @app.route("/appendLogEntry", methods=["POST"])
    def append_log_entry():
        data = request.get_json()
        entry = data.get("entry","")
        index = raft.append_log_entry(entry)
        # Como es el líder, aplicamos el comando inmediatamente a la BD local
        service.apply_command(entry)
        # Replicar a otros
        raft.replicate_log_entry(index, entry)
        return str(index),200
    return app
