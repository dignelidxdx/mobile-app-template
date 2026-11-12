# Lugar Favorito

Aplicación Android nativa para explorar Google Maps, guardar lugares favoritos y traducir frases en español al idioma del país seleccionado.

## Funciones

- Muestra Google Maps y centra la cámara en la ubicación actual.
- Permite seleccionar cualquier coordenada manteniendo pulsado el mapa.
- Identifica el nombre y el país del punto mediante geocodificación del dispositivo.
- Guarda y elimina lugares favoritos de forma local.
- Conserva los favoritos después de cerrar la aplicación.
- Elige automáticamente el idioma de destino según el país detectado.
- Acepta texto o dictado en español.
- Traduce en el dispositivo con Google ML Kit; el modelo del idioma se descarga cuando se usa por primera vez.

## Stack

- Kotlin y Jetpack Compose
- Material 3
- Google Maps Compose
- Google Play Services Location
- Android DataStore Preferences
- Google ML Kit Translation
- Coroutines y StateFlow

## Requisitos

- Android Studio compatible con Android Gradle Plugin 9.1.1
- JDK 17
- Android SDK 37.0
- Dispositivo o emulador con Android 8.0 (API 26) o superior y Google Play Services
- Proyecto de Google Cloud con Maps SDK for Android habilitado

## Configuración de Google Maps

1. Crea una clave para Maps SDK for Android en Google Cloud Console.
2. Restringe la clave a aplicaciones Android usando el nombre de paquete `com.example.lugarfavorito` y el SHA-1 de tu firma.
3. Crea `secrets.properties` en la raíz del proyecto:

```properties
MAPS_API_KEY=TU_CLAVE
```

`secrets.properties` está ignorado por Git y no debe publicarse.

## Ejecutar

1. Abre la carpeta raíz en Android Studio.
2. Espera la sincronización de Gradle.
3. Agrega `secrets.properties`.
4. Ejecuta la configuración `app` en un dispositivo o emulador con Google APIs.
5. Acepta los permisos de ubicación y micrófono cuando la aplicación los solicite.

## Uso

- Pulsa el botón de ubicación para volver a tu posición.
- Mantén pulsado cualquier punto del mapa para seleccionarlo.
- Pulsa el corazón para guardar o eliminar el lugar.
- Selecciona un favorito para regresar a él.
- Escribe una frase o pulsa el micrófono; luego pulsa **Traducir**.

## Verificación

```shell
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

En Windows usa `gradlew.bat`.
