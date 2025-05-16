import { Text, View, StyleSheet } from 'react-native';
// import { multiply } from 'react-native-voice2text'; ❌ Not available

export default function App() {
  return (
    <View style={styles.container}>
      <Text>Voice2Text Example App</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
