from flask import request, jsonify

def append_entries_handler(app, raft):
    @app.route("/appendEntries", methods=["POST"])
    def append_entries():
        data = request.get_json()
        term = data.get("term",0)
        leaderId = data.get("leaderId",None)
        raft.handle_append_entries(term, leaderId)
        return jsonify({"success":True}),200
    return app