import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Voice2TextSpec extends TurboModule {
  checkPermissions(): Promise<boolean>;
  requestPermissions(): Promise<boolean>;
  startListening(languageCode: string | null): Promise<boolean>;
  stopListening(): Promise<boolean>;
  cancelListening(): Promise<boolean>;
  destroy(): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Voice2TextSpec>('Voice2Text');
