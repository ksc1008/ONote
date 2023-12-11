package com.ksc.onote

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ksc.onote.authorization.AuthorizeManager
import com.ksc.onote.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch


class LoginActivity : ComponentActivity() {
    lateinit var binding:ActivityLoginBinding


    private val googleSignInClient: GoogleSignInClient by lazy { getGoogleClient() }
    private val googleAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG,result.toString())
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        if(!task.isSuccessful){
            Toast.makeText(this,"Failed login",Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }

        try {
            val account = task.getResult(ApiException::class.java)

            account.idToken
            Log.d(TAG,"Auth Code : ${account.serverAuthCode}")


            getAccessToken(account.serverAuthCode.toString())
            //moveSignUpActivity()

        } catch (e: ApiException) {
            Log.e(TAG, e.stackTraceToString())
            Toast.makeText(this,"Failed login",Toast.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            requestGoogleLogin()
        }

        AuthorizeManager.getInstance(this)

        if(!trySilentLogin()){
            letReLogin()
        }
    }

    private fun letReLogin(){
        binding.loginButton.visibility = View.VISIBLE
    }

    private fun hideButton(){
        binding.loginButton.visibility = View.INVISIBLE
    }

    private fun getGoogleClient(): GoogleSignInClient {
        val googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestProfile()
            .requestServerAuthCode(getString(R.string.google_login_client_id))
            .build()

        return GoogleSignIn.getClient(this, googleSignInOption)
    }

    private fun requestGoogleLogin() {
        val signInIntent = googleSignInClient.signInIntent
        googleAuthLauncher.launch(signInIntent)
    }

    private fun getAccessToken(autoToken:String):String{
        lifecycleScope.launch {
            NetworkManager.getInstance(baseContext)?.getRequestLogin(autoToken, listener = object:NetworkGetListener<Boolean>{
                override fun getResult(`object`: Boolean) {
                    Log.d(TAG,"Result : $`object`")
                    if(`object`){
                        val switchActivityIntent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(switchActivityIntent)
                        finish()
                    }
                }

                override fun getError(message: String) {
                    Toast.makeText(this@LoginActivity,"Failed login",Toast.LENGTH_LONG).show()
                    letReLogin()
                }

            })
        }
        return ""
    }

    private fun trySilentLogin():Boolean{
        val googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestProfile()
            .requestServerAuthCode(getString(R.string.google_login_client_id))
            .build()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        for(scope in googleSignInOption.scopes){
            if(!GoogleSignIn.hasPermissions(account,scope)){
                return false
            }
        }
        googleSignInClient.silentSignIn().addOnCompleteListener {
            if(!it.isSuccessful){
                Toast.makeText(this,"Failed login",Toast.LENGTH_LONG).show()
                binding.loginButton.visibility = View.VISIBLE
                return@addOnCompleteListener
            }
            try {
                val result = it.getResult(ApiException::class.java)

                Log.d(TAG,"Auth Code : ${result.serverAuthCode}")


                getAccessToken(result.serverAuthCode.toString())
                return@addOnCompleteListener

            } catch (e: ApiException) {
                Log.e(TAG, e.stackTraceToString())
                Toast.makeText(this,"Failed login",Toast.LENGTH_LONG).show()
                binding.loginButton.visibility = View.VISIBLE
            }
        }
        return true
    }
}