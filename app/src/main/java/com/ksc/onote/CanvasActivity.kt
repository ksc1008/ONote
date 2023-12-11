package com.ksc.onote

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationView
import com.ksc.onote.authorization.AuthorizeManager
import com.ksc.onote.calculator.CalculatorViewModel
import com.ksc.onote.canvasViewUI.OnAreaAssignedListener
import com.ksc.onote.canvasViewUI.PenselectFragment
import com.ksc.onote.databinding.ActivityCanvasBinding
import com.ksc.onote.drawingCanvas.CanvasDrawer
import com.ksc.onote.drawingCanvas.CanvasViewModel
import com.ksc.onote.calculator.CalculatorFragment
import com.ksc.onote.drawingCanvas.NoteModel
import com.ksc.onote.formulaViewer.FormulaFragment
import com.ksc.onote.formulaViewer.FormulaViewModel
import com.ksc.onote.graphViewer.GraphFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import kotlin.math.abs


class CanvasActivity : AppCompatActivity() {
    private val TOOLBAR_DEACTIVATE_TRANSPARENCY:Float = 0.1f
    private val TOOLBAR_ACTIVATE_TRANSPARENCY:Float = 0.6f
    val scope = CoroutineScope(Job() + Dispatchers.Main)
    private var currentTool:Toolbar = Toolbar.Pen
    private var saving = false


    enum class Toolbar(var id:Int) {Pen(0),Rectangle(1),Hand(2)}

    private val binding:ActivityCanvasBinding by lazy { ActivityCanvasBinding.inflate(layoutInflater) }
    private val formulaViewModel: FormulaViewModel by viewModels()
    private val serverRequestViewModel: ServerRequestViewModel by viewModels()
    private val calculatorViewModel:CalculatorViewModel by viewModels()
    private val canvasDrawer: CanvasDrawer by viewModels()
    private val canvasViewModel:CanvasViewModel by viewModels()
    private val penSelectFragment: PenselectFragment? by lazy{
        binding.penSelectFragmentContainer.getFragment<PenselectFragment?>()?.apply {
            setOnToolSelectListener(object:
                PenselectFragment.ToolSelectListener {
                override fun invokeHighlighter() {
                    changeTool(Toolbar.Pen)
                    this@CanvasActivity.binding.toolbarPenButton.setImageResource(R.drawable.highlighter2)
                    this@CanvasActivity.binding.canvas.changeHighlighter()
                }

                override fun invokePen() {
                    changeTool(Toolbar.Pen)
                    this@CanvasActivity.binding.toolbarPenButton.setImageResource(R.drawable.pen2)
                    this@CanvasActivity.binding.canvas.changePen()
                }

                override fun invokeEraser() {
                    changeTool(Toolbar.Pen)
                    this@CanvasActivity.binding.toolbarPenButton.setImageResource(R.drawable.eraser2)
                    this@CanvasActivity.binding.canvas.changeErase()
                }

            })

            changeSelectedItem(1)

            setOnPenSettingChangedListener(object:
                PenselectFragment.OnPenSettingChangedListener {
                override fun invokeSliderMove(value: Int) {
                    canvasDrawer.setPenWidth(value.toFloat())
                }

                override fun invokeColorChange(color: Int) {
                    canvasDrawer.setPenColor(color)
                }

            })
        }
    }

    private lateinit var buttons:ArrayList<ImageButton>
    private lateinit var scaleDetector: ScaleGestureDetector
    private var lastPointerX = 0f
    private var lastPointerY = 0f
    private var draggingImage:Boolean = false
    private var canvasX = 0
    private var canvasY = 0

