package com.ksc.onote

import android.app.Instrumentation.ActivityResult
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.ksc.onote.databinding.ActivityMainBinding
import org.json.JSONArray
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if( it.resultCode == RESULT_OK) {
            val uri = it.data?.data
            Log.d(TAG,uri.toString())
            startCanvas(uri)
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
            val popupMenu:PopupMenu = PopupMenu(applicationContext,it)
            menuInflater.inflate(R.menu.create_note_menu,popupMenu.menu)
            popupMenu.setOnMenuItemClickListener (object:PopupMenu.OnMenuItemClickListener{
                override fun onMenuItemClick(p0: MenuItem?): Boolean {
                    when(p0?.itemId){
                        R.id.action_new_node_menu->{
                            startCanvas(null)
                            return true
                        }

                        R.id.action_from_pdf_menu->{
                            showFileChooser()
                            return true
                        }
                    }
                    return false
                }

            })
            popupMenu.show()
        }
    }

    private fun startCanvas(uri:Uri?){
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

    private fun createEmpty(){

    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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