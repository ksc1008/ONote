package com.ksc.onote

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ksc.onote.authorization.AuthorizeManager
import com.ksc.onote.databinding.ActivityLoginBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class LoginActivity : ComponentActivity() {
    lateinit var binding:ActivityLoginBinding


    private val googleSignInClient: GoogleSignInClient by lazy { getGoogleClient() }
    private val googleAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG,result.toString())
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        if(!task.isSuccessful){
            Toast.makeText(this,"Failed login",Toast.LENGTH_SHORT)
            return@registerForActivityResult
        }

        try {
            val account = task.getResult(ApiException::class.java)

            // 이름, 이메일 등이 필요하다면 아래와 같이 account를 통해 각 메소드를 불러올 수 있다.
            val userName = account.givenName
            val serverAuth = account.serverAuthCode
            account.idToken
            Log.d(TAG,"Username : ${account.givenName}")
            Log.d(TAG,"Auth Code : ${account.serverAuthCode}")


            getAccessToken(account.serverAuthCode.toString())
            //moveSignUpActivity()

        } catch (e: ApiException) {
            Log.e(TAG, e.stackTraceToString())
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
    }

    private fun getGoogleClient(): GoogleSignInClient {
        val googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestProfile()
            .requestServerAuthCode(getString(R.string.google_login_client_id))
            .build()

        return GoogleSignIn.getClient(this, googleSignInOption)
    }

    private fun requestGoogleLogin() {
        googleSignInClient.signOut()
        val signInIntent = googleSignInClient.signInIntent
        googleAuthLauncher.launch(signInIntent)
    }

    private fun getAccessToken(autoToken:String):String{
        GlobalScope.launch {
            NetworkManager.getInstance(baseContext)?.getAccessToken(autoToken, listener = object:NetworkGetListener<Boolean>{
                override fun getResult(`object`: Boolean) {
                    Log.d(TAG,"Result : $`object`")
                    if(`object`){
                        val switchActivityIntent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(switchActivityIntent)
                        finish()
                    }
                }

                override fun getError(message: String) {
                }

            })
        }
        return ""
    }
}