package com.example.test1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration)

        val name = findViewById<EditText>(R.id.name)
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val registerBtn = findViewById<Button>(R.id.registerBtn)

        registerBtn.setOnClickListener {
            val nameText = name.text.toString()
            val emailText = email.text.toString()
            val passText = password.text.toString()

            registerUser(nameText, emailText, passText)
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("name", name)
        json.put("email", email)
        json.put("password", password)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("http://192.168.1.5:5000/register") // CHANGE THIS <<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                println(response.body?.string())
            }
        })
    }
}