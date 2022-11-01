package com.nvsp.manta_terminal.evidence.models

import com.google.gson.annotations.SerializedName

data class ActiveOperation(
    @SerializedName("operationId")
    val operationId:Int,
    @SerializedName("productCode")
    val productCode:String,
    @SerializedName("productName")
    val productName:String,
    @SerializedName("productOrderCode")
    val productOrderCode:String,
    @SerializedName("operationName")
    val operationName:String,
    @SerializedName("operationQuantity")
    val operationQuantity:Double?,
    @SerializedName("outDataId")
    val outDataId:Int?,
    @SerializedName("ok")
    val ok:Double,
    @SerializedName("nok")
    val nok:Double,
    @SerializedName("labelNumber")
    val labelNumber:Int,
    @SerializedName("labelQuantily")
    val labelQuantily:Int,
    @SerializedName("productOrderId")
    val productOrderId:Int,
    @SerializedName("operationType")
    val operationType:String,
    @SerializedName("todayNok")
    val todayNok:Int,
    @SerializedName("employeeId")
    val employeeId:Int?,
    @SerializedName("workplaceId")
    val workplaceId:Int?,
    @SerializedName("teamWorking")
    val teamWorking:Boolean?,
){
    companion object{
        fun getUrl(team:Boolean, wpId: Int, emplId:Int):String{
            return "Operations/Active?isTeamWorking=$team&workplaceId=$wpId&employeeId=$emplId"
        }
    }

    fun getPart():String= "$productCode-$productName"

}
