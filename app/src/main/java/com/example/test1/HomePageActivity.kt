package com.example.test1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomePageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)

        val usernameText = findViewById<TextView>(R.id.usernameLayout)
        val emailText = findViewById<TextView>(R.id.emailLayout)
        val dateText = findViewById<TextView>(R.id.dateLayout)

        val username = intent.getStringExtra("username")
        val email = intent.getStringExtra("email")
        val createdAt = intent.getStringExtra("createdAt")

        val logoutBtn = findViewById<ImageView>(R.id.logoutBtn)

        usernameText.text = username
        emailText.text = email
        dateText.text = createdAt

        logoutBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
}