package com.nvsp.manta_terminal.evidence.models

import com.google.gson.annotations.SerializedName

data class NokItems(
    @SerializedName("id")
    val id:Int,
    @SerializedName("defectCode")
    val defectCode:String,
    @SerializedName("defectName")
    val defectName:String,
    @SerializedName("defectType")
    val defectType:String,
    @SerializedName("defectNote")
    val defectNote:String,
    @SerializedName("quantity")
    val quantity:Double?,
    @SerializedName("Rraster")
    val raster:String,
    @SerializedName("employeeId")
    val employeeId:Int?,
    @SerializedName("workplaceId")
    val workplaceId:Int?,
    @SerializedName("teamWorking")
    val teamWorking:Boolean?,//todo fakt nesmysl aby to bylo null
    @SerializedName("operationId")
    val operationId:Int?
){
    fun getDefect():String{
        return "$defectCode - $defectName"
    }
    companion object{
        fun getUrl(team:Boolean, operationId: Int, emplId:Int, wpId:Int) = "Operations/Nok?isTeamWorking=$team&operationId=$operationId&employeeId=$emplId&workplaceId=$wpId"
    }
}
