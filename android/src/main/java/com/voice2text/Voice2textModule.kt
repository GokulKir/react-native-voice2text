package com.voice2text

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class Voice2TextModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(reactContext)

    init {
        speechRecognizer?.setRecognitionListener(this)
    }

    override fun getName(): String = "Voice2Text"

    @ReactMethod
    fun checkPermissions(promise: Promise) {
        val permission = ContextCompat.checkSelfPermission(
            reactContext, Manifest.permission.RECORD_AUDIO
        )
        promise.resolve(permission == PackageManager.PERMISSION_GRANTED)
    }

    @ReactMethod
    fun startListening(languageCode: String?, promise: Promise) {
        if (ContextCompat.checkSelfPermission(reactContext, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            promise.reject("PERMISSION_DENIED", "Record audio permission is required")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                languageCode?.takeIf { it.isNotEmpty() } ?: java.util.Locale.getDefault().toString()
            )
        }
        try {
            speechRecognizer?.startListening(intent)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("START_LISTENING_ERROR", e.message)
        }
    }

    @ReactMethod
    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    @ReactMethod
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val writableMap = Arguments.createMap().apply {
            putString("text", matches?.firstOrNull() ?: "")
            putArray("alternatives", Arguments.fromList(matches ?: emptyList()))
        }
        sendEvent("onSpeechResults", writableMap)
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            else -> "Unknown error: $error"
        }
        sendEvent("onSpeechError", Arguments.createMap().apply {
            putString("message", errorMessage)
            putInt("code", error)
        })
    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
