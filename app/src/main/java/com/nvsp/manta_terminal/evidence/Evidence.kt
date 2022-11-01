package com.nvsp.manta_terminal.evidence

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.nvsp.REMOVE
import com.nvsp.manta_terminal.Const
import com.nvsp.manta_terminal.R
import com.nvsp.manta_terminal.databinding.DialogDefectBinding
import com.nvsp.manta_terminal.databinding.FragmentEvidenceBinding
import com.nvsp.manta_terminal.databinding.ItemOperationBinding
import com.nvsp.manta_terminal.evidence.models.ActiveOperation
import com.nvsp.nvmesapplibrary.architecture.BaseFragment



class Evidence : BaseFragment<FragmentEvidenceBinding, EvidenceViewModel>(EvidenceViewModel::class) {


    private val args: EvidenceArgs by navArgs()
    companion object{
        var selectedOperation:ActiveOperation?=null
    }
    private val activeOperationAdapter: ActiveOperationAdapter by lazy{
        ActiveOperationAdapter(context, mutableListOf(),{
            onOperClick(it)
        },{item, mode ->


        })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.wpId = args.workplaceID
        viewModel.teamWorking = args.teamWorking
    }

    override val bindingInflater: (LayoutInflater) -> FragmentEvidenceBinding
        get() = FragmentEvidenceBinding::inflate

    override fun initViews() {
        viewModel.loadDefectCodes()
        when(args.mode){
            Const.MODE_EVIDENCE_HEI->{
                binding.btnOkCycles.visibility= View.GONE
            }
            Const.MODE_EVIDENCE_HEN->{
                binding.btnOkCycles.visibility= View.VISIBLE
            }
        }
        binding.edCount.setOnEditorActionListener { textView, i, keyEvent ->
            if(i== EditorInfo.IME_ACTION_DONE)
                if(binding.edCount.text.toString().isNullOrEmpty())
                    binding.edCount.setText("0")
            viewModel.count=binding.edCount.text.toString().toInt()
            false
        }
        binding.btnIncrease.setOnClickListener { increaseCount() }
        binding.btnDecrease.setOnClickListener { decreaseCount() }
        binding.btnOkCycles.setOnClickListener {
            selectedOperation?.let {
                viewModel.okCycles(wpId = viewModel.wpId, operation = it.operationId,getCount()){
                    viewModel.loadActiveOperation(viewModel.teamWorking, viewModel.wpId, viewModel.login.value?.idEmployee!!.toInt())
                }
            }

        }
        binding.btnOkItems.setOnClickListener {            selectedOperation?.let {
            viewModel.okItems(wpId = viewModel.wpId, operation = it.operationId,getCount()){
                viewModel.loadActiveOperation(viewModel.teamWorking, viewModel.wpId, viewModel.login.value?.idEmployee!!.toInt())
            }
        }
        }
        binding.btnNOKCount.setOnClickListener { showNokDialog() }


    //    viewModel.loadDefectCodes()
        setOkEnabled(false)
        setOkCyclesEnabled(false)
        setNOKEnabled(false)
        viewModel.login.observe(viewLifecycleOwner){it1->
            it1?.let{
                viewModel.loadActiveOperation(args.teamWorking, args.workplaceID, it.idEmployee!!.toInt())
            }
        }
        initRecyclers()
        with(binding) {
            pager.adapter=ScreenAdapter(childFragmentManager, lifecycle)
            TabLayoutMediator(tabScreen, pager) { tab, pos ->
                tab.text= when (pos) {
                    0 -> getString(R.string.info)
                    1 -> getString(R.string.NOK)
                    2 -> getString(R.string.consumption)
                    3 -> getString(R.string.evidence)
                    else -> getString(R.string.info)
                }
            }.attach()
        }




        viewModel.onProgressActive.observe(viewLifecycleOwner){binding.refOperation.isRefreshing=it}
        binding.refOperation.setOnRefreshListener { viewModel.loadActiveOperation(args.teamWorking, args.workplaceID, (viewModel.login.value?.idEmployee?:(0)).toInt())  }
        viewModel.activeOperationList.observe(viewLifecycleOwner){
            if (it.size==1)
                onOperClick(it[0])
            activeOperationAdapter.setNewItems(it)
        }
    }

