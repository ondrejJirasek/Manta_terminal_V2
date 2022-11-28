package com.nvsp.manta_terminal.ui.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.nvsp.manta_terminal.BaseApp
import com.nvsp.manta_terminal.BuildConfig
import com.nvsp.manta_terminal.R
import com.nvsp.manta_terminal.viewmodels.*
import com.nvsp.manta_terminal.viewmodels.SplashViewModel
import com.nvsp.nvmesapplibrary.architecture.InfoDialog
import com.nvsp.nvmesapplibrary.constants.Const

import com.nvsp.nvmesapplibrary.constants.Keys
import com.nvsp.nvmesapplibrary.login.LoginActivity
import com.nvsp.nvmesapplibrary.settings.SettingsActivity
import com.nvsp.nvmesapplibrary.utils.model.ApkInfo
import kotlinx.coroutines.launch

import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashScreen : AppCompatActivity() {
    private val mViewModel: SplashViewModel by lazy {
        getViewModel(
            null,
            SplashViewModel::class
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        lifecycleScope.launch {
            mViewModel.logOut()
        }
        mViewModel.activeSetting.observe(this) {
            it?.let { set ->
                if (set.uId.isNullOrEmpty()||set.uId=="NONAME")
                    startActivity(SettingsActivity.createIntent(this))
                else
                     mViewModel.loadSettings(set)
            } ?: (startActivity(SettingsActivity.createIntent(this)))

        }
        mViewModel.login.observe(this) {
            it?.let {
                mViewModel.splasCheckstatus.postValue(LOGGED)
            }
        }
        mViewModel.splasCheckstatus.observe(this) { action ->
            when (action) {
                LOADED_SETTING -> {
                    Log.d("SPLASH", "LOADED SETTINGS")

                    mViewModel.activeSetting.value?.let {    mViewModel.testConnection(it.ipAddress, it.port) }
                }
                NO_CONECTED -> {
                    Log.d("SPLASH", "NOT CONECTED")
                    startActivity(SettingsActivity.createIntent(this))
                }
                CONECTED -> {
                    Log.d("SPLASH", "CONECTED")
                    mViewModel.testVersion(){apkInfo, apkExist, error ->
                        if(error)
                            startActivity(SettingsActivity.createIntent(this))
                        else
                        /* if(apkInfo==null && !apkExist){
                             infoDialog.showWithMessage(mViewModel.errorMessage.value?.message?:(""), Const.LEVEL_ERROR) {

                             }
                         }else*/
                            apkInfo?.let { updaterDialog(it,apkExist)  }?: kotlin.run {
                                //  InfoDialog(this).showWithMessage()
                                mViewModel.splasCheckstatus.postValue(UPDATED)
                            }

                    }
                }
                UPDATED -> {
                    Log.d("SPLASH", "UPDATED")
                    mViewModel.loadRemoteSettings(this)
                }
                LOADED_REMOTE_SETTING -> {
                    Log.d("SPLASH", "LOADED REMOTE SETTINGS")
                    mViewModel.testLogin()
                }
                NOT_LOGGED -> {
                    Log.d("SPLASH", "NOT_LOGED")
                    startApp()
                }
                LOGGED -> {
                    Log.d("SPLASH", "STARTAPP")
                    startApp()
                }

            }
        }

    }
    fun startApp(){
        startActivity(MainActivity.createIntent(this))
        finish()
    }
    fun Context.getAppName(): String = applicationInfo.loadLabel(packageManager).toString()
    fun updaterDialog(apk: ApkInfo, apkExist:Boolean){
        val localVersion = mViewModel.updater.getVersion(BuildConfig.VERSION_NAME)
        val nameAPK =getAppName()

        val dialog = InfoDialog(this)
        dialog.setTitle(getString(com.nvsp.nvmesapplibrary.R.string.Aktualizace))
        if(apkExist){
            if(localVersion!=apk.version){
                if(localVersion>apk.version){
                    dialog.showWithMessage("Vaše verze aplikace $nameAPK je ve verzi $localVersion na serveru je k dispozici verze ${apk.version}. Přejete si snížit verzi? ", level = Const.LEVEL_UPDATE){
                        if(it)
                            mViewModel.downloadFile(this)
                        dialog.dismiss()


                        mViewModel.splasCheckstatus.postValue(UPDATED)
                    }
                }
                if(localVersion<apk.version){
                    dialog.showWithMessage("Je k dispozici novější verze aplikace $nameAPK ${apk.version}. Vaše verze je $localVersion. Přejete si aplikaci aktualizovat? ", level = Const.LEVEL_UPDATE){
                        if(it)
                            mViewModel.downloadFile(this)
                        dialog.dismiss()
                        mViewModel.splasCheckstatus.postValue(UPDATED)
                    }
                }
            }else{
                mViewModel.splasCheckstatus.postValue(UPDATED)

            }            }else{
            mViewModel.splasCheckstatus.postValue(UPDATED)
        }
    }
    fun startLoginActivity(){
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtras(BaseApp.createLoginBundle())
        startActivity(intent)
    }
}