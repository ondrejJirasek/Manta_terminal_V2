package com.nvsp.manta_terminal.evidence

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nvsp.REMOVE
import com.nvsp.UPDATE
import com.nvsp.manta_terminal.R
import com.nvsp.manta_terminal.databinding.FragmentNokItemsBinding
import com.nvsp.manta_terminal.databinding.ItemNokBinding

import com.nvsp.manta_terminal.evidence.models.NokItems
import com.nvsp.nvmesapplibrary.architecture.BaseFragment
import com.nvsp.nvmesapplibrary.constants.Const

class NokItemsFragment : BaseFragment<FragmentNokItemsBinding, EvidenceViewModel>(EvidenceViewModel::class) {
    private val nokAdapter: NokAdapter by lazy{
        NokAdapter(context, mutableListOf()){ item, mode ->
            if(mode== REMOVE)
                removeDialogNok(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override val bindingInflater: (LayoutInflater) -> FragmentNokItemsBinding
        get() = FragmentNokItemsBinding::inflate

    override fun initViews() {
        viewModel.selectedOperation?.let {
            viewModel.loadNok(viewModel.teamWorking,it.operationId,(viewModel.login.value?.idEmployee?:(0)).toInt(),viewModel.wpId )
        }

        initRecyclers()

        viewModel.onProgressNOK.observe(viewLifecycleOwner){binding.refNok.isRefreshing=it}
      Log.d("NOK", "selected Operation:${viewModel.selectedOperation}")

        viewModel.nokList.observe(viewLifecycleOwner){
            nokAdapter.setNewItems(it)
        }

    }
    private fun removeDialogNok(item:NokItems){
        infoDialog.showWithMessage(
            getString(R.string.removeItem),
            Const.LEVEL_QUESTION
        ) {
            if (it) {

                viewModel.removeNok(item.id) {
                    viewModel.refreshOperationAndNOK(viewModel.teamWorking,viewModel.wpId,
                        viewModel.selectedOperation?.operationId?:(0))
                    Toast.makeText(
                        requireContext(),
                        getString(com.nvsp.nvmesapplibrary.R.string.itemDeleted),
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }
    }

    override fun onActivityCreated() {

    }

    fun initRecyclers(){
        with(binding.recNok){
            val lm = LinearLayoutManager(context)
            layoutManager = lm
            setHasFixedSize(true)
            adapter = nokAdapter
        }
    }
    inner class NokAdapter(val context: Context?, val dataset:MutableList<NokItems>, val action:(item: NokItems, mode:Int)->Unit): RecyclerView.Adapter<NokAdapter.Holder>() {

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnCreateContextMenuListener {
            val binding = ItemNokBinding.bind(itemView)
            fun bindItem(item: NokItems) {
                itemView.setOnCreateContextMenuListener(this)
            }

            override fun onCreateContextMenu(
                menu: ContextMenu,
                v: View,
                menuInfo: ContextMenu.ContextMenuInfo?
            ) {
                val currentPos = adapterPosition
                val item = dataset[currentPos]

                val remove = menu.add(
                    0,
                    v.id,
                    0,
                    context?.getString(com.nvsp.nvmesapplibrary.R.string.remove)
                )
                remove.setOnMenuItemClickListener {
                    Log.d("MENU", "smazani")
                    action(item, REMOVE)
                    false
                }


            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemNokBinding.inflate(LayoutInflater.from(context), parent, false)
            return Holder(binding.root)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = dataset[position]
            holder.bindItem(item)
            with(holder.binding) {
                Log.d("BINDING", "item:$item")
                tvDefect.text = item.getDefect()
                tvPcs.text =item.quantity.toString()
                tvNote.text = item.defectNote
                //  ivCon.setImageResource()
            }
        }

        override fun getItemCount(): Int {
            return dataset.size
        }

        fun setNewItems(dataSetNew: List<NokItems>) {
            val diffUtil = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return dataset.size
                }

                override fun getNewListSize(): Int {
                    return dataSetNew.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {


                    return dataset[oldItemPosition] == dataSetNew[newItemPosition]

                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem= dataset[oldItemPosition]
                    val newitem =dataSetNew[newItemPosition]
                    return oldItem.id==newitem.id &&
                            oldItem.defectCode==newitem.defectCode &&
                            oldItem.employeeId==newitem.employeeId &&
                            oldItem.quantity==newitem.quantity
                }
            })
            dataset.clear()
            dataset.addAll(dataSetNew)
            diffUtil.dispatchUpdatesTo(this)

        }
    }
}