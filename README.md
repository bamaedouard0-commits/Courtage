# BamScan Free

Application mobile de scan (QR codes, codes-barres et documents), construite avec [Expo](https://expo.dev) et React Native (TypeScript).

## Fonctionnalités

- **Scanner un code** : lecture de QR codes et codes-barres (EAN-13, EAN-8, Code128, Code39, UPC) via la caméra.
- **Scanner un document** : capture photo d'un document via la caméra.
- Écran de résultat affichant la donnée décodée ou l'aperçu du document capturé.

## Stack technique

- Expo SDK 57 (React Native 0.86, React 19)
- TypeScript
- `expo-camera` pour la caméra et le scan de codes
- `@react-navigation/native` + `native-stack` pour la navigation

## Démarrage

```bash
npm install
npm run start   # ouvre Expo Dev Tools
npm run android # lance sur un émulateur/appareil Android
npm run ios     # lance sur un simulateur/appareil iOS (macOS requis)
npm run web     # lance la version web
```

## Structure du projet

```
App.tsx                        # Point d'entrée, configuration de la navigation
src/
  screens/
    HomeScreen.tsx              # Écran d'accueil
    CodeScannerScreen.tsx       # Scan QR code / code-barres
    DocumentScannerScreen.tsx   # Capture photo de document
    ScanResultScreen.tsx        # Affichage du résultat
  types/
    navigation.ts               # Typage des routes de navigation
```
