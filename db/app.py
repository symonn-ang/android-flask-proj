from flask import Flask, request, jsonify
from flask_cors import CORS
import mysql.connector
import bcrypt
import os
from werkzeug.utils import secure_filename
from flask import send_from_directory
import time


app = Flask(__name__)
CORS(app)   # cross-origin 

db = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="testdb"
)

UPLOAD_FOLDER = 'uploads'
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route('/uploads/<filename>')
def uploaded_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)

# global functions up there ^^^
# cursor = db.cursor() global chekc later

@app.route('/register', methods=['POST'])
def register():
    cursor = db.cursor()
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

    except mysql.connector.Error as err:

        if err.errno == 1062:
            return jsonify({
                "status": "error",
                "message": "Email already exists"
            }), 409

        return jsonify({
            "status": "error",
            "message": str(err)
        }), 500

    except Exception as e:

        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()
    

@app.route('/login', methods=['POST'])
def login():
    cursor = db.cursor()

        # http://10.0.2.2:5000//uploads/{filename}
        # http://192.168.1.7:5000/uploads/{filename}
        # http://192.168.1.25:5000/uploads/{filename}

    try:
        data = request.get_json()

        email = data.get('email')
        password = data.get('password')

        cursor.execute("""
            SELECT id, username, email, password, profilepic, createdAt
            FROM users
            WHERE email = %s
        """, (email,))

        result = cursor.fetchone()

        if not result:
            return jsonify({"status": "error", "message": "User not found"}), 404

        user_id, username, email, stored_password, profilepic, createdAt = result

        if bcrypt.checkpw(password.encode('utf-8'), stored_password.encode('utf-8')):
            profilepic_url = None
            if profilepic:
                filename = os.path.basename(profilepic)
                profilepic_url = f"http://192.168.1.25:5000/uploads/{filename}"
            return jsonify({
                "status": "success",
                "message": "Login successful",
                "id": user_id,
                "username": username,
                "email": email,
                "profilepic": profilepic_url,
                "createdAt": str(createdAt).split(" ")[0]
            }), 200

        else:
            return jsonify({"status": "error", "message": "Wrong password"}), 401

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        cursor.close()

@app.route('/upload_profile_pic', methods=['POST'])
def upload_profile_pic():

    cursor = db.cursor()

    try:

        user_id = request.form.get('user_id')

        if 'image' not in request.files:
            return jsonify({
                "status": "error",
                "message": "No image uploaded"
            }), 400

        image = request.files['image']

        filename = f"{user_id}_{int(time.time())}.jpg"

        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)

        image.save(filepath)

        query = """
            UPDATE users
            SET profilepic = %s
            WHERE id = %s
        """

        cursor.execute(query, (filepath, user_id))
        db.commit()

        return jsonify({
            "status": "success",
            "filepath": filepath
        })

    except Exception as e:

        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()


        

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)