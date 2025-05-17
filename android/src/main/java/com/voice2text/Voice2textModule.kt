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

class Voice2TextModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isDestroyed = false

    override fun getName(): String = "Voice2Text"

    private fun initializeRecognizer() {
        if (isDestroyed) return
        
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(reactApplicationContext).apply {
                        setRecognitionListener(this@Voice2TextModule)
                    }
                }
            } catch (e: Exception) {
                sendErrorEvent("INIT_ERROR", "Failed to initialize speech recognizer: ${e.message}")
            }
        }
    }

    private fun sendEvent(eventName: String, params: WritableMap?) {
        if (isDestroyed) return
        reactApplicationContext
 .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun sendErrorEvent(code: String, message: String) {
        val map = Arguments.createMap().apply {
            putString("code", code)
            putString("message", message)
        }
        sendEvent("onSpeechError", map)
    }

    @ReactMethod
    fun checkPermissions(promise: Promise) {
        val granted = ActivityCompat.checkSelfPermission(
            reactApplicationContext, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        promise.resolve(granted)
    }

    @ReactMethod
    fun startListening(languageCode: String?, promise: Promise) {
        initializeRecognizer()
        
        mainHandler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode ?: Locale.getDefault().toLanguageTag())
                }
                speechRecognizer?.startListening(intent)
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
                speechRecognizer?.stopListening()
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
                speechRecognizer?.cancel()
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
                speechRecognizer?.destroy()
                speechRecognizer = null
                isDestroyed = true
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
        Arguments.createMap().apply {
            putDouble("rmsdB", rmsdB.toDouble())
            sendEvent("onSpeechVolumeChanged", this)
        }
    }

    override fun onEndOfSpeech() {
        Arguments.createMap().apply {
            putBoolean("ended", true)
            sendEvent("onSpeechEnd", this)
        }
    }

    override fun onError(error: Int) {
        Arguments.createMap().apply {
            putInt("code", error)
            putString("message", getErrorMessage(error))
            sendEvent("onSpeechError", this)
        }
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
            Arguments.createMap().apply {
                putString("text", matches.firstOrNull() ?: "")
                putArray("alternatives", Arguments.fromList(matches))
                sendEvent("onSpeechResults", this)
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { partial ->
            Arguments.createMap().apply {
                putString("partialText", partial.firstOrNull() ?: "")
                putArray("alternatives", Arguments.fromList(partial))
                sendEvent("onSpeechPartialResults", this)
            }
        }
    }

    // Other unused callbacks
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun getErrorMessage(errorCode: Int): String = when (errorCode) {
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

    override fun onCatalystInstanceDestroy() {
        destroy(Promise { _, _ -> })
    }
}