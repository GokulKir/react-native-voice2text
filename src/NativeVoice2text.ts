import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  checkPermissions(): Promise<boolean>;
  startListening(languageCode: string | null): Promise<boolean>;
  stopListening(): void;
  destroy(): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Voice2Text');
