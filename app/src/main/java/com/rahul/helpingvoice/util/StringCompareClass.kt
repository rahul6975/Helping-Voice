package com.rahul.helpingvoice.util

import android.content.Context

class StringCompareClass {
    fun compareAppName(context: Context, resId : Int, desiredStr : String) : Boolean {
        return context.getString(resId) == desiredStr
    }
}