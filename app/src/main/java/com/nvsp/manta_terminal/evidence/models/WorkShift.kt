package com.nvsp.manta_terminal.evidence.models

import com.google.gson.annotations.SerializedName
const val WORKSHIFT="WorkRecord/WorkShift"
data class WorkShift(
    @SerializedName("shiftId")
    val shiftId:Int,
    @SerializedName("shiftCode")
    val shiftCode:String,
    @SerializedName("shiftName")
    val shiftName:String

){
    override fun toString(): String {
        return shiftName
    }
}
