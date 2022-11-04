package com.nvsp.manta_terminal.evidence.models

import com.google.gson.annotations.SerializedName

data class Batch (
    @SerializedName("structuredId")
    val structureId:Int,
    @SerializedName("operationId")
    val operationId:Int,
    @SerializedName("wpId")
    val wpId:Int,
    @SerializedName("warehouseSelectionId")
    val warehouseSelectionId:Int,
    @SerializedName("sourceCode")
    val sourceCode:String,
    @SerializedName("sourceName")
    val SourceName:String,
    @SerializedName("barCode")
    val barCode:String,
    @SerializedName("mu")
    val mu:String,
    @SerializedName("quantityStock")
    val quantityStock:Double,
    @SerializedName("quantityRequested")
    val quantityRequested:Double,
    @SerializedName("note")
    val note:String
        ){
    companion object{
        fun getUrl(operationId:Int, barCode: String) = "WorkRecord/WarehouseStock?operationId=$operationId&barcode=$barCode"
    }
}