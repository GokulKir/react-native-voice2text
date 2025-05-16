# 🎙️ react-native-voice2text

**react-native-voice2text** is a lightweight, modern, and easy-to-use React Native native module for **Android** that transforms spoken words into text using the device's built-in speech recognition services. Perfect for adding voice commands, accessibility features, or hands-free inputs to your app with a clean and intuitive API.

✅ **Compatible with React Native 0.70+**  
🚧 **iOS support in development**

![React Native Speech to Text](https://blog.logrocket.com/wp-content/uploads/2022/12/build-a-react-native-speech-to-text-dictation-app-nocdn.png)  

---

## 🌟 Features

- 🎤 **Seamless Voice-to-Text**: Leverage native Android speech recognition APIs.
- 🔄 **Real-Time Results**: Stream recognition results as users speak.
- 🚫 **Robust Error Handling**: Gracefully handle errors and edge cases.
- 🔐 **Permission Management**: Built-in checks for microphone access.

---

## 📦 Installation

### 1. Install the Package

```bash
npm install react-native-voice2text
# or
yarn add react-native-voice2text
```

### 2. Native Android Setup

For **React Native >= 0.60**, autolinking handles the setup automatically. For older versions or if autolinking fails, follow the manual linking steps below.

#### Manual Linking

##### a) Update `android/settings.gradle`
Add the following to include the module:

```gradle
include ':react-native-voice2text'
project(':react-native-voice2text').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-voice2text/android')
```

##### b) Update `android/app/build.gradle`
Add the module as a dependency:

```gradle
dependencies {
    implementation project(':react-native-voice2text')
}
```

##### c) Update `MainApplication.java`
Open `android/app/src/main/java/<your-package>/MainApplication.java` and add the `Voice2TextPackage`:

```java
import com.voice2text.Voice2TextPackage;

@Override
protected List<ReactPackage> getPackages() {
  return Arrays.<ReactPackage>asList(
    new MainReactPackage(),
    new Voice2TextPackage() // Add this
  );
}
```

### 3. Add Permissions

Ensure the following permissions are included in `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 📱 Usage Example

Here's a quick example to get you started with voice recognition:

```javascript
import Voice2Text from 'react-native-voice2text';

async function startRecognition() {
  try {
    const granted = await Voice2Text.checkPermissions();
    if (granted) {
      Voice2Text.startListening('en-US');
    } else {
      console.warn('Microphone permission denied');
      // Prompt user to enable permissions
    }
  } catch (error) {
    console.error('Permission error:', error);
  }
}

// Listen for recognized text
Voice2Text.onResults(result => {
  console.log('Recognized Text:', result.text);
});

// Handle errors
Voice2Text.onError(error => {
  console.error('Recognition Error:', error.message);
});

// Stop listening
Voice2Text.stopListening();
```

---

## 🧪 API Reference

| Method | Description |
| --- | --- |
| `checkPermissions(): Promise<boolean>` | Checks and requests microphone permission. |
| `startListening(locale: string)` | Starts voice recognition with the specified locale (e.g., `'en-US'`). |
| `stopListening()` | Stops the active voice recognition session. |
| `onResults(callback: (result: { text: string }) => void)` | Subscribes to recognition result events. |
| `onError(callback: (error: { message: string }) => void)` | Subscribes to error events. |

---

## 📂 Folder Structure (Android)

Ensure your native files are organized as follows:

```
android/app/src/main/java/com/<your-app>/voice2text/
├── Voice2TextModule.java
└── Voice2TextPackage.java
```

---

## 🧠 Important Notes

- **Android Only**: iOS support is under development.
- **Google Speech Services**: Ensure your emulator or device has Google Speech Services installed and active.
- **Internet Connection**: Voice recognition typically requires an active internet connection.

---

## 🚀 Roadmap

- [x] Android support
- [ ] iOS implementation
- [ ] Continuous recognition (streaming)
- [ ] Dynamic language switching
- [ ] Offline recognition support

---

## 👨‍💻 Author

Maintained by [Gokulkrishna](https://github.com/GokulKir)

---

## 📄 License

MIT © 2025