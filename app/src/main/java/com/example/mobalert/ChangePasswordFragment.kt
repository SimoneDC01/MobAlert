package com.example.mobalert

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.mobalert.databinding.FragmentChangePasswordBinding
import com.example.mobalert.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ChangePasswordFragment : Fragment() {
    private lateinit var binding: FragmentChangePasswordBinding
    private val auth = Firebase.auth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);


        binding.submitBtn.setOnClickListener{
            val currentPassword = binding. passwordEt.text.toString()
            val newPassword = binding. newPasswordEt.text. toString()
            val confirmNewPassword = binding. confirmPasswordEt.text. toString()

            //validate data
            if (currentPassword. isEmpty()){
            //Current Password Field (currentPasswordEt) is empty, show error
                binding.passwordEt.error = "Enter current password"
                binding. passwordEt.requestFocus()
            } else if (newPassword. isEmpty() ) {
                binding.newPasswordEt.error = "Enter new password"
                binding.newPasswordEt.requestFocus()
            }
            else if (confirmNewPassword. isEmpty() ){
                binding.confirmPasswordEt.error = "Enter confirm new password"
                binding.confirmPasswordEt.requestFocus()
            } else if (newPassword != confirmNewPassword) {
                binding.confirmPasswordEt.error = "Password doesn't match"
                binding. confirmPasswordEt.requestFocus()
            } else {
                //before changing password re-authenticate the user to check if the user has entered correct c
                val authCredential =
                    EmailAuthProvider.getCredential(auth.currentUser!!.email!!, currentPassword)
                auth.currentUser!!.reauthenticate(authCredential)
                    .addOnSuccessListener {
                        Log.d("LOGIN", "authenticateUserForUpdatePassword: Authentication success")

                        //begin update password, pass the new password as parameter
                        auth.currentUser!!.updatePassword(newPassword)
                            .addOnSuccessListener {
                                Log.d("LOGIN", "updatePassword: Password updated ... ")
                                Toast.makeText(this.activity, "Password updated", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.beginTransaction().replace(R.id.Fragment, ProfileFragment()).commit()
                            }
                            .addOnFailureListener { e ->
                                Log.e("LOGIN", "updatePassword: ", e)
                                Toast.makeText(this.activity, "Password update failed", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("LOGIN", "authenticateUserForUpdatePassword: ", e)
                        Toast.makeText(this.activity, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }
}