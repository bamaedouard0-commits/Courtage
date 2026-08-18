import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { RootStackParamList } from '../types/navigation';

type Props = NativeStackScreenProps<RootStackParamList, 'ScanResult'>;

export default function ScanResultScreen({ route, navigation }: Props) {
  const { type, value, imageUri } = route.params;

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>
        {type === 'code' ? 'Code détecté' : 'Document capturé'}
      </Text>

      {imageUri ? (
        <Image source={{ uri: imageUri }} style={styles.preview} resizeMode="contain" />
      ) : (
        <View style={styles.valueBox}>
          <Text style={styles.value}>{value}</Text>
        </View>
      )}

      <Pressable style={styles.button} onPress={() => navigation.popToTop()}>
        <Text style={styles.buttonText}>Nouveau scan</Text>
      </Pressable>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    gap: 20,
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
  },
  valueBox: {
    backgroundColor: '#f1f5f9',
    borderRadius: 12,
    padding: 20,
    width: '100%',
  },
  value: {
    fontSize: 16,
    color: '#0f172a',
  },
  preview: {
    width: '100%',
    height: 400,
    borderRadius: 12,
    backgroundColor: '#000',
  },
  button: {
    backgroundColor: '#2563eb',
    borderRadius: 12,
    paddingVertical: 14,
    paddingHorizontal: 32,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
