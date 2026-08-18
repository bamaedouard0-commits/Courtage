# FileRescue Libre

**FileRescue Libre** est une application Android open source de récupération de fichiers supprimés (photos, vidéos, musiques, documents/PDF), destinée aux appareils **rootés**.

Le projet est porté par **OUAGADOUSOFT**. Il vise à offrir une alternative 100 % gratuite, sans paywall et sans limite d'export aux solutions propriétaires existantes (Wondershare Recoverit, DiskDigger Pro, dr.fone…), en s'appuyant sur des techniques de récupération de données documentées publiquement (file carving, analyse de signatures de fichiers).

## Statut

🚧 **En développement — V1 (MVP) en cours.**

- ✅ Structure Android, détection root, UI de base
- ✅ Scan rapide (F3) : corbeille MediaStore (`.trashed-*`) et corbeilles FUSE par app (`.Trash-<uid>`)
- ✅ Scan approfondi (F4) : file carving par signatures sur la partition data brute
- ✅ Zone de scan personnalisée (F2), filtres, miniatures (photos/vidéos), export/récupération, historique des scans (Room, purgé au-delà de 20 entrées)
- ✅ Pause/reprise du scan approfondi (F9) : persistante entre sessions (app tuée) et via un bouton pause manuel
- ⏳ Miniatures audio/documents, formats bureautiques (DOCX/XLSX/PPTX, DOC/XLS hérités), portage natif (NDK) — à venir

### Limites connues du scan approfondi (V1)

- **Formats couverts** : JPEG, PNG, BMP, WEBP, HEIC (images), MP4, MOV, 3GP, AVI (vidéos), WAV (audio) et PDF (documents). **GIF, MKV, MP3, DOCX/XLSX/PPTX et DOC/XLS hérités sont volontairement exclus** de cette V1 — leur fin de fichier ne peut pas être déterminée de façon fiable par simple recherche de signature (GIF/MP3 : trailer trop court ou absent ; MKV : EBML nécessitant un vrai parseur ; formats Office : table centrale ZIP ou conteneur OLE2/CFB à parser). Le scan rapide, qui n'a pas cette contrainte (les fichiers existent déjà intacts sur le disque), reconnaît en revanche MP3 et les formats bureautiques.
- **Implémentation Kotlin, pas encore native (NDK)** : le moteur lit et analyse la partition `userdata` en Kotlin pur via `libsu:io`. C'est plus lent qu'un module C/C++ natif (recommandé dans le cahier des charges pour la performance sur de gros volumes), mais permet une V1 fonctionnelle et vérifiable sans risquer une chaîne JNI/CMake non testée. Un portage natif est une optimisation de suivi documentée, pas un blocage fonctionnel.
- **Localisation de la partition** : résolution via des chemins connus (`/dev/block/by-name/userdata`, etc.) puis recherche large en dernier recours. Selon le fabricant/ROM, ce chemin peut varier ou être bloqué par une politique SELinux stricte même sous root.

## Objectifs

- Outil gratuit et sans restriction d'export pour tous les utilisateurs Android.
- Code source public et auditable — transparence totale.
- Zéro collecte de données personnelles, zéro connexion internet requise.
- Base solide et évolutive pour des fonctionnalités avancées.

## Choix technique : root uniquement (V1)

La V1 cible exclusivement les appareils rootés (Magisk recommandé), afin d'accéder directement au périphérique bloc du stockage et effectuer un scan bas niveau fiable (file carving). Le scoped storage introduit depuis Android 10 rend cette approche impossible sans root. Une version sans root, plus limitée, pourra être envisagée en V2.

## Stack technique

| Composant | Choix |
|---|---|
| Langage | Kotlin |
| UI | Jetpack Compose |
| Gestion root | [libsu](https://github.com/topjohnwu/libsu) (topjohnwu) — modules `core` et `io` |
| Scan bas niveau | Kotlin pur (V1) via `libsu:io` ; portage natif C/C++ (NDK) envisagé pour la performance |
| Base de données locale | Room (SQLite) — historique des scans |
| Miniatures | `BitmapFactory` / `MediaMetadataRetriever` (photos et vidéos) — pas encore de Coil/Glide |
| Compatibilité | Android 8.0 (API 26) minimum |

## Architecture

Le projet suit une architecture en couches :

```
UI (Compose) → ViewModel → Domain → Data/Native → Repository (Room)
```

```
app/src/main/java/com/ouagadousoft/filerescuelibre/
├── ui/          # Écrans Compose, navigation et thème
├── viewmodel/   # État de l'app, orchestration des scans
├── domain/      # Modèles, contrats de repository, règles métier
├── data/        # Accès root (RootShell), scan rapide, carving, récupération, miniatures, historique (Room)
└── native/      # Réservé à un futur portage natif (NDK) du moteur de carving
```

## Licence

Ce projet est distribué sous licence **[GNU GPL v3](LICENSE)**. Toute évolution ou fork doit rester open source sous les mêmes conditions.

## Confidentialité

- Zéro télémétrie
- Zéro compte utilisateur
- Zéro requête réseau obligatoire — tout le traitement reste local à l'appareil
