package com.example.mobalert

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mobalert.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()
        reference = database.reference.child("Users")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.loginWithGoogleButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()
            Log.d("LOGIN", "entrato. Email: $email, Password: $password")

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                //email pattern is invalid, show error
                binding.loginEmail.error = "Invalid Email"
                binding.loginEmail.requestFocus()
            } else if (password.isEmpty()){
                //password is not entered, show error
                binding.loginPassword.error = "Enter Password"
                binding.loginPassword.requestFocus()
            } else {
                auth.signInWithEmailAndPassword(email,password).addOnCompleteListener{ task ->
                    if(task.isSuccessful){
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    else {
                        Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                        Log.d("LOGIN", "Error: ${task.exception}")
                    }

                }
            }

        }

        binding.goToSignupButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    /*    binding.loggedButton.setOnClickListener {
            Log.d("LOGIN", "${auth.currentUser?.email}")
        }*/
    }

    private fun signInWithGoogle(){
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
        Log.d("LOGIN", "Login Successful")
        if(result.resultCode == RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>){
        if (task.isSuccessful) {
            val account:GoogleSignInAccount? = task.result
            if (account != null){
                updateUI(account)
            }
        }
        else {
            Toast.makeText(this, "SignIn Failed, Try again later", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount){
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnSuccessListener{ authResult ->

                if(authResult.additionalUserInfo!!.isNewUser) {
                    Log.d("LOGIN", "updateUserInfo: New User")
                    val hashMap = HashMap<String, Any>()
                    hashMap["uid"] = "$auth.uid"
                    hashMap["email"] = "${auth.currentUser?.email}"
                    hashMap["name"] = "${auth.currentUser?.displayName}"
                    hashMap["phoneNumber"] = ""
                    hashMap["dob"] = ""

                    reference.child( auth.uid.toString())
                        .setValue (hashMap)
                        .addOnSuccessListener {
                            Log.d("LOGIN", "updateUserInfo: Info saved")
                        }
                        .addOnFailureListener {e->
                            Log.e("LOGIN", "updateUserInfo: ", e)
                        }
                }
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Can't login currently. Try after sometime", Toast.LENGTH_SHORT).show()
            }
        }

    }