    override fun onActivityCreated() {
    setBackButton(true)
    }
    private fun initRecyclers(){
        with(binding.recOperation){
            val lm = LinearLayoutManager(context)
            layoutManager = lm
            setHasFixedSize(true)
            adapter = activeOperationAdapter
        }

    }
    private fun showNokDialog(){

        val builder = AlertDialog.Builder(context)
            .create()
        val bindDial = DialogDefectBinding.inflate(LayoutInflater.from(context))
        builder.setView(bindDial.root)
        bindDial.btnBackDefects.setOnClickListener { builder.dismiss() }
        bindDial.btnDialogFillDefect.setOnClickListener {
            val def =viewModel.defectCodes.value?.find { it.toString() == bindDial.spDefect.text.toString() }
            viewModel.selectedOperation?.let {oper->
                val slectedType =bindDial.radioGroup.indexOfChild( bindDial.radioGroup.findViewById(bindDial.radioGroup.checkedRadioButtonId))
                viewModel.nokItems(wpId = viewModel.wpId, operation = oper.operationId,viewModel.count, defectCode = def?.id?:(0), type = slectedType, note = bindDial.edNoteNok.text.toString() ){
                    viewModel.loadActiveOperation(viewModel.teamWorking, viewModel.wpId, viewModel.login.value?.idEmployee!!.toInt())
                    viewModel.loadNok(viewModel.teamWorking,oper.operationId,(viewModel.login?.value?.idEmployee?:(0)).toInt(),viewModel.wpId )
                    //    viewModel.loadActiveOperation(MainActivity.teamWorking, MainActivity.wpId, viewModel.login.value?.idEmployee!!.toInt())
                }
            }
            builder.dismiss()
        }
        bindDial.spDefect.addTextChangedListener {
            bindDial.btnDialogFillDefect.isEnabled=!it.isNullOrEmpty()
        }
        viewModel.defectCodes.value?.let {
            Log.d("addArray", "Size: ${it.size}")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,it)
            bindDial.spDefect.setAdapter(adapter)

        }
        builder.show()

    }
    private fun getCount():Int{
        return  if(binding.edCount.text.isNullOrEmpty())
            0
        else
            binding.edCount.text.toString().toInt()
    }
    private fun increaseCount() {
        display(viewModel.count.toString().toInt() + 1)
    }

    private fun decreaseCount() {
        display(viewModel.count.toString().toInt() - 1)
    }
    private fun display(number: Int) {
        viewModel.count=number
        binding.edCount.setText("${viewModel.count}")
    }
    private fun onOperClick(item:ActiveOperation){
        selectedOperation = item
        viewModel.selectedOperation = selectedOperation
        setOkEnabled(true)
        setNOKEnabled(true)
        setOkCyclesEnabled(item.teamWorking?:(false))
        //todo Udelat refresher pro zalozky

      //  viewModel.loadNok(MainActivity.teamWorking,item.operationId,(viewModel.login?.value?.idEmployee?:(0)).toInt(),MainActivity.wpId  )
    }
    fun setOkEnabled(state:Boolean){
        with(binding){
            btnOkItems.isEnabled=state
            if(state){
                btnOkItems.background = resources.getDrawable(com.nvsp.nvmesapplibrary.R.drawable.button_blue)
            }else{
                btnOkItems.background = resources.getDrawable(com.nvsp.nvmesapplibrary.R.drawable.button_grey)
            }
        }
    }
    fun setOkCyclesEnabled(state:Boolean){
        with(binding){
            btnOkCycles.isEnabled=state
            if(state){
                btnOkCycles.background = resources.getDrawable(com.nvsp.nvmesapplibrary.R.drawable.button_blue)
            }else{
                btnOkCycles.background = resources.getDrawable(com.nvsp.nvmesapplibrary.R.drawable.button_grey)
            }
        }
    }
    fun setNOKEnabled(state:Boolean){
        with(binding){
            btnNOKCount.isEnabled=state
            if(state){
                btnNOKCount.background = resources.getDrawable(com.nvsp.nvmesapplibrary.R.drawable.button_blue)
            }else{
                btnNOKCount.background = resources.getDrawable(com.nvsp.nvmesapplibrary.R.drawable.button_grey)
            }
        }
    }
    inner class ActiveOperationAdapter(val context: Context?, val dataset:MutableList<ActiveOperation>, val click:(item: ActiveOperation)->Unit, val action:(item: ActiveOperation, mode:Int)->Unit): RecyclerView.Adapter<ActiveOperationAdapter.Holder>() {

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnCreateContextMenuListener {
            val binding = ItemOperationBinding.bind(itemView)
            fun bindItem(item: ActiveOperation) {
                itemView.setOnCreateContextMenuListener(this)
            }

            override fun onCreateContextMenu(
                menu: ContextMenu,
                v: View,
                menuInfo: ContextMenu.ContextMenuInfo?
            ) {
                val currentPos = adapterPosition
                val item = dataset[currentPos]
                /* val edit =
                     menu.add(0, v.id, 0, context?.getString(com.nvsp.nvmesapplibrary.R.string.edit))*/
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
                /*  edit.setOnMenuItemClickListener {
                      Log.d("MENU", "smazani")
                      action(item, UPDATE)
                      false
                  }*/
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemOperationBinding.inflate(LayoutInflater.from(context), parent, false)
            return Holder(binding.root)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = dataset[position]
            holder.bindItem(item)

            with(holder.binding) {
                root.setOnClickListener {
                    selectedOperation=item
                    click(item)
                    notifyDataSetChanged()
                }
                Log.d("BINDING", "item:$item")

                tvPart.text = item.getPart()
                tvOperation.text=item.operationName
                tvPorductOrder.text = item.productOrderCode
                tvOk.text = item.ok.toString()
                tvNok.text = item.nok.toString()
                tvLive.text=item.operationQuantity.toString()
                Log.d("selectOp", "selected OP: ${selectedOperation?.operationId} actual ${item.operationId}")
                if(selectedOperation?.operationId==item.operationId){
                    holder.binding.layOper.setBackgroundColor(resources.getColor(com.nvsp.nvmesapplibrary.R.color.aquariumAlpha40))
                }else
                    holder.binding.layOper.setBackgroundColor(resources.getColor(com.google.android.material.R.color.mtrl_btn_transparent_bg_color))
                //  ivCon.setImageResource()
            }
        }

        override fun getItemCount(): Int {
            return dataset.size
        }

        fun setNewItems(dataSetNew: List<ActiveOperation>) {
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
                    return dataset[oldItemPosition].operationId == dataSetNew[newItemPosition].operationId
                }
            })
            dataset.clear()
            dataset.addAll(dataSetNew)
            diffUtil.dispatchUpdatesTo(this)
        }
    }
    inner class ScreenAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int {
            return 4
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
              //  0 -> return FillOperation()
                1 -> return NokItemsFragment()
                else -> Fragment()
            }
        }


    }
}