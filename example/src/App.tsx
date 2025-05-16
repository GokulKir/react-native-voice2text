import { useEffect, useState } from 'react';
import {
  Text,
  View,
  StyleSheet,
  Button,
  Alert,
  NativeModules,
  NativeEventEmitter,
  ScrollView,
} from 'react-native';

const { Voice2Text } = NativeModules;
const eventEmitter = new NativeEventEmitter(Voice2Text);

interface SpeechResultsEvent {
  text: string;
  alternatives?: string[];
}

interface SpeechErrorEvent {
  message: string;
  code?: number;
}

export default function App() {
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [alternatives, setAlternatives] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Subscribe to native events
    const resultsSubscription = eventEmitter.addListener(
      'onSpeechResults',
      (event: SpeechResultsEvent) => {
        setTranscript(event.text);
        setAlternatives(event.alternatives ?? []);
        setError(null);
        setIsListening(false);
      }
    );

    const errorSubscription = eventEmitter.addListener(
      'onSpeechError',
      (event: SpeechErrorEvent) => {
        setError(event.message);
        setIsListening(false);
      }
    );

    return () => {
      resultsSubscription.remove();
      errorSubscription.remove();
    };
  }, []);

  const checkAndStartListening = async () => {
    try {
      const hasPermission: boolean = await Voice2Text.checkPermissions();
      if (!hasPermission) {
        Alert.alert(
          'Permission required',
          'Please enable microphone permission in settings.'
        );
        return;
      }
      setError(null);
      setTranscript('');
      setAlternatives([]);
      setIsListening(true);
      await Voice2Text.startListening('en-US');
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to start listening');
      setIsListening(false);
    }
  };

  const stopListening = () => {
    Voice2Text.stopListening();
    setIsListening(false);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Voice2Text Example App</Text>

      <Button
        title={isListening ? 'Listening... Tap to Stop' : 'Start Listening'}
        onPress={isListening ? stopListening : checkAndStartListening}
      />

      {error && <Text style={styles.errorText}>Error: {error}</Text>}

      <ScrollView style={styles.resultsContainer}>
        <Text style={styles.transcriptLabel}>Recognized Text:</Text>
        <Text style={styles.transcriptText}>{transcript || '...'}</Text>

        {alternatives.length > 1 && (
          <>
            <Text style={styles.alternativesLabel}>Alternatives:</Text>
            {alternatives.map((alt, idx) => (
              <Text key={idx} style={styles.alternativeText}>
                {alt}
              </Text>
            ))}
          </>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    paddingTop: 50,
  },
  title: {
    fontSize: 22,
    fontWeight: '600',
    marginBottom: 20,
    textAlign: 'center',
  },
  resultsContainer: {
    marginTop: 30,
  },
  transcriptLabel: {
    fontWeight: 'bold',
    fontSize: 16,
  },
  transcriptText: {
    marginTop: 10,
    fontSize: 18,
    color: '#333',
  },
  alternativesLabel: {
    marginTop: 20,
    fontWeight: 'bold',
    fontSize: 16,
  },
  alternativeText: {
    fontSize: 16,
    color: '#555',
    marginLeft: 10,
  },
  errorText: {
    marginTop: 15,
    color: 'red',
    fontWeight: 'bold',
    textAlign: 'center',
  },
});
