package com.nvsp.manta_terminal.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request
import com.google.gson.Gson
import com.nvsp.manta_terminal.BaseApp
import com.nvsp.manta_terminal.BuildConfig

import com.nvsp.nvmesapplibrary.architecture.BaseViewModel
import com.nvsp.nvmesapplibrary.constants.Const
import com.nvsp.nvmesapplibrary.database.LibRepository
import com.nvsp.nvmesapplibrary.settings.models.Settings

import com.nvsp.nvmesapplibrary.App


import com.nvsp.nvmesapplibrary.communication.volley.ServiceVolley
import com.nvsp.nvmesapplibrary.communication.volley.VolleySingleton
import com.nvsp.nvmesapplibrary.rpc.OutData
import com.nvsp.nvmesapplibrary.utils.CommonUtils
import com.nvsp.nvmesapplibrary.utils.model.ApkInfo
import com.nvsp.nvmesapplibrary.utils.model.RemoteSettings
import com.nvsp.nvmesapplibrary.utils.updater.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

const val LOADED_SETTING=1
const val CONECTED=2
const val UPDATED=3
const val LOADED_REMOTE_SETTING=4
const val  LOGGED=5
const val  NOT_LOGGED=105
const val NO_CONECTED=102
const val NOT_LOADED_REMOTE_SETTING=104

class SplashViewModel(private val repository: LibRepository): BaseViewModel(repository){
    var splasCheckstatus= MutableLiveData<Int>(0)
    val updater: Updater by lazy { Updater(api) }
    val api :ServiceVolley by lazy { ServiceVolley(VolleySingleton.getInstance(), BaseApp.instance.urlApi) }

    fun loadSettings(set:Settings){
        BaseApp.instance.guid=set.uId
        Const.URL_BASE = set.baseUrl()
        BaseApp.instance.urlApi=set.baseUrl()
        Log.d("URL", "URL IS: ${Const.URL_BASE}")
        BaseApp.instance.refresh()
        splasCheckstatus.value=LOADED_SETTING
        App.setip(set.ipAddress, set.port)

    }
    fun skipSettings(){
        splasCheckstatus.value=0
    }
    fun testLogin(){
       login.value?.let {
           splasCheckstatus.postValue(LOGGED)
       }?: kotlin.run {
           splasCheckstatus.postValue(NOT_LOGGED)
       }
    }
    fun loadRemoteSettings(context:Context){
        api.request(
          com.nvsp.nvmesapplibrary.communication.models.Request(
              Request.Method.POST,
          "Devices/CheckRegistration","",
              json = CommonUtils.getRegisterJson(context,BaseApp.instance.guid )
          )
        ){code, response ->
            val remoteSettings=Gson().fromJson(response.getSingleObject().toString(),RemoteSettings::class.java)
            Log.d("REMOTE", remoteSettings.toString())
            BaseApp.remoteSettings=remoteSettings
            OutData.idTerminal = remoteSettings.id
           ServiceVolley.deviceID=remoteSettings.id
            splasCheckstatus.postValue(LOADED_REMOTE_SETTING)
        }



    }
fun testVersion(ret:(ApkInfo?, Boolean, Boolean)->Unit){
    updater.checkVersion(){apk, apkExist,error ->
        Log.d("APK","APK:${apk?.version} ACTUAL VERSION :${BuildConfig.VERSION_CODE}")
        ret(apk, apkExist, error)
    }
}
    fun downloadFile(context:Context){
        updater.enqueueDownload(context)
    }
    fun testConnection(ip:String, port: Int?){
        CoroutineScope(Dispatchers.IO).launch {
            var numOfLostPacket=0
            for(i in 0 until 5){
                Log.d("Test Connection", "test $i ... lost $numOfLostPacket ")
                if(isNetworkAvailable(ip,port?:(80))){
                    numOfLostPacket = 0
                    splasCheckstatus.postValue(CONECTED)
                    break
                }else
                    numOfLostPacket++
                if(numOfLostPacket >2)
                    splasCheckstatus.postValue( NO_CONECTED)
            }
        }



    }

    private  fun isNetworkAvailable(ip:String, port:Int, timeout:Int=1000): Boolean {
        if(ip.contains("ngrok"))
            return true
        else{
            //  val address= InetAddress.getByName(ip+":$port")
            // Log.d("TEST", "adress: ${address.hostAddress} ")
            //return address.isReachable(timeout)

            /* val runtime = Runtime.getRuntime()
             try {
                 val ipProcess = runtime.exec("/system/bin/ping -c 1 $ip")
                 val exitValue = ipProcess.waitFor()
                 return exitValue == 0

             } catch (e: IOException) {
                 e.printStackTrace()
             } catch (e: InterruptedException) {
                 e.printStackTrace()
             }
             return false*/


            try {
                val  socket = Socket()
                val inetAdress = InetAddress.getByName(ip)
                socket.connect(InetSocketAddress(inetAdress, port), timeout)
                socket.close()
                return true
            }

            catch(ce: ConnectException){
                ce.printStackTrace()
                return false
            }

            catch (ex:Exception ) {
                ex.printStackTrace()
                return false
            }

        }
    }

}