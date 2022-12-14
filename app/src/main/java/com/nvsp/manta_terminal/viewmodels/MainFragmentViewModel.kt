package com.nvsp.manta_terminal.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nvsp.manta_terminal.BaseApp
import com.nvsp.manta_terminal.models.Operator
import com.nvsp.manta_terminal.workplaces.Workplace
import com.nvsp.nvmesapplibrary.architecture.CommunicationViewModel
import com.nvsp.nvmesapplibrary.communication.socket.WebSocketClient2
import com.nvsp.nvmesapplibrary.communication.socket.WebSocketClientService

import com.nvsp.nvmesapplibrary.communication.volley.ServiceVolley
import com.nvsp.nvmesapplibrary.database.LibRepository
import com.nvsp.nvmesapplibrary.login.models.User
import com.nvsp.nvmesapplibrary.menu.MenuButtonTerm
import com.nvsp.nvmesapplibrary.menu.MenuDef
import com.nvsp.nvmesapplibrary.queue.models.*
import com.nvsp.nvmesapplibrary.rpc.OutData
import com.nvsp.nvmesapplibrary.rpc.Rpc
import com.nvsp.nvmesapplibrary.rpc.RpcParam
import org.json.JSONObject


class MainFragmentViewModel (private val repository: LibRepository, private val api: ServiceVolley):
    CommunicationViewModel(repository,api){
    var selectedWPId:Int = 0
    val workplaces = MutableLiveData<List<Workplace>>()
    val menu= MutableLiveData<Array<Array<MenuButtonTerm>>>()
    var menuDef : MenuDef?=null
    val operatorsList = MutableLiveData<List<Operator>>()
    val markedOperations = MutableLiveData<List<Int>>()
    val onProgressWQ = MutableLiveData<Boolean>(false)
    val onProcessSocket = MutableLiveData<Boolean>(false)
    val onProgressOP = MutableLiveData<Boolean>(false)
    var gridWQ: EditableModuleLayoutDefinition = EditableModuleLayoutDefinition(0, 0)
    var definitonsWQ: List<ViewDefinition> = emptyList()
    val contentWQ = MutableLiveData<List<ViewData>>(mutableListOf())


    private val webSocket2: WebSocketClient2 by lazy { WebSocketClient2(){ retMessage->
        Log.d("MESSAGE", "RECEIVED ON MAIN: $retMessage")
        onProcessSocket.postValue(true)
        parseData(retMessage)

    } }

    fun webSocketClose(){
        webSocket2.close()

    }
    fun webSocketInit(url:String,port:Int) {
      //  webSocket.initWebSocket(getURLForSocket(url,BaseApp.remoteSettings?.id, selectedWPId))

        webSocket2.init(getURLForSocket(url,BaseApp.remoteSettings?.id, selectedWPId, port))
        webSocket2.connect()
    }
    fun connectToSocket(url:String,port:Int){

        webSocket2.connect(getURLForSocket(url,BaseApp.remoteSettings?.id, selectedWPId,port))

    }
    fun changeWP(wpId:Int){
        webSocket2.setWP(wpId)
    }

   private fun getURLForSocket(url:String,devId:Long?,wp:Int ,port:Int):String{

        return    if(login.value?.role==null)
        "ws://$url:$port/API/Devices/$devId/Status/Workplace/${wp}?editableListId=$WORK_QUEUE_ID&editableListFilterJson=[{argumentKey:WorkplaceID,argumentValue:${wp}}]"
    else
        "ws://$url:$port/API/Devices/$devId/Status/Workplace/${wp}?roleId=${login.value?.role}&editableListId=$WORK_QUEUE_ID&editableListFilterJson=[{argumentKey:WorkplaceID,argumentValue:${wp}}]"
    }
    fun loadWorkplaces(){
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
            Request.Method.GET,
            Workplace.getUrl(BaseApp.remoteSettings?.id?:(-1)),""
        )
        ){ code, response ->
            val gson = Gson()
            val itemType = object : TypeToken<List<Workplace>>(){}.type
            val list = gson.fromJson<List<Workplace>>(response.array.toString(),itemType)
            Log.d("LOADER", "item size:${list.size}")
            workplaces.value=(list)
        }
    }
    fun workplaceStatus(wp:(Workplace)->Unit){
        if(selectedWPId>0)
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.GET,
                Workplace.getUrlForWP(BaseApp.remoteSettings?.id?:(-1), selectedWPId),""
            )
        ){ code, response ->
            val gson = Gson()
            if((response.array?.length() ?: (0)) > 0){
                val item = gson.fromJson(response.getSingleObject().toString(), Workplace::class.java)
                wp(item)
            }
            }

    }
    fun getSelectedWP():Workplace?{
       return  workplaces.value?.find { it.id==selectedWPId }
    }
    fun loadDefs(user: User?=null, idWP:Int) {
        onProgressWQ.value=true
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
               Request.Method.GET,
                ViewDefinition.getURL(WORK_QUEUE_ID),
                "",
                user,
                null
            ),
            hideProgressOnEnd =  false,
            showProgress = false
        ) { code, response ->
            if(code==200)
            response.getSingleObject()?.let {
                gridWQ = ViewDefinition.getGrid(it)
                definitonsWQ = ViewDefinition.createList(it)
               // Log.d("WQ_VIEWMODEL", "grid: $grid")
               // Log.d("WQ_VIEWMODEL", "data: $definitons")
                loadContent(user = user)
            }
        }
    }
  //  fun  isAll(act:Int?)=dataMaxSize.value==act

    fun loadContent(all:Boolean=false, user: User?=null){
        val jsonFilter = mutableListOf<SystemFilters>(
            SystemFilters("WorkplaceID", selectedWPId.toString())

        )
        Log.d("VIEWMODEL MAIN", "jsonFilter: $jsonFilter")
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                com.android.volley.Request.Method.POST,
                ViewData.getURL(WORK_QUEUE_ID),
                "",
                user,
                ViewData.generateParam(0, user?.id, SystemFilters.getJsonArray(jsonFilter))
            ), showProgress = false
        ) { _, response ->
            onProgressWQ.value=false
            response.getSingleObject()?.let {
                //dataMaxSize.value = QueueData.getDataSize(it)
               // isNextAvalaible= QueueData.getNextAvalaible(it)
                contentWQ.value = ViewData.createList(definitonsWQ,it, gridWQ)

            }
        }
    }
    fun socketDataProcessing(json:JSONObject){

        val editableList = json.getJSONObject("editableListData")
        val workplacesJson = json.getJSONArray("deviceWorkplaces")
        val employees = json.getJSONArray("employeesOnWp")
        val buttons = json.getJSONArray("buttonsOnWp")
        val gson = Gson()

         //   onProcessSocket.value=false
            //fillemployee
            val itemTypeOp = object : TypeToken<List<Operator>>() {}.type
            operatorsList.postValue(gson.fromJson<List<Operator>>(employees.toString(), itemTypeOp))

            //workplaces
            val itemTypeWp = object : TypeToken<List<Workplace>>() {}.type
            workplaces.postValue(
                gson.fromJson<List<Workplace>>(
                    workplacesJson.toString(),
                    itemTypeWp
                )
            )

            //buttons
            menuDef?.let { def ->
                menu.postValue(MenuButtonTerm.createList(buttons, def))
            }
            //editableList
            contentWQ.postValue(ViewData.createList(definitonsWQ, editableList, gridWQ))

    }

    fun loadEmployees(){
        onProgressOP.value=true
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.GET,
                Operator.getUrl(selectedWPId),
                "",
                null,
                null
            )
        ) { code, response ->
            onProgressOP.value=false
            val gson = Gson()
            val itemType = object : TypeToken<List<Operator>>(){}.type
            operatorsList.value = gson.fromJson<List<Operator>>(response.array.toString(),itemType)
        }


    }
    fun loginOperator(){
        OutData.logInOutOperatorParams(login.value?.idEmployee, login = true, idWP = selectedWPId)
        OutData.execute(api){i, apiResponse ->
            refreshData()
        }
    }
    fun logoutOperator(idOperator:Int){
        OutData.logInOutOperatorParams(login.value?.idEmployee, login = false, idWP = selectedWPId)
        OutData.execute(api){i, apiResponse ->
            refreshData()
        }
    }
    fun getOperatorsOperation(id:Int){
        val rpc = Rpc("SPMANTA_Operation_EmployeeOnWP")
        val params = mutableListOf<RpcParam>()
        params.add(RpcParam.putInt("EmployeeID", id))
        params.add(RpcParam.putInt("WorkPlaceID", selectedWPId))
        rpc.addParams(params)
        rpc.execute(api) { code, response ->
            Log.d("OPERATIONS", "Operation operator: $response")
            response.array?.let {
                val ids= mutableListOf<Int>()
                for(i in 0 until it.length()){
                    ids.add(it.getJSONObject(i).getInt("ID"))
                }
                markedOperations.value=ids
            }
        }
    }
    fun loadMenu(wpId:Int, role:Long, error:(Boolean)->Unit){
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.GET,
                MenuDef.urlTerm2(role, wpId),""
            )
        ){ code, response ->

            val gson = Gson()
            val obj= response.getSingleObject()
            obj?.let {
                if(obj.isNull("menuDefinition")) {
                    menuDef=null
                    error(true)
                }else {
                    menuDef = gson.fromJson(
                        obj.getJSONObject("menuDefinition").toString(),
                        MenuDef::class.java
                    )
                    Log.d("menuDef", "$menuDef")


                    menuDef?.let { def ->

                        menu.postValue(
                            MenuButtonTerm.createList(
                                obj.getJSONArray("menuButtons"),
                                def
                            )
                        )
                    }?: kotlin.run { menuDef=null}
                }
            }
        }

    }
    fun verifyAndLoginOperation(code:String, ret:(Int)->Unit){
        val rpc = Rpc("SPMANTA_FindOpBarCode")
        val params = mutableListOf<RpcParam>()
        params.add(RpcParam.putInt("TerminalID", (BaseApp.remoteSettings?.id?:(1)).toInt()))
        params.add(RpcParam.putInt("EmployeeID", (login.value?.idEmployee?.toInt())))
        params.add(RpcParam.putInt("WorkPlaceID", selectedWPId))
        params.add(RpcParam.putString("Barcode", code))
        rpc.addParams(params)
        rpc.execute(api) { code, response ->
            response.getSingleObject()?.let {
                ret(it.getInt("ID")) // todo muze vratit pole a zahajim vse
            }

        }
    }
    fun loginOperation(id:Int,  ret:(Boolean)->Unit){
        val team = workplaces.value?.find { it.id==selectedWPId }?.teamWorking?:(false)
        OutData.logInOutOperationParams(login.value?.idEmployee, login = true, idWP = selectedWPId, idOperation = id, team)
        OutData.execute(api){ i, apiResponse ->
            ret(i==200)
        }
    }
    fun logOutOperation(id:Int, selectedWPId:Int, ret:(Boolean)->Unit){
        val team = workplaces.value?.find { it.id==selectedWPId }?.teamWorking?:(false)
        OutData.logInOutOperationParams(login.value?.idEmployee, login = false, idWP = selectedWPId, idOperation = id, team)
        OutData.execute(api){ i, apiResponse ->
            ret(i==200)
        }
    }
    fun writeIdle(id:Int){
        OutData.writeIdle(selectedWPId, id)
        OutData.execute(api,showOnProcess = true ){ i, apiResponse ->
          refreshData()
        }
    }
    fun refreshData(){
        loadEmployees()
        loadWorkplaces()
        loadContent( user = login.value)


    }
    fun parseData(mess:String) {
       // onProcessSocket.value=true
        val json = JSONObject(mess)
     //   val wp = json.getInt("workplaceId")
         //   if(wp ==selectedWPId) {
                socketDataProcessing(json)
               //     }

    }
    }
