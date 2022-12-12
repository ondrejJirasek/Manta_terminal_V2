package com.nvsp.manta_terminal.workplaces

import android.graphics.Color
import android.util.Log
import com.google.gson.annotations.SerializedName


data class Workplace(
    @SerializedName("id")
    val id:Int,
    @SerializedName("code")
    val code:String,
    @SerializedName("name")
    val lname:String,
    @SerializedName("state")
    val state:String,
    @SerializedName("color") //todo konvert na HEX RGB
    val color:String?,
    @SerializedName("isRobot")
    val isRobot: Boolean,
    @SerializedName("notificationStatus")
    val notificationStatus:Boolean,
    @SerializedName("teamWorking")
    val teamWorking:Boolean,
    @SerializedName("locationBeforeId")
    val locationBeforeId:Int,
    @SerializedName("locationAfterId")
    val locationAfterId:Int,
    @SerializedName("typeLoginOp")
    val typeLoginOp:Int,
    @SerializedName("selectLoginOp")
    val selectLoginOp:Int,
    @SerializedName("unitId")
    val unitId:Int

){


    fun getColorHex():Int?{
        return if(color==null)
            null
        else {
            if (color.contains("#")) {
                Color.parseColor(color)
            } else {
                val colorInt = color.toInt()
                val r= (colorInt ) and 0xff
                val g = (colorInt shr 8) and 0xff
                val b = (colorInt shr 16) and 0xff
                Color.rgb(r,g,b)
            }
        }
        //   val hexVal = Integer.toHexString(color)
        // return (0xff000000 + Integer.parseInt(hexVal,16)).toInt()
      /*  color?.let { val hexVal = color.toString()
            val r= (color ) and 0xff
            val g = (color shr 8) and 0xff
            val b = (color shr 16) and 0xff
            return android.graphics.Color.rgb(r,g,b)}?: kotlin.run { return null }

  */
       // Log.d(TAG, "HEX: $hexVal R:$r G:$g B:$b")
        //    (0xff000000 + Integer.parseInt(hexVal, 16)).toInt()*/


    }
    companion object{
        fun getUrl(id:Long):String = "Devices/$id/Workplaces"
        fun getUrlForWP(id:Long, wpId:Int):String = "Devices/$id/Workplaces?workplaceId=$wpId"
    }
}