    private fun changeTool(toolbar: Toolbar){
        for(i:Int in 0 until buttons.size){
            if(i == toolbar.id)
                buttons[i].alpha = TOOLBAR_ACTIVATE_TRANSPARENCY
            else
                buttons[i].alpha = TOOLBAR_DEACTIVATE_TRANSPARENCY
        }
        currentTool = toolbar
        binding.canvas.changeTool(toolbar)
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("On Canvas Created:","Canvas Activity Created")

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.canvas.injectViewModel(canvasDrawer, canvasViewModel)
        NetworkManager.getInstance(this)
        buttons = arrayListOf(binding.toolbarPenButton,binding.toolbarRectangleButton, binding.toolbarHandButton)

        binding.canvas.setOnAreaAssignedListener(object :
            OnAreaAssignedListener {
            override fun invoke(area: RectF) {
                if(abs(area.height() * area.width()) > 10)
                    showFormulaFragment()
            }
        })


        for(i:Int in 0 until buttons.size){
            buttons[i].setOnClickListener {
                changeTool(Toolbar.values()[i])
            }
        }

        supportFragmentManager.beginTransaction()
            .add(binding.graphFragmentContainer.id, GraphFragment().apply {
                setOnImageCopiedListener(object:
                    OnImageCopiedListener {
                    @RequiresApi(Build.VERSION_CODES.S)
                    override fun invoke() {
                        val img = getGraphImage()
                        if(img != null) {
                            Log.d("Capture Log", "(${img.width},${img.height})")
                            binding.canvas.addBitmapToCanvas(img)
                            binding.canvas.dispatchTouchEvent(MotionEvent.obtain(0,0, MotionEvent.ACTION_DOWN,
                                lastPointerX-canvasX,lastPointerY-canvasY,0))
                            draggingImage = true

                        }
                        binding.formulaFragmentContainer.getFragment<FormulaFragment?>()?.hide()
                        binding.calculationFragmentContainer.getFragment<CalculatorFragment?>()?.hide()
                    }
                })
            })
            .add(binding.formulaFragmentContainer.id, FormulaFragment().apply {
                setOnImageCopiedListener(object:
                    OnImageCopiedListener {
                    override fun invoke() {
                        val img = getFormulaImage()
                        if(img != null) {
                            Log.d("Capture Log", "(${img.width},${img.height})")
                            binding.canvas.addBitmapToCanvas(img)
                            binding.canvas.dispatchTouchEvent(MotionEvent.obtain(0,0, MotionEvent.ACTION_DOWN,
                                lastPointerX-canvasX,lastPointerY-canvasY,0))
                            draggingImage = true

                        }
                        binding.graphFragmentContainer.getFragment<GraphFragment?>()?.hide()
                        binding.calculationFragmentContainer.getFragment<CalculatorFragment?>()?.hide()
                    }
                })

                setOnButtonClickedListener(object: FormulaFragment.OnBtnClickedListener{
                    override fun invokeButton1() {
                    }

                    override fun invokeButton2() {
                        showGraph()
                    }

                    override fun invokeButton3() {
                        if(formulaViewModel.isCalculated())
                            showCalculation()
                    }

                })
            })
            .add(binding.calculationFragmentContainer.id, CalculatorFragment().apply {
                setOnImageCopiedListener(object:
                    OnImageCopiedListener {
                    override fun invoke() {
                        val img = getFormulaImage()
                        if(img != null) {
                            Log.d("Capture Log", "(${img.width},${img.height})")
                            binding.canvas.addBitmapToCanvas(img)
                            binding.canvas.dispatchTouchEvent(MotionEvent.obtain(0,0, MotionEvent.ACTION_DOWN,
                                lastPointerX-canvasX,lastPointerY-canvasY,0))
                            draggingImage = true
                        }
                        binding.graphFragmentContainer.getFragment<GraphFragment?>()?.hide()
                        binding.formulaFragmentContainer.getFragment<FormulaFragment?>()?.hide()
                    }
                })

                setOnButtonClickedListener(object: CalculatorFragment.OnBtnClickedListener{
                    override fun invoke() {
                    }
                })
            })
            .commit()

        for(i:Int in 0 until buttons.size){
            buttons[i].setOnClickListener {
                changeTool(Toolbar.values()[i])
                if(Toolbar.values()[i]!= Toolbar.Pen){
                    penSelectFragment?.setPentoolActive(false)
                }
                else{
                    penSelectFragment?.setPentoolActive(true)
                }
            }
        }
        binding.toolbarFunctionButton.setOnClickListener{
            popInFormula()
        }

        binding.canvas.viewTreeObserver.addOnGlobalLayoutListener {
            val location = IntArray(2)
            binding.canvas.getLocationOnScreen(location)
            canvasX = location[0]
            canvasY = location[1]
        }

        scaleDetector = ScaleGestureDetector(this, object: SimpleOnScaleGestureListener() {
            override fun onScale(p0: ScaleGestureDetector): Boolean {
                binding.canvas.scaleEvent(p0.scaleFactor)
                return true
            }
        })

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {

            }
        })

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.canvas.setOnTouchListener { view, motionEvent ->
            view.onTouchEvent(motionEvent)
            if(motionEvent.action==MotionEvent.ACTION_DOWN){
                penSelectFragment?.onOtherScreenSelected()
            }
            true
        }

        calculatorViewModel.runAgent(this)


        calculatorViewModel.bridge.setOnResultListener(object: CalculatorViewModel.OnCalculationResult{
            override fun invokeSuccess(result: String) {
                updateCalculation(result)
            }

            override fun invokeFail() {
                Log.d("Calculation Complete:","error")
            }
        })
        AuthorizeManager.getInstance(this)
    }

    override fun onStart() {
        super.onStart()
        if(canvasViewModel.started)
            return
        canvasViewModel.noteName = intent.getStringExtra("Name")?:"UnNamed Note"
        supportActionBar?.title = canvasViewModel.noteName

        when(intent.getStringExtra("Type")){
            null->canvasViewModel.createEmpty(2000,2000)
            "from_uri"->{
                val uri:Uri? = Uri.parse(intent.getStringExtra("uri"))
                if(uri!=null){
                    canvasViewModel.createFromPdfUri(uri,this)
                }
                canvasViewModel.isNew = true
            }
            "from_json"->{
                val json:String? = intent.getStringExtra("data")
                if(json==null){
                    canvasViewModel.createEmpty(2000,2000)
                }
                else{
                    val model = Json.decodeFromString<NoteModel>(json)
                    canvasViewModel.createFromData(model)
                }
                canvasViewModel.isNew = false
            }
            else-> {
                canvasViewModel.createEmpty(2000, 2000)
                canvasViewModel.isNew = true
            }
        }
    }

    fun updateCalculation(result: String){
        scope.launch(Dispatchers.Main) {
            Log.d("Calculation Complete:",result)
            formulaViewModel.setCalculation(result)
        }
    }

    fun showGraph(){
        binding.calculationFragmentContainer.getFragment<CalculatorFragment?>()?.hide(true)
        binding.graphFragmentContainer.getFragment<GraphFragment?>()?.show()
    }

    fun showCalculation(){
        binding.calculationFragmentContainer.getFragment<CalculatorFragment?>()?.show()
        binding.graphFragmentContainer.getFragment<GraphFragment?>()?.hide(true)
    }

    fun showFormulaFragment(){
        val bitmap = binding.canvas.getAreaBitmap()
        if(bitmap != null)
            getFromServer(bitmap)
    }

    private fun popInFormula(){
        if(!formulaViewModel.hasFormula())
            return
        binding.formulaFragmentContainer.getFragment<FormulaFragment?>()?.show()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if(ev!=null)
            scaleDetector.onTouchEvent(ev)
        lastPointerX = ev?.x?:0f
        lastPointerY = ev?.y?:0f
        if(draggingImage){
            var ev2:MotionEvent? = null
            if(ev!=null) {
                ev2 = MotionEvent.obtain(
                    ev.downTime,
                    ev.eventTime,
                    ev.action,
                    ev.x + binding.canvas.x - canvasX,
                    ev.y + binding.canvas.y - canvasY,
                    0
                )
            }
            binding.canvas.dispatchTouchEvent(ev2)
            if(ev?.action == MotionEvent.ACTION_UP) {
                draggingImage = false
            }
        }


        return super.dispatchTouchEvent(ev)
    }

    private fun getFromServer(bitmap: Bitmap){
        scope.launch(Dispatchers.Main) {
            val result = scope.async {
                serverRequestViewModel.getFormulaFromServer(bitmap)
            }.await()
            Log.d("result:",result.toString())

            if(result.second) {
                formulaViewModel.setFormula(result.first)
                calculatorViewModel.calculate(result.first)
                popInFormula()
                changeTool(Toolbar.Hand)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        hideSystemUI()
        super.onWindowFocusChanged(hasFocus)
    }

    private fun hideSystemUI() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            val controller = window.insetsController

            if(controller != null){
                controller.hide(WindowInsets.Type.systemBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_canvas, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_save->{
                save()
                return true
            }

            R.id.action_clear->{
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("모든 페이지를 초기화합니다.")
                builder.setPositiveButton("확인"
                ) { _, _ -> binding.canvas.clearCanvas()

                }
                builder.setNegativeButton("취소"
                ) { _, _ ->  }

                builder.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun save(){
        if(saving)
            return
        var ctime = System.currentTimeMillis()
        saving = true
        scope.launch {
            val data = canvasViewModel.toDataAsync()
            Log.d("Json Parse","To data, elapsed: ${System.currentTimeMillis()-ctime}ms")
            ctime = System.currentTimeMillis()
            val jelem = Json.encodeToJsonElement(data)

            val jobj:kotlinx.serialization.json.JsonObject = jelem.jsonObject
            Log.d("Json Parse","To jsonObject, elapsed: ${System.currentTimeMillis()-ctime}ms")
            Log.d("Json Parse","size: ${jobj.toString().length/1024}KB")
            Log.d("Json Parse","data: ${jobj.toString()}KB")

            NetworkManager.getInstance()?.postUpdateNote(canvasViewModel.noteName, JSONObject(jobj.toString()),object:NetworkGetListener<Boolean?>{
                override fun getResult(`object`: Boolean?) {
                    if(`object` == null || `object` == false)
                        Log.e("Upload Note","Failed to upload note.")
                    else {
                        Toast.makeText(baseContext, "Saved!", Toast.LENGTH_SHORT).show()
                        Log.d("Upload Note", "Succeed!")
                        NoteListModel.getInstance()?.renewNoteList()
                        canvasViewModel.isNew = false
                    }
                    saving = false
                }

                override fun getError(message: String) {
                    saving = false
                }

            },canvasViewModel.isNew)
        }
    }

}
