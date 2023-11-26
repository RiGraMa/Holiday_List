package com.example.holidaylist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {

    // Lateinit var for FirebaseAuth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // Linking the UI elements to the code
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val textViewRegister = findViewById<TextView>(R.id.textViewRegister)

        // Set an onClickListener on the login button
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Check if email and password are not empty
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_LONG).show()
            }
        }

        textViewRegister.setOnClickListener {
            // Start the RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    // Function to log in the user
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login success
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If login fails, display a message to the user
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    // Function to update UI after login
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, ItemManagementActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
