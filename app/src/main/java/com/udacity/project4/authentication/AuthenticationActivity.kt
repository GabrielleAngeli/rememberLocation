package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private val SIGN_IN_REQUEST_CODE = 253

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        //Firebase Authentication
        if(FirebaseAuth.getInstance().currentUser!=null){
            startActivity(Intent(this, RemindersActivity::class.java))
            finish()
        }else{
            launchSignInFlow()
        }
    }
    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers)
                .build(), SIGN_IN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                Log.d(
                    "TAG",
                    "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!" +
                            " ${FirebaseAuth.getInstance().currentUser?.email}"
                )
                if(FirebaseAuth.getInstance().currentUser!=null){
                    startActivity(Intent(this, RemindersActivity::class.java))
                    finish()
                }
            } else {
                Log.d("TAG", "Sign in unsuccessful ${response?.error?.errorCode}")
                Toast.makeText(
                    this,
                    "Authentication failed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
