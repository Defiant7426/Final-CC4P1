from flask import request, jsonify

def request_vote_handler(app, raft):
    @app.route("/requestVote", methods=["POST"])
    def request_vote():
        data = request.get_json()
        term = data.get("term",0)
        candidateId = data.get("candidateId","")
        voteGranted = raft.handle_request_vote(term, candidateId)
        return jsonify({"voteGranted":voteGranted}),200
    return app
