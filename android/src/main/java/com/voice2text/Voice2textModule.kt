package com.voice2text

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.turbomodule.core.interfaces.TurboModule
import java.util.*

class Voice2TextModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), TurboModule, RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerAvailable: Boolean = false
    private var isListening: Boolean = false

    init {
        initializeSpeechRecognizer()
    }

    private fun initializeSpeechRecognizer() {
        try {
            isRecognizerAvailable = SpeechRecognizer.isRecognitionAvailable(reactContext)
            Log.d(TAG, "Speech recognition available: $isRecognizerAvailable")
            if (isRecognizerAvailable) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(reactContext).apply {
                    setRecognitionListener(this@Voice2TextModule)
                }
            } else {
                sendEvent("onSpeechError", Arguments.createMap().apply {
                    putString("message", "Speech recognition is not available on this device")
                    putInt("code", -100)
                })
            }
        } catch (e: Exception) {
            isRecognizerAvailable = false
            Log.e(TAG, "Failed to initialize speech recognizer", e)
            sendEvent("onSpeechError", Arguments.createMap().apply {
                putString("message", "Failed to initialize speech recognizer: ${e.message}")
                putInt("code", -101)
            })
        }
    }

    override fun getName(): String = "Voice2Text"

    @ReactMethod
    fun checkPermissions(promise: Promise) {
        val granted = ContextCompat.checkSelfPermission(
            reactContext, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Check permissions: $granted")
        promise.resolve(granted)
    }

    @ReactMethod
    fun requestPermissions(promise: Promise) {
        val currentActivity = currentActivity
        if (currentActivity == null) {
            Log.e(TAG, "No current activity available")
            promise.reject("NO_ACTIVITY", "No current activity available")
            return
        }
        try {
            val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
            currentActivity.requestPermissions(permissions, REQUEST_PERMISSION_CODE)
            Log.d(TAG, "Requested RECORD_AUDIO permission")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request permissions", e)
            promise.reject("PERMISSION_REQUEST_ERROR", "Failed to request permissions: ${e.message}")
        }
    }

    @ReactMethod
    fun startListening(languageCode: String?, promise: Promise) {
        if (!isRecognizerAvailable || speechRecognizer == null) {
            Log.e(TAG, "Recognizer not available")
            promise.reject("RECOGNIZER_NOT_AVAILABLE", "Speech recognition is not available")
            return
        }

        if (isListening) {
            Log.e(TAG, "Already listening")
            promise.reject("ALREADY_LISTENING", "Speech recognizer is already listening")
            return
        }

        if (ContextCompat.checkSelfPermission(
                reactContext, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Record audio permission denied")
            promise.reject("PERMISSION_DENIED", "Record audio permission required")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                languageCode?.takeIf { it.isNotEmpty() } ?: Locale.getDefault().toLanguageTag()
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d(TAG, "Started listening with language: ${languageCode ?: Locale.getDefault().toLanguageTag()}")
            promise.resolve(true)
        } catch (e: Exception) {
            isListening = false
            Log.e(TAG, "Failed to start listening", e)
            promise.reject("START_LISTENING_ERROR", "Failed to start listening: ${e.message}")
        }
    }

    @ReactMethod
    fun stopListening(promise: Promise) {
        if (!isListening) {
            Log.d(TAG, "Not listening, stopping anyway")
            promise.resolve(true)
            return
        }
        try {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d(TAG, "Stopped listening")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening", e)
            promise.reject("STOP_LISTENING_ERROR", "Failed to stop listening: ${e.message}")
        }
    }

    @ReactMethod
    fun cancelListening(promise: Promise) {
        if (!isListening) {
            Log.d(TAG, "Not listening, canceling anyway")
            promise.resolve(true)
            return
        }
        try {
            speechRecognizer?.cancel()
            isListening = false
            Log.d(TAG, "Canceled listening")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel listening", e)
            promise.reject("CANCEL_LISTENING_ERROR", "Failed to cancel listening: ${e.message}")
        }
    }

    @ReactMethod
    fun destroy(promise: Promise) {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isRecognizerAvailable = false
            isListening = false
            Log.d(TAG, "Destroyed speech recognizer")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy recognizer", e)
            promise.reject("DESTROY_ERROR", "Failed to destroy recognizer: ${e.message}")
        }
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val map = Arguments.createMap().apply {
            putString("text", matches?.firstOrNull() ?: "")
            val alternatives = Arguments.createArray().apply {
                matches?.forEach { pushString(it) }
            }
            putArray("alternatives", alternatives)
        }
        Log.d(TAG, "Speech results: ${map.toString()}")
        sendEvent("onSpeechResults", map)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val map = Arguments.createMap().apply {
            putString("partialText", partialMatches?.firstOrNull() ?: "")
            val alternatives = Arguments.createArray().apply {
                partialMatches?.forEach { pushString(it) }
            }
            putArray("alternatives", alternatives)
        }
        Log.d(TAG, "Partial results: ${map.toString()}")
        sendEvent("onSpeechPartialResults", map)
    }

    override fun onError(error: Int) {
        isListening = false
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout"
            else -> "Unknown error: $error"
        }

        val errorMap = Arguments.createMap().apply {
            putString("message", errorMessage)
            putInt("code", error)
        }
        Log.e(TAG, "Speech error: $errorMessage (code: $error)")
        sendEvent("onSpeechError", errorMap)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
        sendEvent("onSpeechStart", Arguments.createMap())
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech")
        sendEvent("onSpeechBegin", Arguments.createMap())
    }

    override fun onRmsChanged(rmsdB: Float) {
        val map = Arguments.createMap().apply {
            putDouble("rmsdB", rmsdB.toDouble())
        }
        Log.d(TAG, "Volume changed: $rmsdB")
        sendEvent("onSpeechVolumeChanged", map)
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        Log.d(TAG, "Buffer received")
    }

    override fun onEndOfSpeech() {
        isListening = false
        Log.d(TAG, "End of speech")
        sendEvent("onSpeechEnd", Arguments.createMap().apply {
            putBoolean("ended", true)
        })
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "Event received: $eventType")
    }

    override fun onCatalystInstanceDestroy() {
        Log.d(TAG, "Catalyst instance destroyed")
        destroy(object : Promise {
            override fun resolve(value: Any?) {}
            override fun reject(code: String?, message: String?) {}
            override fun reject(code: String?, message: String?, error: Throwable?) {}
        })
        super.onCatalystInstanceDestroy()
    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                ?.emit(eventName, params)
        } else {
            Log.w(TAG, "Cannot emit $eventName: No active Catalyst instance")
        }
    }

    override fun invalidate() {
        Log.d(TAG, "Invalidating module")
        destroy(object : Promise {
            override fun resolve(value: Any?) {}
            override fun reject(code: String?, message: String?) {}
            override fun reject(code: String?, message: String?, error: Throwable?) {}
        })
    }

    companion object {
        private const val TAG = "Voice2TextModule"
        const val REQUEST_PERMISSION_CODE = 1001
    }
}