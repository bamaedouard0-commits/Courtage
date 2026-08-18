export type RootStackParamList = {
  Home: undefined;
  CodeScanner: undefined;
  DocumentScanner: undefined;
  ScanResult: { type: 'code' | 'document'; value: string; imageUri?: string };
};
