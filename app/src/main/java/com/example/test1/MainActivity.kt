package com.example.test1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import android.widget.Toast
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.TextView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val textView: TextView = findViewById(R.id.title)
        val textView2: TextView = findViewById(R.id.subTitle)

        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val email = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.email)
        val password = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.password)
        val registerBtn = findViewById<TextView>(R.id.registerBtn)

        registerBtn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        textView.setTextColor(android.graphics.Color.WHITE)
        textView2.setTextColor(android.graphics.Color.WHITE)

        textView.post {
            val paint = textView.paint
            val height = textView.height.toFloat()

            val textWidth = paint.measureText(textView.text.toString()) // ya idk

            val gradient = LinearGradient(
                0f, 0f,
                0f, height,
                intArrayOf(
                    android.graphics.Color.WHITE,
                    "#2196F3".toColorInt()
                ),
                floatArrayOf(0.0f, 0.3f),
                Shader.TileMode.CLAMP
            )

            paint.shader = gradient
            textView.invalidate()
        }

        textView2.post {
            val paint = textView2.paint
            val height = textView2.height.toFloat()

            val textWidth = paint.measureText(textView2.text.toString()) // ya idk

            val gradient = LinearGradient(
                0f, 0f,
                0f, height,
                intArrayOf(
                    android.graphics.Color.WHITE,
                    "#2196F3".toColorInt()
                ),
                floatArrayOf(0.4f, 100f),
                Shader.TileMode.CLAMP
            )

            paint.shader = gradient
            textView2.invalidate()
        }

        loginBtn.setOnClickListener {

            val emailText = email.text.toString().trim()
            val passText = password.text.toString().trim()

            if (emailText.isEmpty() || passText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val client = OkHttpClient()

            val json = JSONObject()
            json.put("email", emailText)
            json.put("password", passText)

            val body = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
//                .url("http://10.0.2.2:5000/login")
                .url("http://192.168.1.25:5000/login")
//                .url("http://192.168.1.7:5000/login")

                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Network error. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {

                    val responseBody = response.body?.string()

                    runOnUiThread {

                        if (response.isSuccessful && responseBody != null) {

                            try {
                                val json = JSONObject(responseBody)

                                val id = json.getInt("id")
                                val username = json.getString("username")
                                val email = json.getString("email")
                                val profilepic = json.getString("profilepic")
                                val createdAt = json.getString("createdAt")

                                val intent = Intent(this@MainActivity, HomePageActivity::class.java)

                                intent.putExtra("id", id)
                                intent.putExtra("username", username)
                                intent.putExtra("email", email)
                                intent.putExtra("profilepic", profilepic)
                                intent.putExtra("createdAt", createdAt)

                                startActivity(intent)
                                finish()

                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Parse error", Toast.LENGTH_SHORT).show()
                            }

                        } else {
                            Toast.makeText(this@MainActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }
}