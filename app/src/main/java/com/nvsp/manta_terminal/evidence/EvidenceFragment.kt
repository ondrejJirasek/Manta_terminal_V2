package com.nvsp.manta_terminal.evidence

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.nvsp.manta_terminal.R
import com.nvsp.manta_terminal.databinding.FragmentEvidence2Binding
import com.nvsp.nvmesapplibrary.architecture.BaseFragment

class EvidenceFragment : BaseFragment<FragmentEvidence2Binding, EvidenceViewModel>(EvidenceViewModel::class) {
    override val bindingInflater: (LayoutInflater) -> FragmentEvidence2Binding
        get() = FragmentEvidence2Binding::inflate

    override fun initViews() {
        binding.btnEvidence.setOnClickListener {
            viewModel.evidence() {
                findNavController().navigateUp()
            }
        }
    }

    override fun onActivityCreated() {

    }

}