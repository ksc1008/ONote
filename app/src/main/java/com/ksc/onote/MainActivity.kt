package com.ksc.onote

import android.app.Instrumentation.ActivityResult
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.ksc.onote.databinding.ActivityMainBinding
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var uri:Uri? = null
    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if( it.resultCode == RESULT_OK) {
            uri = it.data?.data
            Log.d(TAG,uri.toString())
            startCanvas()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            showFileChooser()
        }
    }

    private fun startCanvas(){
        val switchActivityIntent = Intent(this, CanvasActivity::class.java)

        if(uri!= null){
            switchActivityIntent.putExtra("Type","from_uri")
            switchActivityIntent.putExtra("uri",uri.toString())
        }
        startActivity(switchActivityIntent)
    }
    private fun showFileChooser(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)

        intent.type = "application/pdf"
        try {
            launcher.launch(intent)
        } catch (ex:Exception) {
            ex.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}