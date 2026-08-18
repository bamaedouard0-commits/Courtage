# Ajoutez ici des règles ProGuard spécifiques au projet.
# Pour plus de détails, voir
#   http://developer.android.com/guide/developing/tools/proguard.html

# Si votre projet utilise WebView avec JS, décommentez pour conserver
# la classe JavascriptInterface :
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Décommentez pour conserver les numéros de ligne pour le débogage des stack
# traces en production.
#-keepattributes SourceFile,LineNumberTable

# Si vous conservez les numéros de ligne, décommentez pour masquer le nom
# du fichier source original.
#-renamesourcefileattribute SourceFile
