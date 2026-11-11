# Reglas de R8 para el build de entrega de PharmaMobil.
# La reduccion elimina/renombra clases que solo se resuelven en tiempo de ejecucion
# (serializacion e inyeccion). Estas reglas conservan lo minimo necesario.

# --- Trazas legibles en Crashlytics -------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Metadatos que necesitan la serializacion y los genericos -----------------------
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

# --- kotlinx.serialization ----------------------------------------------------------
# Los DTO y PerfilLocal se (de)serializan con el serializador generado por el plugin.
-keepclassmembers class pe.edu.upeu.pharmamobile2.** {
    *** Companion;
}
-keepclasseswithmembers class pe.edu.upeu.pharmamobile2.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class pe.edu.upeu.pharmamobile2.**$$serializer { *; }

# --- Ktor: el logger por defecto referencia SLF4J, que no existe en Android ---------
-dontwarn org.slf4j.**
-dontwarn io.ktor.util.debug.**
-dontwarn java.lang.management.**
