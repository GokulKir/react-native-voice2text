// src/NativeVoice2textTurboModule.ts

import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

// Native module interface spec for TurboModule
export interface Spec extends TurboModule {
  checkPermissions(): Promise<boolean>;
  startListening(languageCode: string | null): Promise<boolean>;
  stopListening(): Promise<boolean>;
  cancelListening(): Promise<boolean>;
  destroy(): Promise<boolean>;
}

// Export native module instance enforcing the Spec interface
export default TurboModuleRegistry.getEnforcing<Spec>('Voice2Text');
