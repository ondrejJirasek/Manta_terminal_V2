package com.nvsp.manta_terminal.evidence

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator
import com.nvsp.REMOVE
import com.nvsp.UPDATE
import com.nvsp.manta_terminal.R
import com.nvsp.manta_terminal.databinding.*
import com.nvsp.manta_terminal.evidence.models.Batch
import com.nvsp.manta_terminal.evidence.models.ConsumptionRecord
import com.nvsp.manta_terminal.evidence.models.NokItems
import com.nvsp.nvmesapplibrary.architecture.BaseFragment
import com.nvsp.nvmesapplibrary.constants.Const
import com.nvsp.nvmesapplibrary.utils.CaptureActivity

class ConsumptionFragment : BaseFragment<FragmentConsumptionBinding, EvidenceViewModel>(EvidenceViewModel::class) {
   private val barcode=MutableLiveData<String>()
   private  val consAdapter :ConsumptionAdapter by lazy {
        ConsumptionAdapter(context, mutableListOf()){item, mode ->
            when(mode){
                UPDATE->showAddDialog(item)
                REMOVE->showRemoveDialog(item)
            }


        }
    }
    override val bindingInflater: (LayoutInflater) -> FragmentConsumptionBinding
        get() = FragmentConsumptionBinding::inflate




