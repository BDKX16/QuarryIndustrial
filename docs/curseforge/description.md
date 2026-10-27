![Quarry Industrial](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/banner.png)

**Dig deeper than a pickaxe ever could.** Quarry Industrial gives you machines that are a joy to watch and a joy to
feed: steel gantries that rise on their own, drill strings that bite into the rock, pumpjacks that never run dry,
and a fleet of drones that does the legwork while you watch it all from a radar console.

It starts with a humble LV quarry and ends with a **nuclear quarry fed by eight reactors**. In between: oil, an ocean
rig, generators and a sky full of drones. Built for **Ala Industrial** on Fabric, but it speaks plain **EU (Team
Reborn Energy)**, so any compatible generator, cable or battery will do.

![HV quarry at work](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/01_quarry_hv.jpg)

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![What's new](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/new.png)

The latest updates turned the drones from a trick into a **logistics network**:

- **A Drone Control Station with a real map.** One block shows every pad, every drone in flight and every oil
  geyser within a kilometre, over terrain it draws from the places you have actually walked. Link pads, send the
  surveyor out and forget geysers without leaving your chair.
- **The map remembers.** Explore once and the console keeps it, per world and dimension, like a minimap that never
  forgets. Pan it and zoom it as fast as you like: the terrain is cached as images, so nothing is recalculated while
  you move.
- **See what a drone can reach.** Select a pad and the map draws the range of its drone, upgrades included.
- **Radar Repeaters** stretch the console's coverage. Chain them and the network reaches as far as you care to build.
- **Chunk Loaders for pads**, so a route keeps running while you are far away.
- **Maintenance drones clean distillation columns.** When an Ala Industrial column starts to coke up, the drone flies
  over, scrubs it and comes home with the coal.
- **Read-only GUIs** on pads, pumpjacks, offshore rigs, generators and turbines: status, energy and fluid bars,
  installed upgrades, one right-click away.

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Quarries that scale up](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/quarries.png)

Place a quarry, give it power, and watch it raise a steel **gantry frame** around its work area. A bridge rolls
along the frame, a carriage rolls along the bridge, and from the carriage hangs a real **drill string**: kelly,
pipe with tool joints and a **tricone bit** whose three cones spin on their own axes as they chew through stone.

| Tier | Input | Area (base → max) | Drill heads | Max blocks/s |
|---|---|---|---|---|
| **LV** | 32 EU/t | 9×9 → 16×16 | 1 | 5 |
| **MV** | 128 EU/t | 16×16 → 48×48 | 1 | 20 |
| **HV** | 512 EU/t | 32×32 → 80×80 | 2 | 60 |
| **Nuclear** | 512 EU/t **per port**, 1–8 ports | 64×64 → 144×144 | 4 | 400 |

- **Speed comes from power.** Every block costs EU in proportion to its hardness, and an underfed quarry just digs
  slower instead of stopping.
- **Output goes where you want it.** A 27-slot buffer pushes into any adjacent chest or pipe.
- **Server-friendly.** Only the quarry's chunk and the chunks under the drill heads stay loaded, and blocks are
  broken by a fake player carrying the owner's UUID, so claims behave.
- **It will not flood your base.** Fluids inside the area are removed; fluids touching the border are dammed with
  cobblestone.

![Drill close-up](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/02_drill_closeup.jpg)

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![The Nuclear Quarry](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/nuclear.png)

Ala Industrial's thickest cable, and its reactor, top out at **512 EU/t**. The Nuclear Quarry is what you build when
that is not enough: a **3×3×3 multiblock** with the core on the front face and up to **8 HV energy ports**, one
reactor into each. Four drill heads with **PDC bits** (diamond-cutter blades) take apart a 144×144 area. If you miss
a block, a **holographic blueprint** shows exactly where it goes.

![Nuclear quarry](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/04_nuclear.jpg)

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Oil pumpjacks](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/pumpjacks.png)

Ala Industrial buries oil under 1×1 geysers: a shaft dropping into a dome of crude. Set a **pumpjack** over the
shaft and it will:

1. **Drain** the real oil in the shaft and dome, fast.
2. Then keep pumping **forever** at a steady rate, with up to four speed upgrades.

Oil collects in an internal tank, and Ala Industrial pipes pull it without a wrench, straight to the distillation
column for diesel and fuel oil. The walking beam, horsehead and counterweighted cranks move with proper four-bar
linkage kinematics, because a pumpjack that cheats looks wrong. A diesel motor row lets it run on its own fuel, and
every so often an **Oil Slime** crawls out of the well.

![Pumpjack](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/06_pumpjack.jpg)

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Drill bits that wear out](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/bits.png)

Quarries need a **drill bit**: iron, diamond, netherite or PDC, each crafted from the one before. Every block wears
it, hard rock more than soft, and you can **see it happen**. Carbide inserts fall off row by row, and a dying bit
throws sparks. Pipe in **water** for +25% speed and half the wear. The Nuclear Quarry chews 400 blocks a second, so
without water its PDC bit glows red, smokes and throttles itself.

