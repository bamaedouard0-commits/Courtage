# FileRescue Libre

**FileRescue Libre** est une application Android open source de récupération de fichiers supprimés (photos, vidéos, musiques, documents/PDF), destinée aux appareils **rootés**.

Le projet est porté par **OUAGADOUSOFT**. Il vise à offrir une alternative 100 % gratuite, sans paywall et sans limite d'export aux solutions propriétaires existantes (Wondershare Recoverit, DiskDigger Pro, dr.fone…), en s'appuyant sur des techniques de récupération de données documentées publiquement (file carving, analyse de signatures de fichiers).

## Statut

🚧 **En développement — V1 (MVP) en cours.**
Le socle du projet (structure Android, détection root, UI de base) est en place. Le moteur de scan/carving n'est pas encore implémenté.

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
| Gestion root | [libsu](https://github.com/topjohnwu/libsu) (topjohnwu) |
| Scan bas niveau | Module natif C/C++ (NDK) — à venir |
| Base de données locale | Room (SQLite) — à venir |
| Miniatures | Coil / Glide — à venir |
| Compatibilité | Android 8.0 (API 26) minimum |

## Architecture

Le projet suit une architecture en couches :

```
UI (Compose) → ViewModel → Domain → Data/Native → Repository (Room)
```

```
app/src/main/java/com/ouagadousoft/filerescuelibre/
├── ui/          # Écrans Compose et thème
├── viewmodel/   # État de l'app, orchestration des scans
├── domain/      # Logique métier (règles de fiabilité, filtrage) — à venir
├── data/        # Accès root, lecture bas niveau, repository Room — à venir
└── native/      # Moteur de carving (module NDK) — à venir
```

## Licence

Ce projet est distribué sous licence **[GNU GPL v3](LICENSE)**. Toute évolution ou fork doit rester open source sous les mêmes conditions.

## Confidentialité

- Zéro télémétrie
- Zéro compte utilisateur
- Zéro requête réseau obligatoire — tout le traitement reste local à l'appareil
