package com.nvsp.manta_terminal.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.Fade
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.setMargins
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.integration.android.IntentIntegrator
import com.nvsp.LOGIN_OPERATION
import com.nvsp.MODE_QUEUE
import com.nvsp.TEAM_WORKING
import com.nvsp.WORKPLACE_ID
import com.nvsp.manta_terminal.BaseApp
import com.nvsp.manta_terminal.Const
import com.nvsp.manta_terminal.R
import com.nvsp.manta_terminal.adapters.OperatorAdapter
import com.nvsp.manta_terminal.databinding.DialogFillQrLogOperationBinding
import com.nvsp.manta_terminal.databinding.FragmentMainBinding
import com.nvsp.manta_terminal.viewmodels.MainFragmentViewModel
import com.nvsp.manta_terminal.workplaces.Workplace
import com.nvsp.manta_terminal.workplaces.WorkplaceAdapter
import com.nvsp.nvmesapplibrary.TITAN_CLASS
import com.nvsp.nvmesapplibrary.TITAN_ID
import com.nvsp.nvmesapplibrary.architecture.BaseFragment
import com.nvsp.nvmesapplibrary.communication.socket.MessageListener
import com.nvsp.nvmesapplibrary.communication.socket.WebSocketManager
import com.nvsp.nvmesapplibrary.documentation.DocumentActivity
import com.nvsp.nvmesapplibrary.documentation.OPERATOR_DOC
import com.nvsp.nvmesapplibrary.menu.MenuButtonTerm
import com.nvsp.nvmesapplibrary.menu.MenuDef
import com.nvsp.nvmesapplibrary.queue.OPERATOR_MAIN_LOGGED
import com.nvsp.nvmesapplibrary.queue.OPERATOR_MAIN_NOT_LOGGED
import com.nvsp.nvmesapplibrary.queue.QueueAdapter
import com.nvsp.nvmesapplibrary.queue.models.WORK_QUEUE_ID
import com.nvsp.nvmesapplibrary.queue.work_queue.WorkQueueActivity
import com.nvsp.nvmesapplibrary.utils.CaptureActivity
import com.nvsp.nvmesapplibrary.views.NButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment :
    BaseFragment<FragmentMainBinding, MainFragmentViewModel>(MainFragmentViewModel::class),
    MessageListener {

   private  val barcode=MutableLiveData<String>("")
   private  val wpAdapter: WorkplaceAdapter by lazy {
        WorkplaceAdapter {
            Log.d("SELECTED ITEM:","Select $it")
            selectedWorkplace(it)
        }
    }
    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshData()
            }
        }
    private val resultLauncherCam =registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
            if(intentResult.contents != null) {
                barcode.value = intentResult.contents
            }
        }
    }
   private fun scanByQR(code:(String)->Unit){
        barcode.value=""
        val integrator =  IntentIntegrator(activity).apply {
            captureActivity = CaptureActivity::class.java
            setRequestCode(1)
        }
        resultLauncherCam.launch(integrator.createScanIntent())
        barcode.observe(viewLifecycleOwner) {
            Log.d("observeBarcode", "code: $it")
            if(it.isNotEmpty())
                code(it)
        }
    }
   private  val adapterWQ: QueueAdapter by lazy {
        QueueAdapter(context, OPERATOR_MAIN_NOT_LOGGED,
            { queueData, i ->  //item Click
            }, { _, _ -> //item Long Click
            }, { item ->
                viewModel.logOutOperation(item.getId().toInt(), viewModel.selectedWPId) {
                    refreshData()
                }

            })
    }
    private val adapterOperator: OperatorAdapter by lazy {
        OperatorAdapter(context, OPERATOR_MAIN_NOT_LOGGED, mutableListOf(),
            { item ->  //item Click
            }, { item -> //remove
                viewModel.logoutOperator(item.id)

            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

    }


    companion object;

    override fun onResume() {
        refreshData()
        CoroutineScope(Dispatchers.IO).launch {
            WebSocketManager.connect()
        }
        super.onResume()
    }

    override fun onPause() {
        WebSocketManager.close()
        super.onPause()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        lifecycle.addObserver(this)
    }

    override fun onDetach() {
        super.onDetach()
        lifecycle.removeObserver(this)
    }

    override val bindingInflater: (LayoutInflater) -> FragmentMainBinding
        get() = FragmentMainBinding::inflate

    override fun initViews() {
        initAdapter()
        viewModel.activeSetting.observe(viewLifecycleOwner){


        }

        viewModel.login.observe(viewLifecycleOwner) { usr ->

            if (usr == null) {
                adapterWQ.changeMode(OPERATOR_MAIN_NOT_LOGGED)
                adapterOperator.changeMode(OPERATOR_MAIN_NOT_LOGGED, usr?.idEmployee)
                hideMenu()
            } else {

                adapterWQ.changeMode(OPERATOR_MAIN_LOGGED)
                adapterOperator.changeMode(OPERATOR_MAIN_LOGGED, usr.idEmployee)
                viewModel.loadMenu(viewModel.selectedWPId, usr.role?:(0)){error->
                    infoDialog.showWithMessage(getString(R.string.badConfig)){
                        activity?.finishAffinity()
                    }
                }
            }

            binding.ibAddOperation.isEnabled = usr != null
            binding.ibAddOperator.isEnabled = usr != null

        }
        viewModel.loadDefs(viewModel.login.value, viewModel.selectedWPId)
        binding.tabWP.apply {
            val lm = LinearLayoutManager(context)
            lm.orientation = LinearLayoutManager.HORIZONTAL
            layoutManager = lm
            setHasFixedSize(true)
            adapter = wpAdapter
        }

        viewModel.workplaces.observe(this) {
            Log.d("WORPLACES", "New Worplaces: ${it.size} items ")
            it.forEach {
                Log.d("WORPLACES", " ${it}  ")
            }
            wpAdapter.setNewList(it.toMutableList())
        }
        viewModel.onProgressWQ.observe(viewLifecycleOwner) {
            binding.operRefresher.isRefreshing = it
        }
        viewModel.contentWQ.observe(viewLifecycleOwner) {
            if (it.isNotEmpty())
                Log.d("TAG", "data = ${it[0].data}")
            Log.d("OBSERVER", "data: $it")

            context?.let { _ ->
                adapterWQ.setNewItems(it)
                binding.operationRecycler.invalidateItemDecorations()
            }



        }
        viewModel.operatorsList.observe(viewLifecycleOwner) {
            it.forEach { op ->
                Log.d("OPERATOR", op.toString())
            }
            adapterOperator.setNewItems(it)


        }
        binding.ibAddOperator.setOnClickListener {
            viewModel.loginOperator()
        }
        binding.ibAddOperation.setOnClickListener {
            showAddOperationDialog()

        }
        binding.operRefresher.setOnRefreshListener {
            viewModel.loadContent(user = viewModel.login.value)
        }
        binding.operatorRefresher.setOnRefreshListener {
            viewModel.loadEmployees()
        }
        viewModel.onProgressOP.observe(viewLifecycleOwner) {
            binding.operatorRefresher.isRefreshing = it
        }
        viewModel.menu.observe(viewLifecycleOwner){btnList->
            viewModel.menuDef?.let {btnDef->
                showMenu(btnDef,btnList)
            }
        }
    }

    private fun refreshData() {
        viewModel.loadContent(user = viewModel.login.value)
        viewModel.loadEmployees()
    }

    override fun onActivityCreated() {

        viewModel.loadWorkplaces()
    }

    private fun selectedWorkplace(item: Workplace) {
        Log.e("WORKPACES", "SELECT WORKPLACE $item")
        val url = viewModel.activeSetting.value?.getIpAndPort()
        val devId = BaseApp.remoteSettings?.id
        WebSocketManager.close()
        //val urlAddress = "ws://192.168.1.16:8089/api/Weighing/RequestNotification/4AS9934"
        val urlAddress= if(viewModel.login.value?.role==null)
            "ws://$url/API/Devices/$devId/Status/Workplace/${item.id}?editableListId=$WORK_QUEUE_ID"
        else
            "ws://$url/API/Devices/$devId/Status/Workplace/${item.id}?roleId=${viewModel.login.value?.role}&editableListId=$WORK_QUEUE_ID"///&editableListFilterJson=[]"//[{argumentKey:WorkplaceID,argumentValue:${item.id}}]"

        Log.d("SOCKET INIT", "ip and port:$urlAddress ")

        WebSocketManager.init(urlAddress, this)

        CoroutineScope(Dispatchers.IO).launch {
            WebSocketManager.connect()
        }
        viewModel.login.value?.let {
            viewModel.loadMenu(item.id, it.role?:(0)){error->
                infoDialog.showWithMessage("badConfig"){
                    activity?.finishAffinity()
                }
            }


        }

        binding.apply {
            viewModel.selectedWPId = item.id
            refreshData()
            tvWorkplace.text = item.lname

            if (item.notificationStatus) {
                ivNot.visibility = View.VISIBLE
            } else {
                ivNot.visibility = View.INVISIBLE
            }
            ivTypeWp.setImageResource(
                if (item.teamWorking)
                    R.drawable.ic_team
                else
                    R.drawable.ic_single
            )
            if(item.state.isNotEmpty()){
                tvWorkplaceState.text = item.state
                tvWorkplaceState.visibility = View.VISIBLE
                tvWorkplaceState.backgroundTintList = ColorStateList.valueOf(item.getColorHex())
            }else{
                tvWorkplaceState.text = ""
                tvWorkplaceState.visibility = View.INVISIBLE
            }



          /*  if (item.state != "--") {
                tvWorkplaceState.text = item.state
                if (item.state.isNotEmpty()) {
                    tvWorkplaceState.visibility = View.VISIBLE
                    tvWorkplaceState.backgroundTintList = ColorStateList.valueOf(item.getColorHex())
                } else {
                  //  context?.let {
                        tvWorkplaceState.visibility = View.INVISIBLE
                   // }
                }
            } else
                context?.let {
                    tvWorkplaceState.text = ""
                    tvWorkplaceState.visibility = View.INVISIBLE
                }*/
        }


    }

    private fun initAdapter() {
        binding.operatornRecycler.let {
            val lm = LinearLayoutManager(context)
            it.layoutManager = lm
            it.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL
                )
            )
            it.invalidateItemDecorations()
            it.setHasFixedSize(true)
            it.adapter = adapterOperator
        }


        binding.operationRecycler.let {
            val lm = LinearLayoutManager(context)
            it.layoutManager = lm
            it.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL
                )
            )
            it.invalidateItemDecorations()
            it.setHasFixedSize(true)
            it.adapter = adapterWQ

        }
    }

    fun showMenu(menuDef:MenuDef, buttons:Array<Array<MenuButtonTerm>>){
        binding.menuGrid.visibility=View.VISIBLE
        binding.menuGrid.removeAllViews()
        if(buttons.isNotEmpty()){
            binding.menuGrid.animate().translationX(0f)
        }else{
            binding.menuGrid.animate().translationX(((150+8)*menuDef.colCount).toFloat())
            binding.menuGrid.layoutParams.width = 0
        }
        if(menuDef.hasItems()){
            binding.menuGrid.columnCount=menuDef.colCount+1
            binding.menuGrid.rowCount = menuDef.rowCount+1
            fillMenuGrid(buttons)

        }
        if(viewModel.login.value==null ){
            binding.menuGrid.layoutParams.width = 0
        }
    }
    fun hideMenu(){
        binding.menuGrid.animate().translationX(((150+8)*(viewModel.menuDef?.colCount?:(2))).toFloat())
        binding.menuGrid.visibility=View.GONE
        //binding.menuGrid.layoutParams.width = 0
    }

    override fun onConnectSuccess() {
        Log.i("SOCKET"," Connected successfully \n ")
    }

    override fun onConnectFailed() {
        Log.e("SOCKET"," coneected Failed \n ")
    }

    override fun onClose() {
        Log.i("SOCKET"," Socket Closed \n ")
    }

    override fun onMessage(text: String?) {
        addText( " Receive message: $text \n " )
        //todo zpracovani dat -> idealne VM
            text?.let {


                viewModel.socketDataProcessing(it)
            }

    }

    private fun addText(text: String?) {
        CoroutineScope(Dispatchers.Main).launch {   toogleSocketLed()}

        Log.e("SOCKET", "SOCKET MESSAGE: $text")
    }
    private fun toogleSocketLed(){
        binding.iwSocket.visibility=View.VISIBLE

        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_led)
        val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_led)

        binding.iwSocket.startAnimation(fadeIn)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.iwSocket.startAnimation(fadeOut)
        }, 1000)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.iwSocket.visibility=View.INVISIBLE
        }, 1600)


        }



    private fun fillMenuGrid(dataset:Array<Array<MenuButtonTerm>>){
        dataset.forEachIndexed {row, buttonRow ->
            buttonRow.forEachIndexed { col, menuButton ->
                when(menuButton.specialFunction){
                    "separator"->{
                        addDivider(menuButton)
                    }
                    "SPACE"->{
                        addSpace(row,col)
                    }
                    else->{
                        addButton(menuButton)
                    }
                }
            }
        }
    }

    fun showAddOperationDialog(){
        var id:Int?=null
        val builder = AlertDialog.Builder(context)
            .create()
        val bindingDialog = DialogFillQrLogOperationBinding.inflate(LayoutInflater.from(context))
        builder.setView(bindingDialog.root)

        fun verifyOperation(){
            viewModel.verifyOperationCode(bindingDialog.edBarcode.text.toString()){
                if(it>0){
                    id=it
                    bindingDialog.btnOk.isEnabled=true
                }else {
                    id=null
                    bindingDialog.btnOk.isEnabled=false
                }
            }
        }

        with(bindingDialog){
            btnBack.setOnClickListener {
                builder.dismiss()
            }
            btnChooseFromList.setOnClickListener {
                val intent = Intent(context, WorkQueueActivity::class.java).apply {
                    putExtra(MODE_QUEUE, LOGIN_OPERATION)
                    putExtra(WORKPLACE_ID, viewModel.selectedWPId)
                }
                resultLauncher.launch(intent)
                builder.dismiss()
            }
            bindingDialog.rfidInputLayout.setEndIconOnClickListener {
                Log.d("ONCLICK", "CLICK ON SCANNER")
                scanByQR{
                    Log.d("SCAN", "QR=$it")
                    bindingDialog.edBarcode.setText(it)
                    verifyOperation()
                }
            }
            bindingDialog.edBarcode.setOnEditorActionListener { textView, i, keyEvent ->
                Log.d("barcode done", "i=$i")
                if(i== EditorInfo.IME_ACTION_DONE)
                    verifyOperation()
                false
            }
            btnOk.setOnClickListener {
                id?.let {id->
                    viewModel.loginOperation(id){
                        refreshData()
                    }
                }

                builder.dismiss()
            }
        }
        builder.show()


    }
    private fun addDivider(buttonDef:MenuButtonTerm){
        val view = ImageView(requireContext())
        view.setImageResource(com.nvsp.nvmesapplibrary.R.drawable.separator)
        val layoutParams = GridLayout.LayoutParams()
        layoutParams.width = 0
        layoutParams.height = 10
        layoutParams.columnSpec = GridLayout.spec(0, binding.menuGrid.columnCount, 1f)
        layoutParams.rowSpec = GridLayout.spec(buttonDef.posRow, GridLayout.FILL, 1f)
        view.layoutParams = layoutParams
        binding.menuGrid.addView(view)
    }
    private  fun addSpace(row:Int, col:Int){
        val view =View(requireContext())
        val layoutParams = GridLayout.LayoutParams()
        layoutParams.width = 0
        layoutParams.height = 1
        layoutParams.columnSpec = GridLayout.spec(col, 1, 1f)
        layoutParams.rowSpec = GridLayout.spec(row, GridLayout.FILL, 1f)
        view.layoutParams = layoutParams
        binding.menuGrid.addView(view)
    }
    private fun addButton(buttonDef: MenuButtonTerm){
        val view = NButton(requireContext())
            view.setBorder(null)
        view.setText(buttonDef.label ?: "")
        view.isEnabled = buttonDef.permission>0
        view.setOnClickListener {
            onClick(buttonDef)
        }

        val layoutParams = GridLayout.LayoutParams()
        layoutParams.setMargins(8)
        layoutParams.height = 80
        if (buttonDef.width==1)
            layoutParams.width = 150.toPix()
        else
            layoutParams.width = 308.toPix()

        layoutParams.columnSpec = GridLayout.spec(buttonDef.posCol, buttonDef.width, 1f)
        layoutParams.rowSpec = GridLayout.spec(buttonDef.posRow, GridLayout.FILL, 1f)
        view.layoutParams = layoutParams
        binding.menuGrid.addView(view)
    }

    private fun onClick(button: MenuButtonTerm) {
        Log.d("BTN MENU CLICK", "click on :I${button.objectId}")
        when(button.objectId.toInt()){
            in 1000 ..99999->{//prostoje
                writeIdle(button.objectId)
            }
            Const.TITAN_EVIDENCE->{
                launchEvidenceTitan()
            }
            Const.DOCUMENTATION->{
                showDocumentation()
            }

        }
    }
    fun Int.toPix()=(this*(context?.resources?.displayMetrics?.density?:(1f))+0.5f).toInt()

    private fun writeIdle(id:Int){
        viewModel.writeIdle(id)
    }
