package com.voice2text;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.*;

public class Voice2TextModule extends ReactContextBaseJavaModule implements RecognitionListener {
    private SpeechRecognizer speechRecognizer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isDestroyed = false;

    public Voice2TextModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "Voice2Text";
    }

    private void initializeRecognizer() {
        if (isDestroyed) return;
        mainHandler.post(() -> {
            if (speechRecognizer == null) {
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getReactApplicationContext());
                    speechRecognizer.setRecognitionListener(this);
                } catch (Exception e) {
                    sendErrorEvent("INIT_ERROR", e.getMessage());
                }
            }
        });
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        if (isDestroyed) return;
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    private void sendErrorEvent(String code, String message) {
        WritableMap map = Arguments.createMap();
        map.putString("code", code);
        map.putString("message", message);
        sendEvent("onSpeechError", map);
    }

    @ReactMethod
    public void checkPermissions(Promise promise) {
        boolean granted = ActivityCompat.checkSelfPermission(
            getReactApplicationContext(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
        promise.resolve(granted);
    }

    @ReactMethod
    public void requestPermissions(Promise promise) {
        if (getCurrentActivity() == null) {
            promise.reject("NO_ACTIVITY", "No current activity");
            return;
        }

        ActivityCompat.requestPermissions(
            getCurrentActivity(),
            new String[]{Manifest.permission.RECORD_AUDIO},
            REQUEST_RECORD_AUDIO_PERMISSION
        );
        promise.resolve(true);
    }

    @ReactMethod
    public void startListening(@Nullable String languageCode, Promise promise) {
        initializeRecognizer();
        mainHandler.post(() -> {
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode != null ? languageCode : Locale.getDefault().getLanguage());
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                speechRecognizer.startListening(intent);
                promise.resolve(true);
            } catch (Exception e) {
                promise.reject("START_FAILED", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void stopListening(Promise promise) {
        mainHandler.post(() -> {
            try {
                speechRecognizer.stopListening();
                promise.resolve(true);
            } catch (Exception e) {
                promise.reject("STOP_FAILED", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void cancelListening(Promise promise) {
        mainHandler.post(() -> {
            try {
                speechRecognizer.cancel();
                promise.resolve(true);
            } catch (Exception e) {
                promise.reject("CANCEL_FAILED", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void destroy(Promise promise) {
        mainHandler.post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                    isDestroyed = true;
                }
                promise.resolve(true);
            } catch (Exception e) {
                promise.reject("DESTROY_FAILED", e.getMessage());
            }
        });
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        sendEvent("onSpeechBegin", null);
    }

    @Override
    public void onBeginningOfSpeech() {
        sendEvent("onSpeechStart", null);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        WritableMap map = Arguments.createMap();
        map.putDouble("rmsdB", rmsdB);
        sendEvent("onSpeechVolumeChanged", map);
    }

    @Override
    public void onEndOfSpeech() {
        WritableMap map = Arguments.createMap();
        map.putBoolean("ended", true);
        sendEvent("onSpeechEnd", map);
    }

    @Override
    public void onError(int error) {
        WritableMap map = Arguments.createMap();
        map.putInt("code", error);
        map.putString("message", getErrorMessage(error));
        sendEvent("onSpeechError", map);
    }

    @Override
    public void onResults(Bundle results) {
        List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            WritableMap map = Arguments.createMap();
            map.putString("text", matches.get(0));
            map.putArray("alternatives", Arguments.fromList(matches));
            sendEvent("onSpeechResults", map);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        List<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (partial != null) {
            WritableMap map = Arguments.createMap();
            map.putString("partialText", partial.get(0));
            map.putArray("alternatives", Arguments.fromList(partial));
            sendEvent("onSpeechPartialResults", map);
        }
    }

    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEvent(int eventType, Bundle params) {}

    private String getErrorMessage(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input";
            default: return "Unknown error";
        }
    }

    @Override
    public void onCatalystInstanceDestroy() {
        destroy(new Promise() {
            @Override public void resolve(Object value) {}
            @Override public void reject(String code, String message) {}
            @Override public void reject(String code, Throwable e) {}
            @Override public void reject(String code, String message, Throwable e) {}
            @Override public void reject(Throwable throwable) {}
            @Override public void reject(Throwable throwable, WritableMap userInfo) {}
            @Override public void reject(String code, @javax.annotation.Nullable WritableMap userInfo) {}
            @Override public void reject(String code, String message, @javax.annotation.Nullable WritableMap userInfo) {}
            @Override public void reject(String code, String message, Throwable throwable, WritableMap userInfo) {}
        });
    }

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 101;
}
