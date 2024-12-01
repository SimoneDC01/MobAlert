package com.example.mobalert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobalert.databinding.ActivityLoginBinding
import com.example.mobalert.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.signupButton.setOnClickListener {
            val email = binding.signupEmail.text.toString()
            val password = binding.signupPassword.text.toString()
            val cPassword = binding.signupConfirmPassword.text.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(email) .matches()){
                binding.signupEmail.error = "Invalid Email Pattern"
                binding.signupEmail.requestFocus()
            } else if (password.isEmpty()){
                binding.signupPassword.error = "Enter Password"
                binding.signupPassword.requestFocus()
            } else if (password != cPassword) {
                binding.signupConfirmPassword.error = "Password doesn't match"
                binding.signupConfirmPassword.requestFocus()
            } else {
                auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this){ task ->
                    if(task.isSuccessful){
                        Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    else {
                        Toast.makeText(this, "Signup Failed", Toast.LENGTH_SHORT).show()
                        Log.d("LOGIN", "Error: ${task.exception}")
                    }

                }
            }
        }

        binding.goToLoginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}