    override fun initViews() {
        initRecyclers()
        viewModel.loadConsumption()
    binding.btnAddConsumption.setOnClickListener {
        showAddDialog()
    }
        binding.btnIssue.setOnClickListener {
            viewModel.issueComp()
        }
        viewModel.onProgressCons.observe(viewLifecycleOwner){
            binding.refConsumption.isRefreshing=it
        }
        viewModel.consumptions.observe(viewLifecycleOwner){
            Log.d("CONSUMPTIONS", it.toString())
            consAdapter.setNewItems(it)
        }
        binding.refConsumption.setOnRefreshListener {
            viewModel.loadConsumption()
        }
    }
    private fun showRemoveDialog(item:ConsumptionRecord){
        infoDialog.showWithMessage(
            getString(R.string.removeItem),
            Const.LEVEL_QUESTION
        ) {
            if (it) {
                viewModel.removeCons(item.id) {ret->
                  if(ret)
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
private fun showAddDialog(cons:ConsumptionRecord?=null){
    val builder = AlertDialog.Builder(context)
        .create()
    val bindDial=DialogConsumptionBinding.inflate(LayoutInflater.from(context))
    builder.setView(bindDial.root)
    barcode.postValue("")
    var item:Batch?=null
    val issueCount=MutableLiveData<Double>(0.0)
    val residueCount=MutableLiveData<Double>(0.0)
    var stock=0.0
    fun count(issue:Double, res:Double, changed:Int){

        Log.d("COUNTER", "sklad:$stock vydej :$issue, zbytek:$res need:$stock zmena $changed")
        when(changed){
            0->{
                val mIssue = stock-res
                if(mIssue!=issue)
                    issueCount.value=mIssue
            }
            1->{
                val mRes = stock-issue

                if(mRes!=res)
                    residueCount.value=mRes
            }
        }
    }


    cons?.let {
        viewModel.consumptionLock(it.id, true)
        builder.setCanceledOnTouchOutside(false)
        with(bindDial) {
            edBarcode.isEnabled=false
           // edInfo.text = it.
            stock = it.quantityStock.toDouble()
            edStock.setText( it.quantityStock.toString())
            edResToIssue.setText(it.quantityRequested.toString())
            issueCount.value = it.quantity.toDouble()
            btnOkConsumptionDialog.isEnabled= true
           // edIssueConsDialog.setText(.toString())
            count(it.quantity.toDouble(),0.0, 1)
        }
    }
        bindDial.btnBacConsumptionDialog.setOnClickListener {
            cons?.let {
                viewModel.consumptionLock(it.id, true)
            }
            builder.dismiss() }
        bindDial.btnOkConsumptionDialog.setOnClickListener {

            cons?.let {
                viewModel.consumptionLock(it.id, false)
            }
            cons?.let {
                viewModel.updateConsumption(it.id,it.warehouseSelectionId, it.structureId,issueCount.value, residueCount.value){res->
                    if(res)
                        builder.dismiss()
                }
            }

            item?.let {
                viewModel.postConsumption(it.warehouseSelectionId, it.structureId,issueCount.value, residueCount.value){res->
                    if(res)
                    builder.dismiss()
                }
            }
            barcode.value=""

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


    bindDial.edResConsDialog.setOnEditorActionListener { textView, i, keyEvent ->
        residueCount.postValue(bindDial.edResConsDialog.text.toString().toDouble())
        if(i== EditorInfo.IME_ACTION_DONE)
        if(! bindDial.edResConsDialog.text.isNullOrEmpty()){
            count(bindDial.edIssueConsDialog.text.toString().toDouble(),bindDial.edResConsDialog.text.toString().toDouble(), 0)
        }
        false
    }
    bindDial.edIssueConsDialog.setOnEditorActionListener { textView, i, keyEvent ->

        if(bindDial.edIssueConsDialog.text.toString().toDouble()>stock)
            bindDial.edIssueConsDialog.setText(stock.toString())
     
        issueCount.postValue(bindDial.edIssueConsDialog.text.toString().toDouble())
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

if(cons==null)
    viewModel.batchs.observe(viewLifecycleOwner){
        if(it.isNotEmpty()){
            item = it[0]
            item?.let {     batch->
                bindDial.btnOkConsumptionDialog.isEnabled=true
                bindDial.edInfo.setText(batch.note)
                bindDial.edStock.setText(batch.quantityStock.toString())
                stock=batch.quantityStock
                bindDial.edResToIssue.setText(batch.quantityRequested.toString())

                issueCount.value=if(batch.quantityRequested>batch.quantityStock)
                    batch.quantityStock
                else
                     batch.quantityRequested
                residueCount.value=0.0  }

        }else {
            bindDial.edInfo.setText("")
            bindDial.edStock.setText("")
            stock = 0.0
            bindDial.edResToIssue.setText("")
            issueCount.value = 0.0
            residueCount.value = 0.0
            bindDial.btnOkConsumptionDialog.isEnabled = false
        }
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
        if(barcode.isNotEmpty())
        viewModel.verifyBarcode(barcode)
        else
            viewModel.batchs.value= emptyList()
    }

    fun initRecyclers(){
        with(binding.recConsumption){
            val lm = LinearLayoutManager(context)
            layoutManager = lm
            setHasFixedSize(true)
            adapter = consAdapter
        }
    }
    inner class ConsumptionAdapter(val context: Context?, val dataset:MutableList<ConsumptionRecord>, val action:(item: ConsumptionRecord, mode:Int)->Unit): RecyclerView.Adapter<ConsumptionAdapter.Holder>() {

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnCreateContextMenuListener {
            val binding = ItemConsumptionBinding.bind(itemView)
            fun bindItem(item: ConsumptionRecord) {
                itemView.setOnCreateContextMenuListener(this)
            }

            override fun onCreateContextMenu(
                menu: ContextMenu,
                v: View,
                menuInfo: ContextMenu.ContextMenuInfo?
            ) {
                val currentPos = adapterPosition
                val item = dataset[currentPos]
                val edit = menu.add(
                    0,
                    v.id,
                    0,
                    context?.getString(com.nvsp.nvmesapplibrary.R.string.edit)
                )
                val remove = menu.add(
                    0,
                    v.id,
                    0,
                    context?.getString(com.nvsp.nvmesapplibrary.R.string.remove)
                )
                edit.setOnMenuItemClickListener {
                    Log.d("MENU", "smazani")
                    action(item, UPDATE)
                    false
                }
                remove.setOnMenuItemClickListener {
                    Log.d("MENU", "smazani")
                    action(item, REMOVE)
                    false
                }


            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemConsumptionBinding.inflate(LayoutInflater.from(context), parent, false)
            return Holder(binding.root)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = dataset[position]
            holder.bindItem(item)
            with(holder.binding) {
                Log.d("BINDING", "item:$item")
                tvBatchConsumption.text = item.barCode
                tvPlaceConsumption.text =item.location
                tvNameConsumption.text = item.sourceName
                tvStockConsumption.text = item.quantityStock.toString()
                tvToIssueConstumption.text = item.quantityRequested.toString()
                tvIssueConsumption.text = item.quantity.toString()
                tvMjConsumption.text = item.mu

            }
        }

        override fun getItemCount(): Int {
            return dataset.size
        }

        fun setNewItems(dataSetNew: List<ConsumptionRecord>) {
            val diffUtil = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return dataset.size
                }

                override fun getNewListSize(): Int {
                    return dataSetNew.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

                    Log.d("CONS compare", "is Same ? ${dataset[oldItemPosition] == dataSetNew[newItemPosition]}")
                    return dataset[oldItemPosition] == dataSetNew[newItemPosition]

                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem= dataset[oldItemPosition]
                    val newitem =dataSetNew[newItemPosition]
                    val state =ConsumptionRecord.isSame(oldItem,newitem)
                    Log.d("CONS compare", "is Same content? $state")
                    return ConsumptionRecord.isSame(oldItem,newitem)
                }
            })
            dataset.clear()
            dataset.addAll(dataSetNew)
            diffUtil.dispatchUpdatesTo(this)

        }
    }


}