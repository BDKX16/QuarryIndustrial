# Quarry Industrial

**Animated industrial quarries that grow with your power grid — from a humble LV machine to a nuclear quarry fed by
eight reactors — plus oil pumpjacks for Ala Industrial's geysers.**

Built for **Ala Industrial** on Fabric, but it speaks plain **EU (Team Reborn Energy)**, so any compatible
generator, cable or battery works.

![HV quarry at work](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/screenshots/01_quarry_hv.jpg)

---

## ⛏ Quarries that scale with energy

Place a quarry, give it power, and watch it raise a steel **gantry frame** around its area. A bridge rolls along
the frame, a carriage rolls along the bridge, and from the carriage hangs a real **drill string** — kelly, drill
pipe with tool joints, and a **tricone bit** whose three cones spin on their own axes as they bite into the rock.

| Tier | Input | Area (base → max) | Drill heads | Max blocks/s |
|---|---|---|---|---|
| **LV** | 32 EU/t | 9×9 → 16×16 | 1 | 5 |
| **MV** | 128 EU/t | 16×16 → 48×48 | 1 | 20 |
| **HV** | 512 EU/t | 32×32 → 80×80 | 2 | 60 |
| **Nuclear** | 512 EU/t **per port**, 1–8 ports | 64×64 → 144×144 | 4 | 400 |

- **Speed comes from power.** Every block costs EU based on its hardness. An underfed quarry just digs slower.
- **Output** goes to a 27-slot buffer that auto-pushes into any adjacent chest or pipe.
- **Claim-friendly:** blocks are broken by a fake player carrying the owner's UUID.
- **Light on servers:** only the quarry's chunk and the chunks under the drill heads are kept loaded.
- **Won't flood:** fluids inside the area are removed, and fluids touching the border from outside are dammed
  with cobblestone.

![Drill close-up](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/screenshots/02_drill_closeup.jpg)

## ☢ The Nuclear Quarry

Ala Industrial's thickest cable — and its reactor — top out at **512 EU/t**. The Nuclear Quarry breaks that
ceiling: build a **3×3×3 multiblock** with the core on the front face and up to **8 HV energy ports**, then run
one reactor into each port. Four drill heads with **PDC bits** (diamond-cutter blades) chew through up to a
144×144 area. Missing a block? A **holographic blueprint** shows you exactly where.

![Nuclear quarry](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/screenshots/04_nuclear.jpg)

## 🛢 Oil Pumpjacks

Ala Industrial generates oil geysers: a 1×1 shaft dropping into a buried dome of crude. Put a **pumpjack** over
the shaft and it will:

1. **Drain** the real oil left in the shaft and dome (fast).
2. Then keep pumping **forever** at a steady rate, with up to 4 speed upgrades.

Oil flows into an internal tank. Ala Industrial pipes attached to the base pull it automatically (no wrench
needed) and carry it straight to the distillation column for diesel and fuel oil. The walking beam, horsehead, pitman arms and counterweighted cranks
move with real four-bar-linkage kinematics.

![Pumpjack](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/screenshots/06_pumpjack.jpg)

## 🔧 Upgrades & GUI

**Area, Speed, Fortune I–III, Silk Touch** and a **Void filter** for junk blocks. The GUI shows energy, live EU/t
input (per port for the Nuclear Quarry), current layer, progress and a **live minimap** of the layer being dug.

![GUI](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/screenshots/09_gui.png)

## 📦 Requirements

- Minecraft **26.3**, **Fabric Loader** and **Fabric API**
- Team Reborn Energy is **bundled**
- **Ala Industrial** recommended (cables, reactors, oil). Without it, recipes switch to vanilla ingredients and
  pumpjacks stay idle (no oil fluid).

Everything is configurable in `config/quarryindustrial.json`.

---

### 🇪🇸 En español

Quarries animadas que escalan con tu energía, desde LV hasta una quarry nuclear multibloque alimentada por hasta 8
reactores de Ala Industrial, y cigüeñas que extraen petróleo infinito de los géiseres de Ala. Funciona con EU
(Team Reborn Energy).
