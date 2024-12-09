from flask import jsonify

def status_handler(app, raft):
    @app.route("/status", methods=["GET"])
    def status():
        st = raft.get_status()
        return jsonify({"status":st}), 200
    return app
