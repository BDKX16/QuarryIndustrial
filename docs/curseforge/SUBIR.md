# Cómo subir Quarry Industrial a CurseForge

Todo lo que pide el formulario de CurseForge, listo para copiar.

## 1. Crear el proyecto (una sola vez)

| Campo | Valor |
|---|---|
| **Name** | Quarry Industrial |
| **Summary** (máx. ~100 caracteres) | Animated quarries that scale up to a multi-reactor nuclear quarry, plus oil pumpjacks. |
| **Avatar / logo** | `docs/curseforge/icon.png` (512×512) |
| **Description** | Pegar `docs/curseforge/description.md` (el editor de CurseForge acepta Markdown) |
| **Main category** | Technology |
| **Otras categorías** | Technology › Processing, Technology › Energy, Fabric |
| **License** | All Rights Reserved (como `LICENSE.txt`). Si querés que otros puedan usar el código, cambialo antes de subir. |
| **Source** | https://github.com/BDKX16/QuarryIndustrial |

En la descripción, las imágenes apuntan a `https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/screenshots/...`:
se ven cuando el repo es público y las capturas están en la rama `main`. Si no, subí las capturas a la galería de
CurseForge y usá esos links.

## 2. Galería (Images)

Subí las capturas de `docs/screenshots/` en este orden; la primera sirve de portada:

1. `01_quarry_hv.jpg` — Quarry HV cavando en una pradera, con su pórtico
2. `04_nuclear.jpg` — Quarry nuclear con sus 4 cabezales
3. `06_pumpjack.jpg` — Cigüeña sobre un géiser de Ala
4. `02_drill_closeup.jpg` — Primer plano de la broca tricónica
5. `05_pdc_closeup.jpg` — Broca PDC de la quarry nuclear
6. `03_gantry.jpg` — Carro y puente del pórtico
7. `07_pumpjack_front.jpg` — Cigüeña de frente: cabeza de caballo y vástago
8. `08_ports.jpg` — Cubo de la quarry nuclear con los puertos de energía
9. `09_gui.png` — La GUI con el minimapa

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
