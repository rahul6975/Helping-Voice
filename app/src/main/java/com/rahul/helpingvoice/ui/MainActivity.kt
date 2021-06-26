package com.rahul.helpingvoice.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.rahul.helpingvoice.R
import com.rahul.helpingvoice.helper.PreferenceHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PreferenceHelper.getSharedPreferences(this)

        if (PreferenceHelper.getBooleanFromPreference("loginCheck")) {
            val intent = Intent(this, PodcastActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()

        }
    }
}