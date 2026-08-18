import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { StatusBar } from 'expo-status-bar';

import CodeScannerScreen from './src/screens/CodeScannerScreen';
import DocumentScannerScreen from './src/screens/DocumentScannerScreen';
import HomeScreen from './src/screens/HomeScreen';
import ScanResultScreen from './src/screens/ScanResultScreen';
import { RootStackParamList } from './src/types/navigation';

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  return (
    <NavigationContainer>
      <StatusBar style="auto" />
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen name="Home" component={HomeScreen} options={{ title: 'BamScan Free' }} />
        <Stack.Screen
          name="CodeScanner"
          component={CodeScannerScreen}
          options={{ title: 'Scanner un code' }}
        />
        <Stack.Screen
          name="DocumentScanner"
          component={DocumentScannerScreen}
          options={{ title: 'Scanner un document' }}
        />
        <Stack.Screen
          name="ScanResult"
          component={ScanResultScreen}
          options={{ title: 'Résultat' }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
