<<<<<<< HEAD
import {
  NativeModules,
  NativeEventEmitter,
  PermissionsAndroid,
  Platform,
} from 'react-native';

const { Voice2Text } = NativeModules;
const eventEmitter = new NativeEventEmitter(Voice2Text);

export interface SpeechResultsEvent {
  text: string;
  alternatives: string[];
}

export interface SpeechErrorEvent {
  message: string;
  code: number;
}

export interface SpeechEndEvent {
  ended: boolean;
}

export interface VolumeChangedEvent {
  rmsdB: number;
}

export type SpeechResultsCallback = (results: SpeechResultsEvent) => void;
export type SpeechErrorCallback = (error: SpeechErrorEvent) => void;
export type SpeechEndCallback = (event: SpeechEndEvent) => void;
export type VolumeChangedCallback = (event: VolumeChangedEvent) => void;
export type SimpleEventCallback = () => void;

const Voice2TextModule = {
  async checkPermissions(): Promise<boolean> {
    if (Platform.OS === 'android') {
      return PermissionsAndroid.check(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
      );
    }
    return false;
  },

  async requestPermissions(): Promise<boolean> {
    if (Platform.OS === 'android') {
      const result = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
      );
      return result === PermissionsAndroid.RESULTS.GRANTED;
    }
    return false;
  },

  async startListening(languageCode?: string): Promise<boolean> {
    const hasPermission = await this.checkPermissions();
    if (!hasPermission) {
      const granted = await this.requestPermissions();
      if (!granted) throw new Error('Permission not granted');
    }
    return await Voice2Text.startListening(languageCode || null);
  },

  stopListening(): Promise<boolean> {
    return Voice2Text.stopListening();
  },

  cancelListening(): Promise<boolean> {
    return Voice2Text.cancelListening();
  },

  destroy(): Promise<boolean> {
    return Voice2Text.destroy();
  },

  onResults(callback: SpeechResultsCallback): () => void {
=======
import { NativeModules, NativeEventEmitter } from 'react-native';

const { Voice2Text } = NativeModules;

const eventEmitter = new NativeEventEmitter(Voice2Text);

export default {
  checkPermissions: (): Promise<boolean> => Voice2Text.checkPermissions(),

  startListening: (languageCode: string): void =>
    Voice2Text.startListening(languageCode),

  stopListening: (): void => Voice2Text.stopListening(),

  destroy: (): void => Voice2Text.destroy(),

  onResults: (callback: (results: any) => void): (() => void) => {
>>>>>>> 653118c (chore: initial commit)
    const subscription = eventEmitter.addListener('onSpeechResults', callback);
    return () => subscription.remove();
  },

<<<<<<< HEAD
  onPartialResults(callback: SpeechResultsCallback): () => void {
    const subscription = eventEmitter.addListener(
      'onSpeechPartialResults',
      callback
    );
    return () => subscription.remove();
  },

  onError(callback: SpeechErrorCallback): () => void {
    const subscription = eventEmitter.addListener('onSpeechError', callback);
    return () => subscription.remove();
  },

  onSpeechStart(callback: SimpleEventCallback): () => void {
    const subscription = eventEmitter.addListener('onSpeechStart', callback);
    return () => subscription.remove();
  },

  onSpeechBegin(callback: SimpleEventCallback): () => void {
    const subscription = eventEmitter.addListener('onSpeechBegin', callback);
    return () => subscription.remove();
  },

  onSpeechEnd(callback: SpeechEndCallback): () => void {
    const subscription = eventEmitter.addListener('onSpeechEnd', callback);
    return () => subscription.remove();
  },

  onVolumeChanged(callback: VolumeChangedCallback): () => void {
    const subscription = eventEmitter.addListener(
      'onSpeechVolumeChanged',
      callback
    );
    return () => subscription.remove();
  },
};

export default Voice2TextModule;
=======
  onError: (callback: (error: any) => void): (() => void) => {
    const subscription = eventEmitter.addListener('onSpeechError', callback);
    return () => subscription.remove();
  },
};
>>>>>>> 653118c (chore: initial commit)
