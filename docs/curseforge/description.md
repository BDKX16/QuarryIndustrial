# Quarry Industrial

**Animated industrial quarries that grow with your power grid — from a humble LV machine to a nuclear quarry fed by
eight reactors — plus oil pumpjacks, offshore rigs, diesel and steam generators and a fleet of drones.**

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

## 🔩 Drill bits that wear out

Quarries need a **drill bit** — iron, diamond, netherite or PDC, each crafted from the previous one. Every block
wears it (more for hard rock), and you can *see* it: the carbide inserts fall off row by row, and a dying bit throws
sparks. Pipe in **water** for +25% speed and half the wear. The Nuclear Quarry chews 400 blocks per second: without
water its PDC bit glows red, smokes and throttles itself.

Chips of the block being drilled fly out of the bit, and the **hopper on the carriage** fills up with whatever the
quarry is digging.

## 🌊 Offshore drilling rig

Drop it on a deep ocean: it builds a 7×7 grated deck, four legs down to the seabed and a lattice derrick with a
travelling block, a doghouse, an engine house and a **gas flare**. It drills 24 m into the seabed, then produces oil
forever — faster than any pumpjack.

## 🚁 Drones

Drone Pads and a Drone Linker, plus five drones that take off, cruise above everything and land:

- **Cargo** — items between two pads (empty a quarry into a chest far away, no pipes).
- **Tanker** — 16 buckets of any fluid (offshore oil to the coast).
- **Power** — a flying 400k EU battery for quarries off the grid.
- **Maintenance** — swaps worn drill bits on nearby quarries and rigs by itself.
- **Survey** — sweeps a 128-block radius and brings back a report with every oil geyser it saw.

## ⚡ Ala Industrial, all the way

- **Diesel Generator** — burns Ala's diesel, fuel oil or biofuel (up to 128 EU/t). Pumpjack → distillation → diesel → power.
- **Steam Turbine** — caps an Ala **Steam Nozzle** and turns the reactor's wasted steam into EU.
- **Diesel motor** — feed diesel to a pumpjack's motor row and it runs on its own fuel.
- **Oil Slimes** crawl out of your wells now and then.
- **Ore Seeker** upgrade, crafted from Ala's drill row and column modules: the drill head only breaks ores.

## 📘 Manual, sounds & advancements

An in-game **Operator's Manual** (10 chapters, English and Spanish) comes with your first quarry. Custom sounds for
the drill, diesel engines, turbine, drones, the pumpjack's stroke and a stall alarm, and **14 advancements**.

## 🔧 Upgrades & GUI

**Area, Speed, Fortune I–III, Silk Touch, Ore Seeker** and a **Void filter** for junk blocks. The GUI shows energy,
coolant, bit heat, live EU/t input (per port for the Nuclear Quarry), current layer, progress and a **live minimap**
of the layer being dug.

![GUI](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/screenshots/09_gui.png)

## 📦 Requirements

- Minecraft **26.3**, **Fabric Loader** and **Fabric API**
- Team Reborn Energy is **bundled**
- **Ala Industrial** recommended (cables, reactors, oil, diesel, steam). Without it, recipes switch to vanilla
  ingredients and the oil machines stay idle (no oil fluid).
- Optional: **Jade** (machine info when you look at a block), **JEI** or **REI** (recipes and an info page per machine).

Everything is configurable in `config/quarryindustrial.json`.

---

### 🇪🇸 En español

Quarries animadas que escalan con tu energía, desde LV hasta una quarry nuclear multibloque alimentada por hasta 8
reactores de Ala Industrial, con brocas que se gastan y refrigeración por agua; cigüeñas y plataformas offshore que
extraen petróleo infinito; generadores diésel y de vapor; y cinco drones (carga, cisterna, energía, mantenimiento y
explorador). Trae un manual en el juego, sonidos propios y 14 logros. Funciona con EU (Team Reborn Energy).
