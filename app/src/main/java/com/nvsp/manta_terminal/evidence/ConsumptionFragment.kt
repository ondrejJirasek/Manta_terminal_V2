package com.nvsp.manta_terminal.evidence

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import com.google.zxing.integration.android.IntentIntegrator
import com.nvsp.manta_terminal.R
import com.nvsp.manta_terminal.databinding.DialogConsumptionBinding
import com.nvsp.manta_terminal.databinding.DialogDefectBinding
import com.nvsp.manta_terminal.databinding.DialogFillQrLogOperationBinding
import com.nvsp.manta_terminal.databinding.FragmentConsumptionBinding
import com.nvsp.manta_terminal.evidence.models.Batch
import com.nvsp.nvmesapplibrary.architecture.BaseFragment
import com.nvsp.nvmesapplibrary.utils.CaptureActivity

class ConsumptionFragment : BaseFragment<FragmentConsumptionBinding, EvidenceViewModel>(EvidenceViewModel::class) {
    val barcode=MutableLiveData<String>()
    override val bindingInflater: (LayoutInflater) -> FragmentConsumptionBinding
        get() = FragmentConsumptionBinding::inflate




    override fun initViews() {
    binding.btnAddConsumption.setOnClickListener {
        showAddDialog()
    }
    }
private fun showAddDialog(){
    var item:Batch?=null
        val issueCount=MutableLiveData<Double>(0.0)
        val residueCount=MutableLiveData<Double>(0.0)
    var needIssueCount=0.0
        val builder = AlertDialog.Builder(context)
            .create()

         val bindDial=DialogConsumptionBinding.inflate(LayoutInflater.from(context))
        builder.setView(bindDial.root)
        bindDial.btnBacConsumptionDialog.setOnClickListener { builder.dismiss() }
        bindDial.btnOkConsumptionDialog.setOnClickListener {
            item?.let {
                viewModel.postConsumption(it,issueCount.value, residueCount.value){res->
                    if(res)
                    builder.dismiss()
                }
            }


        }
    bindDial.layBatchConsDialog.setEndIconOnClickListener {
        Log.d("ONCLICK", "CLICK ON SCANNER")
        scanByQR()
    }
    bindDial.edBarcode.setOnEditorActionListener { textView, i, keyEvent ->
        if(i== EditorInfo.IME_ACTION_DONE)
            barcode.value = bindDial.edBarcode.text.toString()
        false
    }
    barcode.observe(viewLifecycleOwner){
        Log.d("BArcode", "code #$it#")
        verifyBarcode(it)
    }
     fun count(issue:Double, res:Double, changed:Int){
         Log.d("COUNTER", "vydej :$issue, zbytek:$res need:$needIssueCount zmena $changed")
         when(changed){
             0->{
                 val mIssue = needIssueCount-res
                 if(mIssue!=issue)
                     issueCount.value=mIssue
             }
             1->{
                 val mRes = needIssueCount-issue

                 if(mRes!=res)
                     residueCount.value=mRes
             }
         }
     }

    bindDial.edResConsDialog.setOnEditorActionListener { textView, i, keyEvent ->
        if(i== EditorInfo.IME_ACTION_DONE)
        if(! bindDial.edResConsDialog.text.isNullOrEmpty()){
            count(bindDial.edIssueConsDialog.text.toString().toDouble(),bindDial.edResConsDialog.text.toString().toDouble(), 0)
        }
        false
    }
    bindDial.edIssueConsDialog.setOnEditorActionListener { textView, i, keyEvent ->
        if(i== EditorInfo.IME_ACTION_DONE)
        if(!bindDial.edIssueConsDialog.text.isNullOrEmpty()){
            count(bindDial.edIssueConsDialog.text.toString().toDouble(),bindDial.edResConsDialog.text.toString().toDouble(),1)
        }
        false
    }
    issueCount.observe(viewLifecycleOwner){
Log.d("OBSERVER", "ISSUE: $it")
        bindDial.edIssueConsDialog.setText(it.toString())
    }
    residueCount.observe(viewLifecycleOwner){
        Log.d("OBSERVER", "residue: $it")
        bindDial.edResConsDialog.setText(it.toString())
    }


    viewModel.batchs.observe(viewLifecycleOwner){
        if(it.isNotEmpty()){
            item = it[0]
            item?.let {     batch->
                bindDial.btnOkConsumptionDialog.isEnabled=true
                bindDial.edInfo.setText(batch.note)
                bindDial.edStock.setText(batch.quantityStock.toString())
                needIssueCount=batch.quantityRequested
                bindDial.edResToIssue.setText(needIssueCount.toString())
                issueCount.value=batch.quantityRequested
                residueCount.value=0.0  }

        }else
            bindDial.btnOkConsumptionDialog.isEnabled=false
    }
            builder.show()
}
    override fun onActivityCreated() {

    }
    fun scanByQR(){
        val integrator =  IntentIntegrator(activity).apply {
            captureActivity = CaptureActivity::class.java
            setRequestCode(1)
        }
        //integrator.initiateScan()
        resultLauncher.launch(integrator.createScanIntent())
    }
    private var resultLauncher = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)

            if(intentResult.contents != null) {
                barcode.value=intentResult.contents

            }
        }
    }
    private fun verifyBarcode(barcode: String){
        viewModel.verifyBarcode(barcode)
    }

}