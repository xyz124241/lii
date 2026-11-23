# ItemLock + Sense

Paper-Plugin zum serverweiten Sperren von Items über ein Inventar-Menü sowie zum Erstellen und Verwenden der Sense.

## Build
- Java 25
- Maven
- Paper 26.2 API

Lokal bauen: `mvn clean package`

Das fertige Plugin liegt danach unter `target/ItemLock-1.0.0.jar`.
GitHub Actions veröffentlicht `ItemLock.jar` als Release-Datei.

**Hinweis:** In diesem Repository ist derzeit kein `resourcepack/`-Ordner enthalten. Deshalb erstellt der Workflow nur die Plugin-JAR und versucht nicht mehr, ein nicht vorhandenes Resourcepack zu zippen.

## Befehle
- `/itemlock` (`/il`, `/sperre`): Menü zum Sperren/Freigeben
- `/itemlock list`
- `/itemlock reload`
- `/sense give [spieler]`

Sense-Rezept (formlos): Netherite-Axt + Netherite-Schwert + Mace + Nether Star.
