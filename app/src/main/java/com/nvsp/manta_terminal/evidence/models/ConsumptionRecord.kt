package com.nvsp.manta_terminal.evidence.models

import com.google.gson.annotations.SerializedName

data class ConsumptionRecord(
    @SerializedName("id"                   ) var id                   : Int,
    @SerializedName("terminalId"           ) var terminalId           : Int,
    @SerializedName("employeeId"           ) var employeeId           : Int,
    @SerializedName("productOrderId"       ) var productOrderId       : Int,
    @SerializedName("operationId"          ) var operationId          : Int,
    @SerializedName("wpId"                 ) var wpId                 : Int,
    @SerializedName("warehouseSelectionId" ) var warehouseSelectionId : Int,
    @SerializedName("structureId"          ) var structureId          : Int,
    @SerializedName("sourceCode"           ) var sourceCode           : String,
    @SerializedName("sourceName"           ) var sourceName           : String,
    @SerializedName("barCode"              ) var barCode              : String,
    @SerializedName("mu"                   ) var mu                   : String,
    @SerializedName("errorMsg"             ) var errorMsg             : String? = null,
    @SerializedName("location"             ) var location             : String,
    @SerializedName("quantityStock"        ) var quantityStock        : Double,
    @SerializedName("quantity"             ) var quantity             : Double,
    @SerializedName("quantityRequested"    ) var quantityRequested    : Double,
    @SerializedName("variationId"          ) var variationId          : String? = null
){
    companion object{
        fun getURL(operationId: Int,wpId: Int)="WorkRecord/MaterialConsumption?workplaceId=$wpId&operationId=$operationId"
        fun getLockUrl(mat:Int, state:Boolean) = "WorkRecord/MaterialConsumption/$mat/Lock?lockState=$state"
        fun getDeleteUrl(mat:Int) = "WorkRecord/MaterialConsumption/$mat"
        fun isSame(old:ConsumptionRecord, new:ConsumptionRecord):Boolean{
          return   old.id==new.id &&
                  old.terminalId==new.terminalId &&
                  old.employeeId==new.employeeId &&
                  old.productOrderId==new.productOrderId &&
                  old.operationId==new.operationId &&
                  old.wpId==new.wpId &&
                  old.warehouseSelectionId==new.warehouseSelectionId &&
                  old.structureId==new.structureId &&
                  old.sourceCode==new.sourceCode &&
                  old.sourceName==new.sourceName &&
                  old.barCode==new.barCode &&
                  old.mu==new.mu &&
                  old.errorMsg==new.errorMsg &&
                  old.location==new.location &&
                  old.quantityStock==new.quantityStock &&
                  old.quantity==new.quantity &&
                  old.quantityRequested==new.quantityRequested &&
                  old.variationId==new.variationId


        }
    }
}