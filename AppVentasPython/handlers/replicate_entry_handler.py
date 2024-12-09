from flask import request, jsonify

def replicate_entry_handler(app, raft, service):
    @app.route("/replicateEntry", methods=["POST"])
    def replicate_entry():
        data = request.get_json()
        command = data.get("entry","")
        index = raft.append_log_entry(command)
        # Aplicar el comando en el seguidor
        service.apply_command(command)
        return str(index),200
    return app
