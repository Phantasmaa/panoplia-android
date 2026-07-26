# Panoplia · Android

Cliente nativo en Kotlin (Jetpack Compose) del ecosistema de microservicios de
[panoplia-cloud](https://github.com/...). Reemplaza la PWA "Utilidades" con UI
Android first-class, acceso a galería nativo para image-enhancer, y un launcher
con grid de microservicios.

## Stack

- **Kotlin 2.0.21** + **Jetpack Compose** (Material 3, dark theme)
- **Hilt 2.55** (DI)
- **Retrofit 2.11 + OkHttp 4.12 + Moshi 1.15** (networking + JSON)
- **Navigation Compose 2.8.5**
- **DataStore Preferences 1.1.1** (sesión persistente)
- **Coroutines + Flow** (async)
- **minSdk 26 · targetSdk 36** (compileSdk 36)
- **JDK 17+** para build

## Arquitectura

```
app/
├── data/
│   ├── model/       Models Moshi (@JsonClass)
│   ├── remote/      Retrofit PanopliaApi
│   ├── local/       SessionManager (DataStore)
│   └── repo/        PanopliaRepository (single source of truth)
├── di/              Hilt NetworkModule
├── ui/
│   ├── MainActivity  NavHost entry
│   ├── theme/       PanopliaTheme dark scheme
│   └── screens/
│       ├── SplashScreen    Decide login vs home based on DataStore
│       ├── LoginScreen     POST /auth/verify
│       ├── HomeScreen      Grid de microservicios
│       └── ImageEnhancerScreen   Native: pick → upload → render
└── PanopliaApp       @HiltAndroidApp
```

### Servicios registrados (static catalog en `PanopliaRepository.services()`)

| id              | nombre           | ruta                       | native |
|-----------------|------------------|----------------------------|--------|
| image-enhancer  | Mejorar imagen   | `/proxy/image-enhancer/`   | ✅     |
| image-crop      | Recortar imagen  | `/proxy/image-crop/`       | ❌     |
| pdf-tools       | PDF Tools        | `/proxy/pdf-tools/`        | ❌     |
| pdf-chat        | PDF Chat         | `/proxy/pdf-chat/`         | ❌     |
| chordbook       | Chordbook        | `/proxy/chordbook/`        | ❌     |
| metube          | MeTube           | `/metube/`                 | ❌     |
| finanzas        | Finanzas         | `/`                        | ❌     |

Servicios `native: true` se renderizan en Compose puro. El resto abre en el
navegador via Intent.

## Build

```sh
# JDK 17 o 21
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

## Run tests

```sh
./gradlew :app:testDebugUnitTest
```

## Configurar backend

Editá `app/build.gradle.kts`:

```kotlin
defaultConfig {
    buildConfigField("String", "API_BASE_URL", "\"https://tu-tunnel.trycloudflare.com\"")
}
```

O usá flavors (`staging`, `prod`) con diferentes URLs.

## Pendiente / Roadmap

- [ ] AppLinks con Digital Asset Links para abrir URLs sin pasar por el browser
- [ ] Módulo nativo de image-crop (Compose canvas + Android graphics)
- [ ] Módulo nativo de chordbook (storage local + sync)
- [ ] Widgets (finanzas: balance del día)
- [ ] Share Intent Receiver (compartir imagen → "Mejorar")
- [ ] Background workers (sync finanzas offline)
- [ ] Tests instrumentados (Espresso + Compose UI test)
- [ ] Migrar TWA actual a este paquete (unificar install)
- [ ] Firma release con keystore dedicado

## Decisiones técnicas

- **Kotlin nativo, no WebView**: se reutiliza la lógica de los microservicios
  Python pero la UI es Compose. Ventaja: acceso a APIs de Android (galería,
  share, foreground services), animaciones nativas, GPU para image processing.
- **Static catalog de servicios**: en vez de fetchear un `/herramientas.json`
  cada vez, hardcoded. Permite tipo-seguro y compila offline. Si querés
  dinamismo, ver PR futuro.
- **Hilt + KSP**: KSP es ~2x más rápido que KAPT y Hilt 2.55 ya lo soporta
  oficialmente.
- **DataStore sobre SharedPreferences**: API Flow-based, no se rompe con
  crashes concurrentes.
- **API_BASE_URL en BuildConfig**: permite flavors sin tocar código.
- **Cleartext enabled solo para `trycloudflare.com` y `localhost`**: en
  `network_security_config.xml`.

## Licencia

MIT.
