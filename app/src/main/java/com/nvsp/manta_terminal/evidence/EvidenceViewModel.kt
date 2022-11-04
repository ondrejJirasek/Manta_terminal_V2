package com.nvsp.manta_terminal.evidence

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request
import com.google.gson.reflect.TypeToken
import com.nvsp.manta_terminal.BaseApp
import com.nvsp.manta_terminal.evidence.models.*
import com.nvsp.nvmesapplibrary.architecture.CommunicationViewModel
import com.nvsp.nvmesapplibrary.communication.volley.ServiceVolley
import com.nvsp.nvmesapplibrary.database.LibRepository
import com.nvsp.nvmesapplibrary.models.DEFECT_CODE
import com.nvsp.nvmesapplibrary.models.DefectCode
import com.nvsp.nvmesapplibrary.rpc.OutData
import com.nvsp.nvmesapplibrary.rpc.Rpc
import com.nvsp.nvmesapplibrary.rpc.RpcParam
import org.json.JSONObject

class EvidenceViewModel(repository: LibRepository, private val api: ServiceVolley) : CommunicationViewModel(repository, api){
    var teamWorking:Boolean=false
    var wpId: Int = -1
    var count:Int = 1
    val onProgressNOK = MutableLiveData<Boolean>(false)
    val onProgressActive = MutableLiveData<Boolean>(false)
    val defectCodes = MutableLiveData<List<DefectCode>>()
    val activeOperationList = MutableLiveData<List<ActiveOperation>>()
    var selectedOperation:ActiveOperation?=null
    val nokList = MutableLiveData<List<NokItems>>()
    val workShifts = MutableLiveData<List<WorkShift>>()
    val batchs = MutableLiveData<List<Batch>>()
    fun loadDefectCodes() {
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.GET,
                DEFECT_CODE,
                "",
                login.value,
                null
            ),
            showProgress = false,
            hideProgressOnEnd = false
        ) { code, response ->

            val itemType = object : TypeToken<List<DefectCode>>() {}.type
            defectCodes.value = gson.fromJson(response.array.toString(), itemType)
        }
    }
    fun loadShifts() {
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.GET,
                WORKSHIFT,
                "",
                login.value,
                null
            ),
            showProgress = false,
            hideProgressOnEnd = false
        ) { code, response ->

            val itemType = object : TypeToken<List<WorkShift>>() {}.type
            workShifts.value = gson.fromJson(response.array.toString(), itemType)
        }

//todo
    }
    fun loadActiveOperation(tw:Boolean, wpId:Int, employeeId:Int){

        onProgressActive.value=true
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.GET,
                ActiveOperation.getUrl(tw,wpId,employeeId),
                "",
                login.value,
                null),
            showProgress = false,
            hideProgressOnEnd = false){code, response ->
            onProgressActive.value=false
            val itemType = object :  TypeToken<List<ActiveOperation>>(){}.type
            activeOperationList.value = gson.fromJson(response.array.toString(), itemType)


        }
    }
    fun removeNok(id:Int, ret:(Boolean)->Unit){

        val rpc = Rpc("SPMANTA_DelNOKList")
        val params = mutableListOf<RpcParam>()
        params.add(RpcParam.putInt("ID_OutData", id))
        rpc.addParams(params)
        rpc.execute(api) { code, response ->

            ret(true)
        }
    }
    fun okCycles(wpId:Int, operation:Int,qty:Int, ret: (Boolean) -> Unit){
        OutData.evidenceOkCycles(login.value?.idEmployee,  idWP = wpId, idOperation =operation, count = qty )
        OutData.execute(api){ i, apiResponse ->
            ret(i==200)
        }
    }
    fun okItems(wpId:Int, operation:Int,qty:Int, ret: (Boolean) -> Unit){
        OutData.evidenceOkPcs(login.value?.idEmployee,  idWP = wpId, idOperation =operation, count = qty )
        OutData.execute(api){ i, apiResponse ->
            ret(i==200)
        }
    }    fun nokItems(wpId:Int, operation:Int,qty:Int,defectCode:Int,type:Int, note:String?, ret: (Boolean) -> Unit){
        OutData.evidenceNokPcs(login.value?.idEmployee,  idWP = wpId, idOperation =operation, count = qty, defectCodeId = defectCode, type = type, note = note  )
        OutData.execute(api){ i, apiResponse ->
            ret(i==200)
        }
    }
    fun loadNok(tw:Boolean, opId:Int, employeeId:Int, wpId:Int){
        onProgressNOK.value=true
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.GET,
                NokItems.getUrl(tw,opId,employeeId,wpId),
                "",
                login.value,
                null),
            showProgress = false,
            hideProgressOnEnd = false){code, response ->
            onProgressNOK.value=false
            val itemType = object :  TypeToken<List<NokItems>>(){}.type
            nokList.value = gson.fromJson(response.array.toString(), itemType)


        }
    }
    fun refreshOperationAndNOK(tw:Boolean, wpId:Int, opId:Int){
        Log.d("REFRESHALL", "teamWork:$tw wpId:$wpId, operationID:$opId")
        val employeeId=(login.value?.idEmployee?:(0)).toInt()
        loadActiveOperation(tw, wpId, employeeId)
        loadNok(tw,opId,employeeId, wpId )
    }

    fun verifyBarcode(code:String){

        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.GET,
                Batch.getUrl(selectedOperation?.operationId?:(-1),code),
                "",
                login.value,
                null),
            showProgress = true,
            hideProgressOnEnd = true){code, response ->

            val itemType = object :  TypeToken<List<Batch>>(){}.type
            batchs.value = gson.fromJson(response.array.toString(), itemType)


        }
    }
    fun postConsumption(batch:Batch, issue:Double?, res:Double?,ret: (Boolean) -> Unit){
        val json= JSONObject(     """{
            "terminalId": ${BaseApp.remoteSettings?.id?:(-1)},
            "employeeId": ${login.value?.idEmployee},
            "productOrderId": ${selectedOperation?.productOrderId},
            "operationId": ${selectedOperation?.operationId},
            "wpId": ${wpId},
            "quantity": $issue,
            "warehouseSelectionId": ${batch.warehouseSelectionId},
            "structuredId": ${batch.structureId},
            "variationId":0 
        }"""   )
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.POST,
                "WorkRecord/MaterialConsumption",
                "",
                login.value,
                json),
            showProgress = true,
            hideProgressOnEnd = true){code, _ ->
            ret(code==200)
        }


    }
    fun evidence(ret: (Boolean) -> Unit){
        val json = JSONObject()
        json.put("workplaceId",wpId )
        json.put("employeeId",login.value?.idEmployee )
        json.put("terminalId", BaseApp.remoteSettings?.id )
        json.put("operationId", selectedOperation?.operationId )
        api.request(
            com.nvsp.nvmesapplibrary.communication.models.Request(
                Request.Method.POST,
                "WorkRecord/MakeRecords",
                "",
                login.value,
                json),
            showProgress = true,
            hideProgressOnEnd = true){code, _ ->
            ret(code==200)
        }
    }
}