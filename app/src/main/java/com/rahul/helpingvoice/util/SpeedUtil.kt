package com.rahul.helpingvoice.util

object SpeedUtil {
    fun checkSpeed(playerSpeed: Float) : Boolean {
        if(playerSpeed > 2.0 || playerSpeed < 0.75){
            return false
        }
        return true;
    }
}