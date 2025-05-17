import {
  NativeModules,
  NativeEventEmitter,
  PermissionsAndroid,
  Platform,
} from 'react-native';

const { Voice2Text } = NativeModules;
const eventEmitter = new NativeEventEmitter(Voice2Text);

// Strongly typed event interfaces
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

// Event callback types
export type SpeechResultsCallback = (results: SpeechResultsEvent) => void;
export type SpeechErrorCallback = (error: SpeechErrorEvent) => void;
export type SpeechEndCallback = (event: SpeechEndEvent) => void;
export type VolumeChangedCallback = (event: VolumeChangedEvent) => void;
export type SimpleEventCallback = () => void;

// Main module interface
export interface Voice2TextModuleInterface {
  // Permission methods
  checkPermissions(): Promise<boolean>;
  requestPermissions(): Promise<boolean>;

  // Control methods
  startListening(languageCode?: string): Promise<boolean>;
  stopListening(): Promise<boolean>;
  cancelListening(): Promise<boolean>;
  destroy(): Promise<boolean>;

  // Event subscriptions
  onResults(callback: SpeechResultsCallback): () => void;
  onPartialResults(callback: SpeechResultsCallback): () => void;
  onError(callback: SpeechErrorCallback): () => void;
  onSpeechStart(callback: SimpleEventCallback): () => void;
  onSpeechBegin(callback: SimpleEventCallback): () => void;
  onSpeechEnd(callback: SpeechEndCallback): () => void;
  onVolumeChanged(callback: VolumeChangedCallback): () => void;
}

const Voice2TextModule: Voice2TextModuleInterface = {
  async checkPermissions(): Promise<boolean> {
    try {
      if (Platform.OS === 'android') {
        const hasPermission = await PermissionsAndroid.check(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
        );
        return hasPermission;
      } else if (Platform.OS === 'ios') {
        console.warn('iOS permission checking not implemented yet');
        // TODO: Implement iOS permission check (e.g., using react-native-permissions)
        return false;
      }
      return false;
    } catch (error) {
      console.error('Error checking permissions:', error);
      return false;
    }
  },

  async requestPermissions(): Promise<boolean> {
    try {
      if (Platform.OS === 'android') {
        const result = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
        );
        return result === PermissionsAndroid.RESULTS.GRANTED;
      } else if (Platform.OS === 'ios') {
        console.warn('iOS permission request not implemented yet');
        // TODO: Implement iOS permission request (e.g., using react-native-permissions)
        return false;
      }
      return false;
    } catch (error) {
      console.error('Error requesting permissions:', error);
      return false;
    }
  },

  async startListening(languageCode?: string): Promise<boolean> {
    try {
      const hasPermission = await this.checkPermissions();
      if (!hasPermission) {
        const granted = await this.requestPermissions();
        if (!granted) {
          throw new Error('Audio recording permission not granted');
        }
      }
      return await Voice2Text.startListening(languageCode || null);
    } catch (error) {
      console.error('Error starting listening:', error);
      throw error;
    }
  },

  async stopListening(): Promise<boolean> {
    try {
      return await Voice2Text.stopListening();
    } catch (error) {
      console.error('Error stopping listening:', error);
      throw error;
    }
  },

  async cancelListening(): Promise<boolean> {
    try {
      return await Voice2Text.cancelListening();
    } catch (error) {
      console.error('Error canceling listening:', error);
      throw error;
    }
  },

  async destroy(): Promise<boolean> {
    try {
      return await Voice2Text.destroy();
    } catch (error) {
      console.error('Error destroying module:', error);
      throw error;
    }
  },

  onResults(callback: SpeechResultsCallback): () => void {
    const subscription = eventEmitter.addListener('onSpeechResults', callback);
    return () => subscription.remove();
  },

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
