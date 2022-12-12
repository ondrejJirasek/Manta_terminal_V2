package com.nvsp.manta_terminal.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.setMargins
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.integration.android.IntentIntegrator
import com.nvsp.*
import com.nvsp.nvmesapplibrary.DOCUMENTATION
import com.nvsp.nvmesapplibrary.EVIDENCE_STANDARD
import com.nvsp.nvmesapplibrary.TITAN_EVIDENCE
import com.nvsp.manta_terminal.BaseApp
import com.nvsp.manta_terminal.Const
import com.nvsp.manta_terminal.R
import com.nvsp.manta_terminal.adapters.OperatorAdapter
import com.nvsp.manta_terminal.databinding.DialogFillQrLogOperationBinding
import com.nvsp.manta_terminal.databinding.FragmentMainBinding
import com.nvsp.manta_terminal.viewmodels.MainFragmentViewModel
import com.nvsp.manta_terminal.workplaces.Workplace
import com.nvsp.manta_terminal.workplaces.WorkplaceAdapter
import com.nvsp.nvmesapplibrary.*
import com.nvsp.nvmesapplibrary.architecture.BaseFragment
import com.nvsp.nvmesapplibrary.architecture.InfoDialog
import com.nvsp.nvmesapplibrary.communication.socket.MessageListener
import com.nvsp.nvmesapplibrary.communication.socket.WebSocketClientService
import com.nvsp.nvmesapplibrary.details.DetailGeneric
import com.nvsp.nvmesapplibrary.documentation.DocumentActivity
import com.nvsp.nvmesapplibrary.documentation.ID_OPERATION
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
import org.json.JSONObject

