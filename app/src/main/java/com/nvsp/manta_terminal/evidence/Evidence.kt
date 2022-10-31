package com.nvsp.manta_terminal.evidence

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.nvsp.manta_terminal.Const
import com.nvsp.manta_terminal.databinding.FragmentEvidenceBinding
import com.nvsp.nvmesapplibrary.R
import com.nvsp.nvmesapplibrary.architecture.BaseFragment



class Evidence : BaseFragment<FragmentEvidenceBinding, EvidenceViewModel>(EvidenceViewModel::class) {

    private val args: EvidenceArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override val bindingInflater: (LayoutInflater) -> FragmentEvidenceBinding
        get() = FragmentEvidenceBinding::inflate

    override fun initViews() {
        when(args.mode){
            Const.MODE_EVIDENCE_HEI->{
                binding.btnOkCycles.visibility= View.GONE
            }
            Const.MODE_EVIDENCE_HEN->{
                binding.btnOkCycles.visibility= View.VISIBLE
            }
        }


    }

    override fun onActivityCreated() {
    setBackButton(true)
    }


}