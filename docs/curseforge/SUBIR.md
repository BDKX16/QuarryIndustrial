# Cómo subir Quarry Industrial a CurseForge

Todo lo que pide el formulario de CurseForge, listo para copiar.

## 1. Crear el proyecto (una sola vez)

| Campo | Valor |
|---|---|
| **Name** | Quarry Industrial |
| **Summary** (máx. ~100 caracteres) | Animated quarries, a nuclear mega-quarry, oil rigs and a drone fleet run from a radar console. |
| **Avatar / logo** | `docs/curseforge/icon.png` (512×512) |
| **Description** | Pegar `docs/curseforge/description.md` (el editor de CurseForge acepta Markdown) |
| **Main category** | Technology |
| **Otras categorías** | Technology › Processing, Technology › Energy, Fabric |
| **License** | All Rights Reserved (como `LICENSE.txt`). Si querés que otros puedan usar el código, cambialo antes de subir. |
| **Source** | https://github.com/BDKX16/QuarryIndustrial |

En la descripción, las imágenes (banner, encabezados de sección, separador y capturas con marco) apuntan a
`https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/...`: se ven cuando el repo es
público y `docs/curseforge/` está en la rama `main` (hay que hacer push antes de pegar la descripción). Si no,
subí esas imágenes a la galería de CurseForge y reemplazá los links.

**Colores de la página (regla 60/30/10):** 60 % neutro grafito (`#0B0E14` a `#303B4D`), 30 % azul acero (`#123560`
a `#9CCBF2`, el del mapa de la centralita) y 10 % ámbar de peligro (`#A87408`, `#F2B816`, `#FFDE7A`). El banner,
los encabezados, el separador y los marcos de las capturas se generan con `python tools/gen_banner.py` (lee la
tipografía del jar de Minecraft; no se versiona). Para cambiar un texto o un color, editá el script y volvé a correrlo.

## 2. Galería (Images)

Subí las capturas de `docs/screenshots/` (sin marco) en este orden; la primera sirve de portada:

1. `01_quarry_hv.jpg` — Quarry HV cavando en una pradera, con su pórtico
2. `32_console_selected.png` — La centralita de drones: mapa, ficha del pad y su alcance
3. `04_nuclear.jpg` — Quarry nuclear con sus 4 cabezales
4. `14_drone_pads.jpg` — Los cinco drones en sus pads
5. `17_offshore_deck.jpg` — Cubierta y torre de la plataforma offshore
6. `06_pumpjack.jpg` — Cigüeña sobre un géiser de Ala
7. `11_hopper.jpg` — El carro con la tolva llena
8. `02_drill_closeup.jpg` — Primer plano de la broca tricónica
9. `05_pdc_closeup.jpg` — Broca PDC de la quarry nuclear
10. `30_console_map.png` y `31_console_drones.png` — Pestañas de mapa y de drones
11. `33_console_wells.png` — Pestaña de pozos
12. `16_offshore.jpg`, `15_drone_flight.jpg`, `03_gantry.jpg`, `07_pumpjack_front.jpg`, `08_ports.jpg` — Extras
13. `09_gui.png` — La GUI de la quarry con el minimapa

El logo (`docs/curseforge/icon.png`) y el banner (`docs/curseforge/banner.png`, 960×240) son los dos recursos de
marca del proyecto.

## 3. Subir el archivo (cada versión)

```bash
./gradlew build
```

| Campo | Valor |
|---|---|
| **Archivo** | `build/libs/QuarryIndustrial-<versión>.jar` (no el `-sources.jar`) |
| **Display name** | `Quarry Industrial <versión> (26.3, Fabric)` |
| **Release type** | Beta mientras no esté jugado en partidas reales; después Release |
| **Game version** | 26.3 |
| **Mod loader** | Fabric |
| **Java** | 25 |
| **Relaciones** | Fabric API → *Required dependency*; Ala Industrial → *Optional dependency* |
| **Changelog** | Lo nuevo de esa versión |

Antes de subir, cambiá `mod_version` en `gradle.properties` (hoy dice `1.0-SNAPSHOT`) por algo como `1.0.0`, y
`maven_group` (`org.example.pz`) por un grupo propio si lo vas a publicar en Maven.
