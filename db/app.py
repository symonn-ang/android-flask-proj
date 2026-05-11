from flask import Flask, request, jsonify
from flask_cors import CORS
import mysql.connector
import bcrypt


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

        username = data.get('username')
        email = data.get('email')
        password = data.get('password')

        if not username or not email or not password:
            return jsonify({"status": "error", "message": "Missing fields"}), 400

        # hash deez use bcrypt or argon2

        hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())

        query = "INSERT INTO users (username, email, password) VALUES (%s, %s, %s)"
        cursor.execute(query, (username, email, hashed_password.decode('utf-8')))
        db.commit()

        return jsonify({"status": "success", "message": "User registered"}), 201

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    

@app.route('/login', methods=['POST'])
def login():
    try:
        data = request.get_json()

        email = data.get('email')
        password = data.get('password')

        cursor.execute("SELECT password FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()

        if not result:
            return jsonify({"status": "error", "message": "User not found"}), 404

        stored_password = result[0]

        if bcrypt.checkpw(password.encode('utf-8'), stored_password.encode('utf-8')):
            return jsonify({"status": "success", "message": "Login successful"}), 200
        else:
            return jsonify({"status": "error", "message": "Wrong password"}), 401

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    

    

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)