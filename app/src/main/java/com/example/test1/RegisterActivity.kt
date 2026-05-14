package com.example.test1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// for flask vvv
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
// for flask ^^^
class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration)

        val username = findViewById<EditText>(R.id.name)
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val backBtn = findViewById<Button>(R.id.backBtn)

        registerBtn.setOnClickListener {
            val nameText = username.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passText = password.text.toString().trim()

            if (nameText.isEmpty() || emailText.isEmpty() || passText.isEmpty()) {
                Toast.makeText(this, "Please answer all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passText.length < 6) {
                Toast.makeText(this, "Please enter 6 or more characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(nameText, emailText, passText)
        }

        backBtn.setOnClickListener {
            finish();
        }

    }

    private fun registerUser(username: String, email: String, password: String) {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("username", username)
        json.put("email", email)
        json.put("password", password)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("http://10.0.2.2:5000/register")
//            .url("http://192.168.1.25:5000/register") // CHANGE THIS sa ipconfig <<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Network Error. Check your connection.", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Registration Successful!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Failed: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
                println("Response: $responseBody")
            }
        })
    }
}