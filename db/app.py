from flask import Flask, request, jsonify
from flask_cors import CORS  # Add this
import mysql.connector

app = Flask(__name__)
CORS(app)   # cross-origin 

# use connection pooling
db = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="testdb"
)

cursor = db.cursor()

@app.route('/register', methods=['POST'])
def register():
    try:
        data = request.get_json()

        name = data.get('name')
        email = data.get('email')
        password = data.get('password')

        if not name or not email or not password:
            return jsonify({"status": "error", "message": "Missing fields"}), 400

        # hash use bcrypt or argon2
        query = "INSERT INTO users (name, email, password) VALUES (%s, %s, %s)"
        cursor.execute(query, (name, email, password))
        db.commit()

        return jsonify({"status": "success", "message": "User registered"}), 201

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)