private fun showDocumentation(){
    if(adapterWQ.selectedItemId!=null){

        val intent = Intent(activity, DocumentActivity::class.java)
        val b = Bundle()

        val operation = viewModel.contentWQ.value?.find { it.getId()== adapterWQ.selectedItemId}
        b.putInt("MODE", OPERATOR_DOC)
        //todo zpracovat parametry

     //   b.putString("IdProductOrder",operation?.getByColumn("ID_ProductOrder"))
     //   b.putInt("IdOperation", adapterOperation.rowID) //Your id
    //    b.putString("NameOperation", operation?.getByColumn("Name_Operation"))
        intent.putExtras(b) //Put your id to your next Intent
        startActivity(intent)
    }else{
        infoDialog.showWithMessage(getString(R.string.operatrionNotSelect)){

        }
    }



}

    private fun launchEvidenceTitan(){
        val sendIntent = Intent(Intent.ACTION_MAIN)
        sendIntent.setComponent(ComponentName(TITAN_ID, TITAN_CLASS))
        viewModel.login.value?.fillIntent(sendIntent)
        sendIntent.putExtra(WORKPLACE_ID, viewModel.selectedWPId)
        sendIntent.putExtra(TEAM_WORKING, viewModel.workplaces.value?.find { it.id == viewModel.selectedWPId }?.teamWorking)
        /* val sendIntent = requireActivity().packageManager.getLaunchIntentForPackage("com.nvsp.sico")
         viewModel.login.value?.fillIntent(sendIntent)*/
        startActivity(sendIntent)
    }



    private fun launchExternalModule(appId:String, appClass:String){
        val sendIntent = Intent(Intent.ACTION_MAIN)
        sendIntent.setComponent(ComponentName(appId, appClass))
        viewModel.login.value?.fillIntent(sendIntent)
        /* val sendIntent = requireActivity().packageManager.getLaunchIntentForPackage("com.nvsp.sico")
         viewModel.login.value?.fillIntent(sendIntent)*/
        startActivity(sendIntent)
    }

}