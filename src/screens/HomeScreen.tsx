import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { RootStackParamList } from '../types/navigation';

type Props = NativeStackScreenProps<RootStackParamList, 'Home'>;

export default function HomeScreen({ navigation }: Props) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>BamScan Free</Text>
      <Text style={styles.subtitle}>Scannez des codes ou des documents en un instant</Text>

      <Pressable
        style={[styles.card, styles.cardPrimary]}
        onPress={() => navigation.navigate('CodeScanner')}
      >
        <Text style={styles.cardTitle}>Scanner un code</Text>
        <Text style={styles.cardText}>QR code / code-barres</Text>
      </Pressable>

      <Pressable
        style={[styles.card, styles.cardSecondary]}
        onPress={() => navigation.navigate('DocumentScanner')}
      >
        <Text style={styles.cardTitle}>Scanner un document</Text>
        <Text style={styles.cardText}>Photo de document</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'stretch',
    justifyContent: 'center',
    padding: 24,
    gap: 16,
  },
  title: {
    fontSize: 32,
    fontWeight: '700',
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 15,
    color: '#666',
    textAlign: 'center',
    marginBottom: 24,
  },
  card: {
    borderRadius: 16,
    padding: 24,
  },
  cardPrimary: {
    backgroundColor: '#2563eb',
  },
  cardSecondary: {
    backgroundColor: '#0f172a',
  },
  cardTitle: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 4,
  },
  cardText: {
    color: '#e2e8f0',
    fontSize: 14,
  },
});
