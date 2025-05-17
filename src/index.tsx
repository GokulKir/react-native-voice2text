// src/NativeVoice2text.ts

import { NativeModules, NativeEventEmitter } from 'react-native';
import type { EmitterSubscription } from 'react-native';

// Native module from React Native's NativeModules
const { Voice2Text } = NativeModules;

// Event emitter for native events
const eventEmitter = new NativeEventEmitter(Voice2Text);

// Types for the events emitted by the native module
interface Voice2TextEventTypes {
  onSpeechResults: { text: string; alternatives: string[] };
  onSpeechPartialResults: { partialText: string; alternatives: string[] };
  onSpeechError: { message: string; code: number };
  onSpeechStart: Record<string, never>;
  onSpeechBegin: Record<string, never>;
  onSpeechEnd: { ended: boolean };
  onSpeechVolumeChanged: { rmsdB: number };
}

// Interface for JS wrapper methods for Voice2Text native module
interface Voice2TextInterface {
  checkPermissions: () => Promise<boolean>;
  requestPermissions: () => Promise<boolean>;
  startListening: (languageCode?: string) => Promise<boolean>;
  stopListening: () => Promise<boolean>;
  cancelListening: () => Promise<boolean>;
  destroy: () => Promise<boolean>;

  onResults: (
    callback: (results: Voice2TextEventTypes['onSpeechResults']) => void
  ) => () => void;

  onPartialResults: (
    callback: (results: Voice2TextEventTypes['onSpeechPartialResults']) => void
  ) => () => void;

  onError: (
    callback: (error: Voice2TextEventTypes['onSpeechError']) => void
  ) => () => void;

  onSpeechStart: (callback: () => void) => () => void;
  onSpeechBegin: (callback: () => void) => () => void;

  onSpeechEnd: (
    callback: (event: Voice2TextEventTypes['onSpeechEnd']) => void
  ) => () => void;

  onVolumeChanged: (
    callback: (event: Voice2TextEventTypes['onSpeechVolumeChanged']) => void
  ) => () => void;
}

// JS wrapper implementation calling native module and exposing event subscriptions
const Voice2TextModule: Voice2TextInterface = {
  checkPermissions: () => Voice2Text.checkPermissions(),

  // Permissions usually handled externally, so just return true here
  requestPermissions: async () => true,

  startListening: (languageCode?: string) =>
    Voice2Text.startListening(languageCode || null),

  stopListening: () => Voice2Text.stopListening(),
  cancelListening: () => Voice2Text.cancelListening(),
  destroy: () => Voice2Text.destroy(),

  onResults: (callback) => {
    const subscription: EmitterSubscription = eventEmitter.addListener(
      'onSpeechResults',
      callback
    );
    return () => subscription.remove();
  },

  onPartialResults: (callback) => {
    const subscription: EmitterSubscription = eventEmitter.addListener(
      'onSpeechPartialResults',
      callback
    );
    return () => subscription.remove();
  },

  onError: (callback) => {
    const subscription: EmitterSubscription = eventEmitter.addListener(
      'onSpeechError',
      callback
    );
    return () => subscription.remove();
  },

  onSpeechStart: (callback) => {
    const subscription: EmitterSubscription = eventEmitter.addListener(
      'onSpeechStart',
      callback
    );
    return () => subscription.remove();
  },

  onSpeechBegin: (callback) => {
    const subscription: EmitterSubscription = eventEmitter.addListener(
      'onSpeechBegin',
      callback
    );
    return () => subscription.remove();
  },

  onSpeechEnd: (callback) => {
    const subscription: EmitterSubscription = eventEmitter.addListener(
      'onSpeechEnd',
      callback
    );
    return () => subscription.remove();
  },

  onVolumeChanged: (callback) => {
    const subscription: EmitterSubscription = eventEmitter.addListener(
      'onSpeechVolumeChanged',
      callback
    );
    return () => subscription.remove();
  },
};

export default Voice2TextModule;
