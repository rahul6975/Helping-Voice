package com.rahul.helpingvoice.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rahul.helpingvoice.R
import com.rahul.helpingvoice.helper.PreferenceHelper
import kotlinx.android.synthetic.main.activity_welcome.*
import java.util.*
import kotlin.collections.HashMap

class WelcomeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val RQ_SPEECH = 102
    private var textToSpeech: TextToSpeech? = null
    private var textToSpeech2: TextToSpeech? = null

    private var name = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        PreferenceHelper.getSharedPreferences(this)
        textToSpeech = TextToSpeech(this, this)
        textToSpeech2 = TextToSpeech(this, this)

    }

    private fun askName() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech not available", Toast.LENGTH_SHORT).show()
        } else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "say something")
            startActivityForResult(i, RQ_SPEECH)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RQ_SPEECH && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            name = result?.get(0).toString()
            welcomeMessage()
        }
    }


    private fun welcomeMessage() {
        PreferenceHelper.writeStringToPreference("name", name)
        PreferenceHelper.writeBooleanToPreference("loginCheck", true)
        startActivity(Intent(this, PodcastActivity::class.java))
        finish()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech!!.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            } else {
                speck()
            }
        }
    }

    private fun speck() {
        val text =
            "Welcome!, you can consider me as your personal assistant which will help you find books, podcasts, " +
                    "even help you in an emergency, lets get started with your name, what would you like to call me ?"
        val hashMap = HashMap<String, String>()
        hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
        textToSpeech!!.speak(text, TextToSpeech.QUEUE_FLUSH, hashMap)
        textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String?) {
                askName()
            }

            override fun onError(utteranceId: String?) {
            }

        })

    }


    override fun onDestroy() {
        if (textToSpeech != null) {
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }
        super.onDestroy()
    }

}