class MainFragment :
    BaseFragment<FragmentMainBinding, MainFragmentViewModel>(MainFragmentViewModel::class),
    MessageListener {


   private  val barcode=MutableLiveData("")
   private  val wpAdapter: WorkplaceAdapter by lazy {
        WorkplaceAdapter {
            Log.d("SELECTED ITEM:","Select $it")
            selectedWorkplace(it)
        }
    }
   private var resultLauncher =
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
            { _, _ ->  //item Click
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
                viewModel.getOperatorsOperation(item.id)
            }, { item -> //remove
                viewModel.logoutOperator(item.id)

            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("EVENT", "MainOnCreate")

        super.onCreate(savedInstanceState)

    }


    companion object;

    override fun onResume() {
        Log.e("EVENT", "MainOnResume")
        refreshData()
viewModel.activeSetting.observe(viewLifecycleOwner){set->
   // val url = viewModel.activeSetting.value?.ipAddress
    set?.let {
        if(it.autorefresh==1)
            it.socketPort?.let { socketPort->
                viewModel.connectToSocket(it.ipAddress, socketPort)
            }?:kotlin.run {
                infoDialog.showWithMessage(getString(R.string.badport),com.nvsp.nvmesapplibrary.constants.Const.LEVEL_ERROR){}
            } }

        //viewModel.webSocketInit(it.ipAddress)

    }



        /*CoroutineScope(Dispatchers.IO).launch {
            WebSocketManager.connect()
        }*/
        super.onResume()
    }

    override fun onPause() {
        Log.e("EVENT", "MainOnPause")
       // WebSocketManager.close()
       viewModel.webSocketClose()
        super.onPause()
    }

    override fun onAttach(context: Context) {
        Log.e("EVENT", "MainOnAttach")
        super.onAttach(context)
        lifecycle.addObserver(this)
    }

    override fun onDetach() {
        Log.e("EVENT", "MainOnDetach")
        super.onDetach()
        lifecycle.removeObserver(this)
    }

    override val bindingInflater: (LayoutInflater) -> FragmentMainBinding
        get() = FragmentMainBinding::inflate

    override fun initViews() {
        Log.e("EVENT", "MainInitViews")
        initAdapter()
        viewModel.activeSetting.observe(viewLifecycleOwner){


        }
        viewModel.onProcessSocket.observe(viewLifecycleOwner){
            Log.d("SOCKET", "socket process:$it")
            if(it)
            toogleSocketLed()
        }
        binding.ibRefresh.setOnClickListener {
            refreshData()
        }
        viewModel.login.observe(viewLifecycleOwner) { usr ->

            if (usr == null) {
                adapterWQ.changeMode(OPERATOR_MAIN_NOT_LOGGED)
                adapterOperator.changeMode(OPERATOR_MAIN_NOT_LOGGED, null)
                hideMenu()
            } else {

                adapterWQ.changeMode(OPERATOR_MAIN_LOGGED)
                adapterOperator.changeMode(OPERATOR_MAIN_LOGGED, usr.idEmployee)
                viewModel.loadMenu(viewModel.selectedWPId, usr.role?:(0)){
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
            Log.d("WORPLACES", "New Worplaces: ${it.size} items  selected: ${wpAdapter.selectedItem}")
            if(it.isNotEmpty())
                if(wpAdapter.selectedItem==null)
                    wpAdapter.selectedItem= it[0]
            it.forEach {wp->
                Log.d("WORPLACES", " $wp  ")
                if(wpAdapter.selectedItem ==wp) selectedWorkplace(wp)
            }
            wpAdapter.setNewList(it.toMutableList())
        }
        viewModel.onProgressWQ.observe(viewLifecycleOwner) {
            binding.operRefresher.isRefreshing = it
        }
        viewModel.markedOperations.observe(viewLifecycleOwner){
            adapterWQ.markOperation(it)

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
            Log.e("SELECT OPERATIONLOGIN", "selectedOper ${viewModel.getSelectedWP()}\n par:${viewModel.getSelectedWP()?.typeLoginOp}")
            when(viewModel.getSelectedWP()?.typeLoginOp){

                Const.LOGIN_OPERATION_BY_QR-> { showAddOperationDialog()}
                Const.LOGIN_OPERATION_BY_LIST->{chooseFromList()}
                else->{showAddOperationDialog()}
            }
           // showAddOperationDialog()

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
            }?: kotlin.run { hideMenu() }
        }
    }

    private fun refreshData() {
        viewModel.loadContent(user = viewModel.login.value)
        viewModel.loadEmployees()
        viewModel.workplaceStatus(){
            wpAdapter.setNewItem(it)
            setInfoWp(it)
        }

    }

    override fun onActivityCreated() {
        setBackButton(false)
        viewModel.loadWorkplaces()

    }

    private fun selectedWorkplace(item: Workplace) {
        Log.e("WORKPACES", "SELECT WORKPLACE $item")
       // val url = viewModel.activeSetting.value?.getIpAndPort()
    //    val devId = BaseApp.remoteSettings?.id
        //viewModel.selectedWPId = item.id
        adapterWQ.markOperation(emptyList())
        adapterOperator.selectedItem=null
      //  WebSocketManager.close()
        //val urlAddress = "ws://192.168.1.16:8089/api/Weighing/RequestNotification/4AS9934"
      //  val urlAddress= if(viewModel.login.value?.role==null)
      //      "ws://$url/API/Devices/$devId/Status/Workplace/${item.id}?editableListId=$WORK_QUEUE_ID"
      //  else
      //      "ws://$url/API/Devices/$devId/Status/Workplace/${item.id}?roleId=${viewModel.login.value?.role}&editableListId=$WORK_QUEUE_ID"///&editableListFilterJson=[]"//[{argumentKey:WorkplaceID,argumentValue:${item.id}}]"

       // Log.d("SOCKET INIT", "ip and port:$urlAddress ")
        /*   WebSocketManager.close()
         if(WebSocketManager.isConnect())
              WebSocketManager.changeURL(urlAddress)
          else {*/
        /*    WebSocketManager.init(urlAddress, this)

            CoroutineScope(Dispatchers.IO).launch {
                WebSocketManager.connect(urlAddress,false)
            }
     //   }*/
     //   if(item.id!=0)
        viewModel.changeWP(item.id)
        setInfoWp(item)
        if(viewModel.selectedWPId !=item.id) {
            viewModel.selectedWPId = item.id
            viewModel.login.value?.let {
                viewModel.loadMenu(item.id, it.role ?: (0)) {
                    infoDialog.showWithMessage(getString(R.string.badConfig)) {
                        hideMenu()
                       // activity?.finishAffinity()
                    }
                }
            }
            refreshData()
        }




    }

    private fun setInfoWp(item:Workplace){
        binding.apply {
            Log.d("SELECTEDWP", "SELECT WP: $item")
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
                tvWorkplaceState.backgroundTintList = ColorStateList.valueOf(item.getColorHex()?:(com.nvsp.nvmesapplibrary.R.color.zxing_transparent))
            }else{
                tvWorkplaceState.text = ""
                tvWorkplaceState.visibility = View.INVISIBLE
            }

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
       // Log.i("SOCKET"," Connected successfully \n ")
    }

    override fun onConnectFailed() {
    //    Log.e("SOCKET"," coneected Failed \n ")
    }

    override fun onClose() {
      //  Log.i("SOCKET"," Socket Closed \n ")
    }

    override fun onMessage(text: String?) {

        //todo zpracovani dat -> idealne VM
            text?.let {
                val json = JSONObject(text)
                val wp = json.getInt("workplaceId")
                if(wp ==viewModel.selectedWPId) {
                    addText( " Receive message: $text \n " )
                    viewModel.socketDataProcessing(json)
                }
            }

    }

    private fun addText(text: String?) {
        CoroutineScope(Dispatchers.Main).launch {   toogleSocketLed()}

      //  Log.e("SOCKET", "SOCKET MESSAGE: $text")
    }
    private fun toogleSocketLed(){
        try {
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
            viewModel.onProcessSocket.value=false
        }catch (e:Exception){
            Log.e("stateSocket", "$e")
        }



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

    private fun chooseFromList(){

        val intent = Intent(context, WorkQueueActivity::class.java).apply {
            putExtra(MODE_QUEUE, LOGIN_OPERATION)
            putExtra(WORKPLACE_ID, viewModel.selectedWPId)
        }
        resultLauncher.launch(intent)

    }
   private fun showAddOperationDialog(){
       // var id:Int?=null
        val builder = AlertDialog.Builder(context)
            .create()
        val bindingDialog = DialogFillQrLogOperationBinding.inflate(LayoutInflater.from(context))
        builder.setView(bindingDialog.root)

        fun verifyOperation(){
            viewModel.verifyAndLoginOperation(bindingDialog.edBarcode.text.toString()){
                builder.dismiss()
                refreshData()
               /* if(it>0){
                    id=it

                   // bindingDialog.btnOk.isEnabled=true
                }else {
                    id=null
                    bindingDialog.btnOk.isEnabled=false
                }*/
            }
        }

        with(bindingDialog){
            btnBack.setOnClickListener {
                builder.dismiss()
            }
            btnChooseFromList.setOnClickListener {

                chooseFromList()
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
             /*   id?.let {id->
                    viewModel.loginOperation(id){
                        refreshData()
                    }
                }*/
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
        when(button.objectId){
            in 1000 ..99999->{//prostoje
                writeIdle(button.objectId)
            }
            TITAN_EVIDENCE->{
                launchEvidenceTitan()
            }
            DOCUMENTATION->{
                showDocumentation()
            }
            INFO->{
                showInfo()
            }
           EVIDENCE_STANDARD->{
                val action = MainFragmentDirections.actionMainFragmentToEvidence(
                    workplaceID = viewModel.selectedWPId,
                    teamWorking = viewModel.workplaces.value?.find { it.id == viewModel.selectedWPId }?.teamWorking?:(false),
                    mode = Const.MODE_EVIDENCE_HEI
                )
                findNavController().navigate(action)
            }

        }
    }
    fun Int.toPix()=(this*(context?.resources?.displayMetrics?.density?:(1f))+0.5f).toInt()

    private fun writeIdle(id:Int){
        viewModel.writeIdle(id)
    }
    private fun showInfo(){

            adapterWQ.selectedItemId?.let{
                val intent = Intent(activity, DetailGeneric::class.java)
                val b = Bundle()
                b.putInt("IdOperation", it.toInt()) //Your id
                intent.putExtras(b) //Put your id to your next Intent
                startActivity(intent)
            }?: kotlin.run {
                infoDialog.showWithMessage(getString(R.string.operatrionNotSelect)){

                }
            }
    }
private fun showDocumentation(){
    if(adapterWQ.selectedItemId!=null){

        val intent = Intent(activity, DocumentActivity::class.java)
        val b = Bundle()

        val operation = viewModel.contentWQ.value?.find { it.getId()== adapterWQ.selectedItemId}
        b.putInt("MODE", OPERATOR_DOC)
        b.putInt(ID_OPERATION, (operation?.getId()?:(-1)).toInt())

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
       // if(isPackageInstalled(TITAN_ID, requireContext().packageManager)) {
        try {
            val sendIntent = Intent(Intent.ACTION_MAIN)
            sendIntent.component = ComponentName(TITAN_ID, TITAN_CLASS)
            viewModel.login.value?.fillIntent(sendIntent)
            sendIntent.putExtra(WORKPLACE_ID, viewModel.selectedWPId)
            sendIntent.putExtra(
                TEAM_WORKING,
                viewModel.workplaces.value?.find { it.id == viewModel.selectedWPId }?.teamWorking
            )
            sendIntent.putExtra(SETTINGS, viewModel.activeSetting.value?.toJson())
            /* val sendIntent = requireActivity().packageManager.getLaunchIntentForPackage("com.nvsp.sico")
         viewModel.login.value?.fillIntent(sendIntent)*/
            startActivity(sendIntent)
            /*  }else{

        }*/
        }catch (e:Exception){
            Log.e("startAPK", "chyba:$e")
            infoDialog.showWithMessage(getString(R.string.apkNotInstalled, TITAN_ID)){}
        }

    }



   /* private fun launchExternalModule(appId:String, appClass:String){

        val sendIntent = Intent(Intent.ACTION_MAIN)
        sendIntent.setComponent(ComponentName(appId, appClass))
        viewModel.login.value?.fillIntent(sendIntent)
        /* val sendIntent = requireActivity().packageManager.getLaunchIntentForPackage("com.nvsp.sico")
         viewModel.login.value?.fillIntent(sendIntent)*/
        startActivity(sendIntent)
    }*/
 private fun isPackageInstalled (packageName:String, packageManager: PackageManager):Boolean{
     return try {
         packageManager.getPackageInfo(packageName,PackageManager.GET_ACTIVITIES)
         true
     }catch (e:PackageManager.NameNotFoundException){
         Log.e("startAPK", "chyba:$e")
         false
     }
 }


}