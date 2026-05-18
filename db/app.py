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
# http://10.0.2.2:5000//uploads/{filename}
# http://192.168.1.7:5000/uploads/{filename}
# http://192.168.1.25:5000/uploads/{filename}

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

        # http://10.0.2.2:5000//uploads/{filename}
        # http://192.168.1.7:5000/uploads/{filename}
        # http://192.168.1.25:5000/uploads/{filename}

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

@app.route('/create_post', methods=['POST'])
def create_post():
    cursor = db.cursor()

    try:
        user_id = request.form.get('user_id')
        post_text = request.form.get('post_text')
        
        image_path = None

        if 'image' in request.files:
            image = request.files['image']

            filename = f"{user_id}_{int(time.time())}.jpg"
            filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)

            image.save(filepath)

            image_path = filepath

        query = """
            INSERT INTO posts (user_id, post_text, post_image)
            VALUES (%s, %s, %s)
        """

        cursor.execute(query, (user_id, post_text, image_path))
        db.commit()

        post_id = cursor.lastrowid
        
        return jsonify({
            "status": "success",
            "post_id": post_id
        }), 201

    except Exception as e:
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()

@app.route('/toggle_like', methods=['POST'])
def toggle_like():

    cursor = db.cursor()

    try:
        post_id = request.form.get('post_id')
        user_id = request.form.get('user_id')

        # check if likd
        cursor.execute("""
            SELECT id
            FROM post_likes
            WHERE post_id = %s AND user_id = %s
        """, (post_id, user_id))

        existing = cursor.fetchone()

        # unlike
        if existing:

            cursor.execute("""
                DELETE FROM post_likes
                WHERE post_id = %s AND user_id = %s
            """, (post_id, user_id))

            cursor.execute("""
                UPDATE posts
                SET like_count = like_count - 1
                WHERE id = %s
            """, (post_id,))

            db.commit()

            return jsonify({
                "liked": False
            })

        # like
        else:

            cursor.execute("""
                INSERT INTO post_likes (post_id, user_id)
                VALUES (%s, %s)
            """, (post_id, user_id))

            cursor.execute("""
                UPDATE posts
                SET like_count = like_count + 1
                WHERE id = %s
            """, (post_id,))

            db.commit()

            return jsonify({
                "liked": True
            })

    except Exception as e:

        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()

@app.route('/delete_post', methods=['POST'])
def delete_post():
    cursor = db.cursor()

    try:
        post_id = request.form.get("post_id")
        user_id = request.form.get("user_id")

        # unique del
        cursor.execute("""
            DELETE FROM posts
            WHERE id = %s AND user_id = %s
        """, (post_id, user_id))

        db.commit()

        return jsonify({
            "status": "success"
        })

    except Exception as e:
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()

@app.route('/edit_post', methods=['POST'])
def edit_post():

    cursor = db.cursor()

    try:
        post_id = request.form.get("post_id")
        user_id = request.form.get("user_id")
        post_text = request.form.get("post_text")

        image_path = None

        if 'image' in request.files:
            image = request.files['image']

            filename = f"{post_id}_{int(time.time())}.jpg"
            filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)

            image.save(filepath)
            image_path = filepath
        
        if image_path:
            cursor.execute("""
                UPDATE posts
                SET post_text = %s,
                    post_image = %s
                WHERE id = %s AND user_id = %s
            """, (post_text, image_path, post_id, user_id))
        
        else:
            cursor.execute("""
                UPDATE posts
                SET post_text = %s
                WHERE id = %s AND user_id = %s
            """, (post_text, post_id, user_id))

        db.commit()

        return jsonify({
            "status": "success"
        })

    except Exception as e:

        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()

@app.route('/create_comment', methods=['POST'])
def create_comment():

    cursor = db.cursor()

    try:

        user_id = request.form.get("user_id")
        post_id = request.form.get("post_id")
        comment_text = request.form.get("comment_text")

        if not comment_text:
            return jsonify({
                "status": "error",
                "message": "Comment is empty"
            }), 400

        cursor.execute("""
            INSERT INTO comments (user_id, post_id, comment_text)
            VALUES (%s, %s, %s)
        """, (user_id, post_id, comment_text))

        cursor.execute("""
            UPDATE posts
            SET comment_count = comment_count + 1
            WHERE id = %s
        """, (post_id,))

        db.commit()

        return jsonify({
            "status": "success"
        })

    except Exception as e:

        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()




