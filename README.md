# Country Explorer — Android (Kotlin + Jetpack Compose)

A deliberately small app that exercises every fundamental you need for real
Android work: networking, JSON, concurrency, local persistence, list/detail
navigation, state management, and unit testing.

Its twin lives at [country-explorer-ios](https://github.com/sreekumarrajan/country-explorer-ios)
and implements the **same requirements** with SwiftUI, so you can read the two
side by side.

---

## What it does

- Loads ~217 countries from the World Bank API (no API key required)
- Loads each country's latest population from a **second** endpoint, in parallel
- Search by country name or capital
- Tap a row for a detail screen
- Favourite countries with the heart; favourites persist in a local database
- "Favourites only" filter
- Loading / error / empty states, with retry

## The data sources

| Need | Endpoint |
|---|---|
| Countries | `https://api.worldbank.org/v2/country?format=json&per_page=400` |
| Population | `https://api.worldbank.org/v2/country/all/indicator/SP.POP.TOTL?format=json&mrv=1` |
| Flag images | `https://flagcdn.com/w320/{iso2}.png` |

No signup, no API key, no rate limits worth worrying about.

> The response envelope is `[metadata, [rows]]` — a JSON array holding two
> *different* types. That is deliberate: real APIs are full of shapes that no
> single data class describes, and `CountryRemoteDataSource.decodeRows` shows
> how to handle one without contorting your models.

---

## Running it

```bash
# Unit tests (fast, no device needed)
./gradlew test

# Build a debug APK
./gradlew assembleDebug

# Install onto a running emulator or connected device
./gradlew installDebug
```

Or open the folder in Android Studio and press ⌃R.

**Requirements:** JDK 21, Android SDK with platform 37. `local.properties`
points Gradle at your SDK and is intentionally *not* committed.

---

## How the code is organised

```
app/src/main/java/com/sreekumar/countryexplorer/
├── MainActivity.kt              # the single Activity; hands off to Compose
├── CountryExplorerApplication.kt# process-wide object; builds the DI container
├── AppContainer.kt              # manual dependency injection
├── domain/
│   └── Country.kt               # the model the UI wants
├── data/
│   ├── CountryRepository.kt     # single source of truth; merges net + db
│   ├── remote/                  # Retrofit API, DTOs, envelope decoding
│   └── local/                   # Room entity, DAO, database
└── ui/
    ├── CountryExplorerApp.kt    # navigation graph (typed routes)
    ├── list/                    # list screen + its ViewModel
    ├── detail/                  # detail screen + its ViewModel
    ├── components/              # reusable Composables
    └── theme/                   # Material 3 theming
```

The dependency rule: **ui → data → domain**, never backwards. A ViewModel may
not import anything from `androidx.compose.ui`, which is what keeps it testable
on a plain JVM.

---

## Coming from Angular or React?

| You know | Android equivalent | File to read |
|---|---|---|
| React component | `@Composable` function | `ui/components/CountryRow.kt` |
| `useState` | `remember { mutableStateOf() }` | `ui/CountryExplorerApp.kt` |
| `useEffect(fn, [])` | `LaunchedEffect(Unit)` / `init` | `ui/list/CountryListViewModel.kt` |
| Controlled input | state hoisting | `ui/list/CountryListScreen.kt` |
| `key` prop | `items(key = { ... })` | `ui/list/CountryListScreen.kt` |
| RxJS `Observable` | `Flow` | `data/local/FavoriteCountry.kt` |
| `BehaviorSubject` | `MutableStateFlow` | `data/CountryRepository.kt` |
| `combineLatest` | `combine` | `data/CountryRepository.kt` |
| `debounceTime` | `.debounce()` | — (noted in the list ViewModel) |
| `Promise.all` | `coroutineScope { async ... }` | `data/remote/CountryRemoteDataSource.kt` |
| Angular service + DI | repository + `AppContainer` | `AppContainer.kt` |
| React Router config | `NavHost` + typed routes | `ui/CountryExplorerApp.kt` |
| `package.json` | `gradle/libs.versions.toml` | `gradle/libs.versions.toml` |
| CSS variables | `MaterialTheme` | `ui/theme/Theme.kt` |

Every file carries comments explaining the Android concept **and** how it maps
back to what you already know. Read them in this order:

1. `domain/Country.kt` — Kotlin syntax, data classes, null safety
2. `data/remote/` — Retrofit, serialization, structured concurrency
3. `data/local/` — Room, reactive queries
4. `data/CountryRepository.kt` — merging streams
5. `ui/list/CountryListViewModel.kt` — the ViewModel contract
6. `ui/list/CountryListScreen.kt` — Compose, state hoisting
7. `app/src/test/` — how all of the above becomes testable

---

## Things that will surprise a web developer

**Your screen is destroyed and recreated when the device rotates.** Not
re-rendered — *destroyed*. This is why `ViewModel` exists and why it holds the
state rather than the Composable. Nothing in the browser behaves this way.

**Network access is a declared permission.** Missing `<uses-permission
android:name="android.permission.INTERNET" />` in `AndroidManifest.xml` and
every request fails.

**`compileSdk`, `targetSdk` and `minSdk` are three different things.** Which
APIs you can call, which runtime behaviours you opt into, and which devices can
install the app. They move independently.

**The build system has opinions.** See `build.gradle.kts` in the root for a real
example: AGP 9 compiles Kotlin itself, but ships a Kotlin version too old for
the AndroidX libraries here, so the build raises it explicitly.

---

## Exercises

In rough order of difficulty:

1. **Sort control.** Add a toggle for sorting by name vs population descending.
   (Touch: `CountryListViewModel`, `CountryListScreen`.)
2. **Region filter.** A dropdown of the distinct regions. Derive the list from
   the data rather than hard-coding it.
3. **Pull-to-refresh.** Wrap the list in `PullToRefreshBox` and wire it to
   `onRefresh()` — the ViewModel already distinguishes refresh from first load.
4. **Offline cache.** Add a `countries` table to Room and serve from it when the
   network fails, so the app opens with data on a plane.
5. **Detail-screen map.** Show the lat/long on a Google Map or static map image.
6. **A test for the repository.** Prove that favouriting a country re-emits the
   merged list. The fakes you need already exist in `app/src/test/`.
7. **Swap manual DI for Hilt.** `AppContainer` becomes `@Module`; the ViewModels
   gain `@HiltViewModel`. A mechanical change — which is the point.
