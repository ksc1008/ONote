package com.ksc.onote

import android.content.ContentValues.TAG
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.ksc.onote.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if( it.resultCode == RESULT_OK) {
            val uri = it.data?.data
            Log.d(TAG,uri.toString())
            startCanvas(uri)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        NetworkManager.getInstance(this)

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

    fun startCanvas(uri:Uri?){
        val switchActivityIntent = Intent(this, CanvasActivity::class.java)

        if(uri!= null){
            switchActivityIntent.putExtra("Name","note_pdf")
            switchActivityIntent.putExtra("Type","from_uri")
            switchActivityIntent.putExtra("uri",uri.toString())
        }
        var m_Text:String = ""
        var result:Boolean = false

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("생성할 노트 이름을 정해주세요.")
        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
        builder.setView(input)
        builder.setPositiveButton("OK"
        ) { dialog, which ->
            result = true
            m_Text = input.text.toString()
            switchActivityIntent.putExtra("Name", m_Text)
            startActivity(switchActivityIntent)
        }
        builder.setNegativeButton("Cancel",
            { dialog, which -> result=false; dialog.cancel() })

        builder.show()

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