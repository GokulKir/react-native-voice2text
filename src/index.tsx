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
    const subscription = eventEmitter.addListener('onSpeechResults', callback);
    return () => subscription.remove();
  },

  onError: (callback: (error: any) => void): (() => void) => {
    const subscription = eventEmitter.addListener('onSpeechError', callback);
    return () => subscription.remove();
  },
};