@app.route('/delete_comment', methods=['POST'])
def delete_comment():

    cursor = db.cursor()

    try:

        comment_id = request.form.get("comment_id")
        user_id = request.form.get("user_id")

        cursor.execute("""
            SELECT post_id
            FROM post_comments
            WHERE id = %s
        """, (comment_id,))

        result = cursor.fetchone()

        if not result:
            return jsonify({
                "status": "error",
                "message": "Comment not found"
            }), 404

        post_id = result[0]

        cursor.execute("""
            DELETE FROM post_comments
            WHERE id = %s AND user_id = %s
        """, (comment_id, user_id))

        cursor.execute("""
            UPDATE posts
            SET comment_count = comment_count - 1
            WHERE id = %s
        """, (post_id,))

        db.commit()

        return jsonify({
            "status": "success"
        })

    except Exception as e:

        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()


@app.route('/edit_comment', methods=['POST'])
def edit_comment():

    cursor = db.cursor()

    try:

        comment_id = request.form.get("comment_id")
        user_id = request.form.get("user_id")
        comment_text = request.form.get("comment_text")

        cursor.execute("""
            UPDATE post_comments
            SET comment_text = %s
            WHERE id = %s AND user_id = %s
        """, (comment_text, comment_id, user_id))

        db.commit()

        return jsonify({
            "status": "success"
        })

    except Exception as e:

        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()



# GET routes start here vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv



@app.route('/posts', methods=['GET'])
def get_posts():

    cursor = db.cursor()

    try:

        user_id = request.args.get('user_id')

        cursor.execute("""
            SELECT 
                p.id,
                p.user_id,
                u.username,
                u.email,
                u.profilepic,
                p.post_text,
                p.post_image,
                p.createdAt,
                p.like_count,
                p.comment_count,

                EXISTS(
                    SELECT 1
                    FROM post_likes pl
                    WHERE pl.post_id = p.id
                    AND pl.user_id = %s
                ) AS isLiked

            FROM posts p
            JOIN users u ON p.user_id = u.id
            ORDER BY p.createdAt DESC
        """, (user_id,))

        results = cursor.fetchall()

        posts = []

        for row in results:

            (
                id,
                user_id,
                username,
                email,
                profilepic,
                text,
                image,
                createdAt,
                like_count,
                comment_count,
                isLiked
            ) = row

            if profilepic:
                profilepic = f"http://192.168.1.25:5000/{profilepic}"

            if image:
                image = f"http://192.168.1.25:5000/{image}"

            posts.append({
                "id": id,
                "user_id": user_id,
                "username": username,
                "email": email,
                "profilepic": profilepic,
                "post_text": text,
                "post_image": image,
                "createdAt": str(createdAt),
                "likeCount": like_count,
                "commentCount": comment_count,
                "isLiked": bool(isLiked)
            })

        return jsonify(posts)

    except Exception as e:

        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()

@app.route('/comments', methods=['GET'])
def get_comments():

    cursor = db.cursor()

    try:

        post_id = request.args.get("post_id")

        cursor.execute("""
            SELECT
                c.id,
                c.user_id,
                c.post_id,
                c.comment_text,
                c.createdAt,
                u.username,
                u.profilepic

            FROM comments c

            JOIN users u
            ON c.user_id = u.id

            WHERE c.post_id = %s

            ORDER BY c.createdAt ASC
        """, (post_id,))

        results = cursor.fetchall()

        comments = []

        for row in results:

            (
                comment_id,
                user_id,
                post_id,
                comment_text,
                createdAt,
                username,
                profilepic
            ) = row

            if profilepic:
                profilepic = f"http://192.168.1.25:5000/%7Bprofilepic%7D"

            comments.append({
                "id": comment_id,
                "user_id": user_id,
                "post_id": post_id,
                "comment_text": comment_text,
                "createdAt": str(createdAt),
                "username": username,
                "profilepic": profilepic
            })

        return jsonify(comments)

    except Exception as e:

        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        cursor.close()



if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)