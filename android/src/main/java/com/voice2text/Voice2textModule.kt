package com.voice2text

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.util.*

class Voice2TextModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RecognitionListener {

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(reactContext)
    private val reactContext: ReactApplicationContext = reactContext
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        speechRecognizer.setRecognitionListener(this)
    }

    override fun getName(): String = "Voice2Text"

    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    @ReactMethod
    fun checkPermissions(promise: Promise) {
        val granted = ActivityCompat.checkSelfPermission(
            reactContext, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        promise.resolve(granted)
    }

    @ReactMethod
    fun startListening(languageCode: String?, promise: Promise) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            val lang =
                if (languageCode.isNullOrEmpty()) Locale.getDefault().toLanguageTag() else languageCode
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
        }

        mainHandler.post {
            try {
                speechRecognizer.startListening(intent)
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("START_FAILED", e.message)
            }
        }
    }

    @ReactMethod
    fun stopListening(promise: Promise) {
        mainHandler.post {
            try {
                speechRecognizer.stopListening()
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("STOP_FAILED", e.message)
            }
        }
    }

    @ReactMethod
    fun cancelListening(promise: Promise) {
        mainHandler.post {
            try {
                speechRecognizer.cancel()
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("CANCEL_FAILED", e.message)
            }
        }
    }

    @ReactMethod
    fun destroy(promise: Promise) {
        mainHandler.post {
            try {
                speechRecognizer.destroy()
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("DESTROY_FAILED", e.message)
            }
        }
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        sendEvent("onSpeechBegin", null)
    }

    override fun onBeginningOfSpeech() {
        sendEvent("onSpeechStart", null)
    }

    override fun onRmsChanged(rmsdB: Float) {
        val map = Arguments.createMap()
        map.putDouble("rmsdB", rmsdB.toDouble())
        sendEvent("onSpeechVolumeChanged", map)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        val map = Arguments.createMap()
        map.putBoolean("ended", true)
        sendEvent("onSpeechEnd", map)
    }

    override fun onError(error: Int) {
        val map = Arguments.createMap()
        map.putInt("code", error)
        map.putString("message", getErrorMessage(error))
        sendEvent("onSpeechError", map)
    }

    override fun onResults(results: Bundle?) {
        val matches: ArrayList<String>? =
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val map = Arguments.createMap()
        map.putString("text", matches?.getOrNull(0) ?: "")
        map.putArray("alternatives", Arguments.fromList(matches ?: emptyList<String>()))
        sendEvent("onSpeechResults", map)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partial: ArrayList<String>? =
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val map = Arguments.createMap()
        map.putString("partialText", partial?.getOrNull(0) ?: "")
        map.putArray("alternatives", Arguments.fromList(partial ?: emptyList<String>()))
        sendEvent("onSpeechPartialResults", map)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }
}
