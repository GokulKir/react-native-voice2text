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
    // iOS permissions handled by native side or not required here
    return true;
  },

  async requestPermissions(): Promise<boolean> {
    if (Platform.OS === 'android') {
      const result = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
      );
      return result === PermissionsAndroid.RESULTS.GRANTED;
    }
    return true;
  },

  async startListening(languageCode?: string | null): Promise<boolean> {
    const hasPermission = await this.checkPermissions();
    if (!hasPermission) {
      const granted = await this.requestPermissions();
      if (!granted) throw new Error('Permission not granted');
    }
    return Voice2Text.startListening(languageCode ?? null);
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
    const subscription = eventEmitter.addListener('onSpeechResults', callback);
    return () => subscription.remove();
  },

  onPartialResults(callback: SpeechResultsCallback): () => void {
    const subscription = eventEmitter.addListener('onSpeechPartialResults', callback);
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
    const subscription = eventEmitter.addListener('onSpeechVolumeChanged', callback);
    return () => subscription.remove();
  },
};

export default Voice2TextModule;
