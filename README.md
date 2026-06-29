# 🌟 Verity – Your AI Helper Friend
## Minecraft Java Edition 1.20.1 – Forge Mod

---

## Mit csinál?

Amikor betöltöd a világot, **Verity** üdvözöl:
```
✨ Verity has joined the world!
[Verity] Hi! I'm Verity, your personal Helper Friend! 🌟
[Verity] Ask me anything — I know everything about Minecraft!
```

**Chat parancs:** `!verity <kérdés>`

| Példa | Eredmény |
|---|---|
| `!verity where is diamond` | Elmondja a legjobb Y-szintet |
| `!verity give me 5 diamonds` | Tényleg odaadja! ✅ |
| `!verity give me an iron sword` | Kardot ad ✅ |
| `!verity take me to a village` | Megkeresi a falut ✅ |
| `!verity how to make beacon` | Bármilyen kérdés! |

---

## Fordítás (Build) – Lépések

### Szükséges:
- **Java 17** (JDK, nem JRE!) → https://adoptium.net/
- **Internet kapcsolat** (Gradle letölti a Forge-ot első buildnél)

### Parancsok:

**Windows (CMD/PowerShell):**
```
gradlew.bat build
```

**Linux/Mac:**
```bash
chmod +x gradlew
./gradlew build
```

**Az elkészült JAR helye:**
```
build/libs/verity-1.0.0.jar
```

---

## Telepítés

1. Töltsd le és telepítsd a **Forge 1.20.1-47.2.0** -t:
   → https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html

2. Másold a `verity-1.0.0.jar` fájlt a Minecraft `mods/` mappájába:
   - **Windows:** `%AppData%\.minecraft\mods\`
   - **Linux:** `~/.minecraft/mods/`
   - **Mac:** `~/Library/Application Support/minecraft/mods/`

3. Indítsd el a Minecraft Forge 1.20.1 profilt.

---

## API Kulcs beállítása

Az első indítás után a config fájl automatikusan létrejön:

**Windows:** `%AppData%\.minecraft\config\verity.toml`
**Linux:** `~/.minecraft/config/verity.toml`

Nyisd meg és írd be a kulcsot:
```toml
[verity.api]
    # Get a FREE key at: https://aistudio.google.com/app/apikey
    geminiApiKey = "IDE_A_KULCSOD"
    geminiModel = "gemini-3.5-flash"
```

**Ingyenes API kulcs:** https://aistudio.google.com/app/apikey

---

## Config lehetőségek (verity.toml)

```toml
[verity.api]
    geminiApiKey = "..."        # Gemini API kulcs (KÖTELEZŐ)
    geminiModel = "gemini-3.5-flash"  # Modell neve

[verity.behavior]
    commandPrefix = "!verity"   # Parancs prefix
    showWelcomeMessage = true   # Üdvözlő üzenet be/ki
    allowItemGiving = true      # Item adás engedélyezése
    allowTeleport = true        # Teleport engedélyezése
```

---

## Projekt struktúra

```
verity_forge/
├── build.gradle               ← Forge build konfig
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat      ← Build parancs
├── gradle/wrapper/
│   └── gradle-wrapper.properties
└── src/main/
    ├── java/com/verity/mod/
    │   ├── VerityMod.java         ← Fő mod osztály
    │   ├── VerityConfig.java      ← Konfiguráció
    │   ├── VerityEventHandler.java ← Chat + join esemény
    │   └── GeminiClient.java      ← Gemini API hívás
    └── resources/
        ├── pack.mcmeta
        └── META-INF/
            └── mods.toml          ← Mod metadata
```

---

## Hibák és megoldások

| Hiba | Megoldás |
|---|---|
| `BUILD FAILED` – Java verzió | Telepítsd a Java 17 JDK-t |
| Verity nem válaszol | Ellenőrizd a `verity.toml` API kulcsát |
| `HTTP 400` | Hibás modell név a configban |
| `HTTP 429` | Túl sok kérés – várj egy percet |
| Item nem érkezik | Ellenőrizd az item ID-t (pl. `diamond` ✅, `diamonds` ❌) |

---

*Verity Mod – Java Edition 1.20.1 Forge ✨*
