package com.nvsp.manta_terminal.evidence

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.nvsp.manta_terminal.R
import com.nvsp.manta_terminal.databinding.DialogConsumptionBinding
import com.nvsp.manta_terminal.databinding.DialogEvidencePrintBinding
import com.nvsp.manta_terminal.databinding.FragmentEvidence2Binding
import com.nvsp.nvmesapplibrary.architecture.BaseFragment
import com.nvsp.nvmesapplibrary.constants.Const

class EvidenceFragment : BaseFragment<FragmentEvidence2Binding, EvidenceViewModel>(EvidenceViewModel::class) {
    override val bindingInflater: (LayoutInflater) -> FragmentEvidence2Binding
        get() = FragmentEvidence2Binding::inflate

    override fun initViews() {
        binding.btnEvidence.setOnClickListener {
            viewModel.evidence() {
                findNavController().navigateUp()
            }
        }
        binding.btnEvidencePrint.setOnClickListener {
        showEvPrintDialog()
        }
    }

    override fun onActivityCreated() {

    }
   private  fun showEvPrintDialog(){
        val builder = AlertDialog.Builder(context)
            .create()
        val bindDial= DialogEvidencePrintBinding.inflate(LayoutInflater.from(context))
        builder.setView(bindDial.root)
       Log.d("SELECTED OPERATION", "selected Operation: ${viewModel.selectedOperation}")
       val selectedOper = viewModel.activeOperationList.value?.find {it.operationId == viewModel.selectedOperation?.operationId  }
       bindDial.edDialogEvPrintNumber.setText(selectedOper?.ok?.toInt().toString())
       bindDial.edDialogevPrintQuantity.setText(selectedOper?.ok.toString())
        bindDial.btnEvPrintBack.setOnClickListener {
            builder.dismiss()
        }
        bindDial.btnDialogEvPrintOk.setOnClickListener {
            try {
              val quantity = bindDial.edDialogevPrintQuantity.text.toString().toDouble()
                val  number=bindDial.edDialogEvPrintNumber.text.toString().toInt()
               if (quantity>0 && number>0){
                viewModel.generateOkLabels(viewModel.selectedOperation?.operationId?:(-1),quantity, number){
                    if(it)
                        viewModel.evidence() {
                            findNavController().navigateUp()
                        }
                }
               }
           }catch (e:Exception ){
               Log.e("ERROR print dialog", "$e")
               infoDialog.showWithMessage(getString(R.string.badValues), Const.LEVEL_ERROR){
               }
              }
            builder.dismiss()
        }
        builder.show()
    }

}