Chips of the block being drilled fly off the bit, and the **hopper on the carriage** fills with whatever the quarry
is digging.

![Bit wear](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/11_hopper.jpg)

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Offshore drilling rig](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/offshore.png)

Drop it on deep ocean and it builds itself: a 7×7 grated deck, four legs down to the seabed and a lattice derrick
with travelling block, doghouse, engine house and a **gas flare** that lights the night. It drills 24 metres into the
seabed, then produces oil forever, faster than any pumpjack.

![Offshore rig](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/17_offshore_deck.jpg)

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Drones](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/drones.png)

Drone Pads, a Drone Linker and five drones that take off, cruise above everything and land on their own:

- **Cargo:** items between two pads. Empty a quarry into a chest across the map, no pipes.
- **Tanker:** 16 buckets of any fluid. Offshore oil, delivered to the coast.
- **Power:** a flying 400k EU battery for machines that are off the grid.
- **Maintenance:** swaps worn drill bits on nearby quarries and rigs, and scrubs fouled Ala Industrial distillation
  columns.
- **Survey:** sweeps a 128-block radius and brings back a report with every oil geyser it saw. Up to four **range
  upgrades** per pad widen the sweep by 64 blocks each.

The pad at the other end drops the cargo into any chest, tank or machine touching it. The pad at the start pulls
from any chest touching it. Plumbing optional.

![Drone pads](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/14_drone_pads.jpg)

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Drone Control Station](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/console.png)

A radar-blue console that shows your whole operation on one screen. The **Map** tab draws the pads, their links, the
drones in flight (with the survey route left to fly), the geysers found so far and the terrain under all of it. The
**Drones** tab lists every pad with its status, energy and destination, and the **Wells** tab lists every geyser a
surveyor has seen.

Click a pad to select it: you get a card with its status, link and energy, plus its **range ring** on the map,
shaped by whatever upgrades it carries. From there you can link it to another pad or send the surveyor out, from
across the base.

![The Map tab](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/30_console_map.png)

The console sees 1024 blocks around it. **Radar Repeaters** add their own radius wherever the network already
reaches, so a chain of them covers any distance.

![Drone Control Station map](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/32_console_selected.png)

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Ala Industrial, all the way](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/ala.png)

- **Diesel Generator** burns Ala's diesel, fuel oil or biofuel for up to 128 EU/t. Pumpjack, distillation, diesel,
  power: the loop closes.
- **Steam Turbine** caps an Ala **Steam Nozzle** and turns a reactor's wasted steam into EU.
- **Ore Seeker** upgrade, crafted from Ala's drill modules: the drill head only breaks ores.
- **Chunk Loaders** for pads, so long routes keep flying (they respect the `chunkLoading` setting).

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Manual, sounds, advancements](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/manual.png)

An in-game **Operator's Manual** (ten chapters, English and Spanish) arrives with your first quarry. The machines
have voices of their own: drill, diesel engines, turbine, drones, the pumpjack's stroke and a stall alarm for when
something stops. There are **14 advancements** to chase.

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Upgrades and GUI](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/upgrades.png)

**Area, Speed, Fortune I–III, Silk Touch, Ore Seeker** and a **Void filter** for junk blocks. The quarry GUI shows
energy, coolant, bit heat, live EU/t input (per port on the Nuclear Quarry), the current layer, progress and a **live
minimap** of the layer being dug.

![Quarry GUI](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/shots/09_gui.png)

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![Requirements](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/requirements.png)

- Minecraft **26.3**, **Fabric Loader** and **Fabric API**
- Team Reborn Energy is **bundled**
- **Ala Industrial** recommended: cables, reactors, oil, diesel and steam. Without it, recipes fall back to vanilla
  ingredients and the oil machines stay idle, since there is no oil to find.
- Optional: **Jade** (machine info when you look at a block), **JEI** or **REI** (recipes and an info page per
  machine)

Almost everything is tunable in `config/quarryindustrial.json`: speeds, ranges, costs, chunk loading and more. The
file is rewritten with new options on every update and keeps the values you changed.

![](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/divider.png)

![En español](https://raw.githubusercontent.com/BDKX16/QuarryIndustrial/main/docs/curseforge/headers/spanish.png)

**Cavá más hondo de lo que cualquier pico podría.** Quarries animadas que escalan con tu energía, desde una LV hasta
una **quarry nuclear multibloque** alimentada por hasta 8 reactores de Ala Industrial. Brocas que se gastan (y se
ven gastarse) y se refrigeran con agua; cigüeñas y una plataforma offshore con petróleo infinito; generadores diésel
y de vapor; y **cinco drones** (carga, cisterna, energía, mantenimiento y explorador) que se coordinan desde una
**centralita con mapa**: recuerda el terreno que recorriste, muestra el alcance de cada dron según sus mejoras y se
amplía con radares repetidores. El dron de mantenimiento hasta limpia las destilerías de Ala. Trae manual dentro del
juego en español, sonidos propios y 14 logros. Funciona con EU (Team Reborn Energy).
