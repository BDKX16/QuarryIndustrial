"""Escribe el contenido del Manual del operador (assets/quarryindustrial/manual/<idioma>.json).

Uso:  python tools/gen_manual.py
Cada capítulo tiene un ícono y páginas; la pantalla parte sola las páginas largas en varias hojas.
"""
import json
from pathlib import Path

from lang_es import to_tu

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/quarryindustrial/manual"
Q = "quarryindustrial:"
A = "alaindustrial:"

# (id, ícono, título en, título es, [(título en, título es, texto en, texto es, [items])])
CHAPTERS = [
    ("start", Q + "manual", "Getting Started", "Primeros pasos", [
        ("What this mod does", "Qué hace este mod",
         "Quarry Industrial adds automatic mining and oil extraction that scale with your power grid: quarries "
         "from LV to a multi-reactor Nuclear Quarry, oil pumpjacks, offshore rigs, generators and a fleet of "
         "drones to move everything around.\n\nIt is built for Ala Industrial, but it runs on plain EU (Team Reborn "
         "Energy), so any compatible cable, generator or battery works.",
         "Quarry Industrial agrega minería automática y extracción de petróleo que escalan con tu red de energía: "
         "quarries desde LV hasta una quarry nuclear con varios reactores, cigüeñas petroleras, plataformas "
         "offshore, generadores y una flota de drones para mover todo.\n\nEstá hecho para Ala Industrial, pero "
         "funciona con EU (Team Reborn Energy), así que sirve cualquier cable, generador o batería compatible.",
         [Q + "quarry_lv", Q + "pumpjack"]),
        ("Your first quarry", "Tu primera quarry",
         "1. Craft a Quarry (LV) and an Iron Tricone Bit.\n2. Place the quarry facing you: it mines the square "
         "behind it.\n3. Right-click it with the bit to install it.\n4. Connect a cable (32 EU/t is enough).\n"
         "5. Put a chest next to it: the drops go there.\n\nThe quarry first builds its frame, then starts digging.",
         "1. Fabricá una Quarry (LV) y una broca tricónica de hierro.\n2. Poné la quarry mirando hacia vos: mina "
         "el cuadrado de atrás.\n3. Clic derecho con la broca para instalarla.\n4. Conectá un cable (32 EU/t "
         "alcanzan).\n5. Poné un cofre al lado: ahí van los drops.\n\nLa quarry primero arma su marco y después "
         "empieza a excavar.",
         [Q + "drill_bit_iron", "minecraft:chest"]),
        ("Recipes", "Recetas",
         "Every item has two recipes. With Ala Industrial installed they use its circuits, casings and plates; "
         "without it, vanilla equivalents. JEI and REI show the active one, plus an information page for each "
         "machine.",
         "Cada item tiene dos recetas. Con Ala Industrial usan sus circuitos, carcasas y placas; sin Ala, "
         "equivalentes vanilla. JEI y REI muestran la que corresponde, y una página de información de cada máquina.",
         []),
    ]),
    ("quarries", Q + "quarry_hv", "Quarries", "Quarries", [
        ("Tiers", "Tiers",
         "LV: 32 EU/t, 9x9 to 16x16, 1 head, 5 blocks/s\nMV: 128 EU/t, 16x16 to 48x48, 1 head, 20 blocks/s\n"
         "HV: 512 EU/t, 32x32 to 80x80, 2 heads, 60 blocks/s\nNuclear: 512 EU/t per port (1-8), 64x64 to 144x144, "
         "4 heads, 400 blocks/s\n\nSpeed comes from power: every block costs 50 x (1 + hardness) EU. An underfed "
         "quarry just digs slower.",
         "LV: 32 EU/t, 9x9 a 16x16, 1 cabezal, 5 bloques/s\nMV: 128 EU/t, 16x16 a 48x48, 1 cabezal, 20 bloques/s\n"
         "HV: 512 EU/t, 32x32 a 80x80, 2 cabezales, 60 bloques/s\nNuclear: 512 EU/t por puerto (1-8), 64x64 a "
         "144x144, 4 cabezales, 400 bloques/s\n\nLa velocidad sale de la energía: cada bloque cuesta 50 x (1 + "
         "dureza) EU. Con poca energía, simplemente mina más lento.",
         [Q + "quarry_lv", Q + "quarry_mv", Q + "quarry_hv"]),
        ("Frame and drill string", "Marco y sarta",
         "The quarry raises a steel frame one block outside its area, 4 blocks high. A bridge rolls along the "
         "frame, a carriage rolls along the bridge and a real drill string hangs from it: kelly, drill pipe and "
         "the bit at the bottom. The hopper on the carriage fills up with what is being mined.",
         "La quarry levanta un marco de acero un bloque por fuera del área, de 4 de alto. Un puente corre sobre "
         "el marco, un carro sobre el puente y de él cuelga una sarta de verdad: kelly, tubos y la broca abajo. La "
         "tolva del carro se va llenando con lo que se mina.",
         []),
        ("Output, claims and chunks", "Salida, claims y chunks",
         "Drops go to a 27-slot buffer that empties into any chest or pipe next to the quarry. If it fills up, the "
         "quarry stops and an alarm sounds.\n\nBlocks are broken by a fake player with the owner's UUID, so claim "
         "mods are respected. Only the quarry's chunk and the chunks under the drill heads are kept loaded.\n\n"
         "Fluids inside the area are removed; fluids touching the border are dammed with cobblestone.",
         "Los drops van a un buffer de 27 slots que se vacía en cualquier cofre o caño pegado. Si se llena, la "
         "quarry se para y suena una alarma.\n\nLos bloques los rompe un jugador falso con el UUID del dueño, así "
         "que se respetan los mods de claims. Solo quedan cargados el chunk de la quarry y los de los cabezales.\n\n"
         "Los fluidos dentro del área se borran; los que tocan el borde se tapan con adoquín.",
         ["minecraft:chest"]),
    ]),
    ("bits", Q + "drill_bit_diamond", "Drill Bits & Cooling", "Brocas y refrigeración", [
        ("Drill bits", "Brocas",
         "A quarry does not mine without a bit (it builds the frame anyway). Each bit is crafted from the "
         "previous one:\nIron: speed x1.0, life 6000\nDiamond: x1.3, 24000\nNetherite: x1.6, 80000\nPDC: x2.0, "
         "200000\n\nA block wears 1 + hardness / 3 points: stone 1, deepslate 2, obsidian 17. Look at the bit "
         "while it works: the carbide inserts disappear row by row, and a nearly dead bit throws sparks.",
         "Sin broca la quarry no mina (el marco lo arma igual). Cada broca se fabrica con la anterior:\nHierro: "
         "velocidad x1.0, vida 6000\nDiamante: x1.3, 24000\nNetherite: x1.6, 80000\nPDC: x2.0, 200000\n\nCada "
         "bloque gasta 1 + dureza / 3 puntos: piedra 1, pizarra profunda 2, obsidiana 17. Mirá la broca mientras "
         "trabaja: los insertos de carburo se van cayendo fila por fila y una broca casi muerta tira chispas.",
         [Q + "drill_bit_iron", Q + "drill_bit_diamond", Q + "drill_bit_netherite", Q + "drill_bit_pdc"]),
        ("Water cooling", "Refrigeración con agua",
         "Pipe water into the quarry (any face; into the core for the Nuclear Quarry). Each block uses 1 mB and "
         "gives +25% speed and half the bit wear. An Ala Industrial pump on a water source is enough.",
         "Entrá agua por caño a la quarry (por cualquier cara; en la nuclear, por el núcleo). Cada bloque gasta "
         "1 mB y da +25 % de velocidad y la mitad de desgaste. Con una bomba de Ala sobre una fuente de agua alcanza.",
         ["minecraft:water_bucket"]),
        ("Heat", "Calor",
         "The Nuclear Quarry chews 400 blocks per second: without water its PDC bit heats up. Above 70% heat it "
         "slows down (down to 20% speed), the bit glows red and smokes. With water it never overheats.",
         "La quarry nuclear muele 400 bloques por segundo: sin agua la broca PDC se calienta. Arriba del 70 % de "
         "calor frena (hasta el 20 % de velocidad), la broca se pone al rojo y echa humo. Con agua nunca se recalienta.",
         [Q + "quarry_core"]),
        ("Replacing bits", "Cambiar brocas",
         "Right-click the quarry with a new bit, or use the bit slot in its GUI. A Maintenance Drone can do it "
         "for you (see the Drones chapter).",
         "Clic derecho con una broca nueva o el slot de broca de la GUI. Un dron de mantenimiento lo puede hacer "
         "solo (ver el capítulo de drones).",
         [Q + "maintenance_drone"]),
    ]),
    ("upgrades", Q + "upgrade_speed", "Upgrades", "Upgrades", [
        ("Quarry upgrades", "Upgrades de la quarry",
         "Area: +16 per side, up to the tier max (rebuilds the frame).\nSpeed: +50% blocks/s, x1.25 cost (up to 4).\n"
         "Fortune I-III: fortune drops, +50% cost per level.\nSilk Touch: silk drops, x2 cost.\nVoid: destroys "
         "junk (stone, dirt, gravel...).\nOre Seeker: the head only breaks ores and leaves the rock in place.\n\n"
         "They go in the side panel of the GUI, or right-click the quarry with one. Sneak + right-click takes the "
         "last one out.",
         "Área: +16 por lado, hasta el máximo del tier (rehace el marco).\nVelocidad: +50 % bloques/s, x1.25 de "
         "coste (hasta 4).\nFortuna I-III: drops con fortuna, +50 % de coste por nivel.\nToque de seda: drops con "
         "seda, x2 de coste.\nFiltro: destruye la basura (piedra, tierra, grava...).\nBuscavetas: el cabezal solo "
         "rompe minerales y deja la roca.\n\nVan en el panel lateral de la GUI, o clic derecho con uno. Agachado + "
         "clic saca el último.",
         [Q + "upgrade_area", Q + "upgrade_speed", Q + "upgrade_ore_seeker"]),
    ]),
    ("nuclear", Q + "quarry_core", "Nuclear Quarry", "Quarry nuclear", [
        ("Why", "Para qué",
         "Ala Industrial's thickest cable and its reactor top out at 512 EU/t. The Nuclear Quarry breaks that "
         "ceiling with energy ports: one reactor per port, up to 8.",
         "El cable más grueso de Ala Industrial y su reactor llegan a 512 EU/t. La quarry nuclear rompe ese techo "
         "con puertos de energía: un reactor por puerto, hasta 8.",
         [Q + "energy_port"]),
        ("Building it", "Cómo se arma",
         "1. Place the Nuclear Quarry Core facing you.\n2. Behind it, build a 3x3x3 cube: the core is the centre "
         "of the front face; the other 26 blocks are Quarry Casing or HV Energy Ports (1 to 8).\n3. Missing blocks "
         "show up as a green hologram.\n\nThe area starts behind the cube. Pipe water into the core to keep it cool.",
         "1. Poné el Núcleo de quarry nuclear mirando hacia vos.\n2. Detrás, armá un cubo de 3x3x3: el núcleo es el "
         "centro de la cara delantera; los otros 26 bloques son Carcasa de quarry o Puertos de energía HV (de 1 a "
         "8).\n3. Lo que falta se ve como un holograma verde.\n\nEl área empieza detrás del cubo. Entrá agua al "
         "núcleo para enfriarla.",
         [Q + "quarry_core", Q + "quarry_casing", Q + "energy_port"]),
    ]),
    ("oil", Q + "pumpjack", "Oil Pumpjacks", "Cigüeñas petroleras", [
        ("Geysers", "Géiseres",
         "Ala Industrial generates oil geysers: a jet of oil on the surface, a 1x1 shaft sealed with stone and a "
         "buried dome full of crude around Y -45. A Survey Drone finds them for you.",
         "Ala Industrial genera géiseres de petróleo: un chorro en la superficie, un caño de 1x1 sellado con piedra "
         "y un domo enterrado lleno de crudo cerca de Y -45. Un dron explorador los encuentra por vos.",
         [Q + "survey_drone"]),
        ("Placing it", "Cómo se pone",
         "Stand behind the shaft and place the pumpjack facing it: the 3x7x5 structure builds itself towards "
         "you. It first drains the real oil in the shaft and dome (1 bucket per second), then pumps forever at "
         "10 mB/t for 32 EU/t, up to ~30 mB/t with 4 speed upgrades.",
         "Parate detrás del caño y poné la cigüeña mirando hacia él: la estructura de 3x7x5 se arma hacia vos. "
         "Primero vacía el crudo real del caño y el domo (1 cubo por segundo) y después bombea para siempre a "
         "10 mB/t por 32 EU/t, hasta ~30 mB/t con 4 upgrades de velocidad.",
         [Q + "pumpjack", Q + "upgrade_speed"]),
        ("Pipes and diesel", "Caños y diésel",
         "Ala pipes attached to the base pull the oil by themselves (no wrench needed). Send it to Ala's "
         "distillation column for diesel and fuel oil.\n\nPipe diesel into the motor row (the back of the base) "
         "and the pumpjack runs on its own diesel when it lacks EU: black smoke comes out of the exhaust.\n\n"
         "Now and then an Oil Slime crawls out of the well.",
         "Los caños de Ala pegados a la base sacan el petróleo solos (sin llave). Mandalo a la columna de "
         "destilación de Ala para diésel y fuel oil.\n\nCon diésel por la fila del motor (atrás de la base), la "
         "cigüeña anda con su propio diésel cuando le falta EU: sale humo negro del escape.\n\nCada tanto sale un "
         "Oil Slime del pozo.",
         [A + "fluid_pipe", Q + "diesel_generator"]),
    ]),
    ("offshore", Q + "offshore_rig", "Offshore Rig", "Plataforma offshore", [
        ("Placing it", "Cómo se pone",
         "Use it on the surface of an ocean at least 8 blocks deep. It builds a 7x7 grated deck at sea level and "
         "four legs down to the seabed. The derrick, the flare boom and the engine house are part of it.",
         "Usala sobre la superficie de un océano de al menos 8 bloques de profundidad. Arma una cubierta de rejilla "
         "de 7x7 a nivel del mar y cuatro patas hasta el fondo. La torre, la antorcha y la sala de máquinas vienen "
         "incluidas.",
         [Q + "offshore_rig"]),
        ("Drilling and production", "Perforación y producción",
         "Install a drill bit (right-click the deck with it) and give it power: the drill string goes down through "
         "the water and 24 m into the seabed (256 EU/t, 50 wear points per metre). Once it reaches the reservoir "
         "it produces 25 mB/t of oil forever (96 EU/t), more with speed upgrades, and the gas flare lights up.",
         "Instalale una broca (clic derecho en la cubierta) y dale energía: la sarta baja por el agua y 24 m dentro "
         "del lecho marino (256 EU/t, 50 puntos de desgaste por metro). Al llegar al reservorio produce 25 mB/t de "
         "petróleo para siempre (96 EU/t), más con upgrades de velocidad, y se enciende la antorcha.",
         [Q + "drill_bit_diamond", Q + "upgrade_speed"]),
        ("Getting the oil ashore", "Llevar el petróleo a tierra",
         "Oil comes out of any deck block. Running pipes across the sea is tedious: put a Drone Pad on the deck "
         "with a Tanker Drone and link it to a pad on the coast. Diesel goes in through the rig's controller, so "
         "it can run on its own oil once refined.",
         "El petróleo sale por cualquier bloque de la cubierta. Llevar caños por el mar es tedioso: poné un pad "
         "con un dron cisterna sobre la cubierta y vinculalo a un pad en la costa. El diésel entra por el "
         "controlador, así la plataforma puede andar con su propio petróleo refinado.",
         [Q + "tanker_drone", Q + "drone_pad"]),
    ]),
    ("power", Q + "diesel_generator", "Generators", "Generadores", [
        ("Diesel Generator", "Generador diésel",
         "Burns Ala Industrial's liquid fuels and outputs up to 128 EU/t (MV) to adjacent cables and machines:\n"
         "Diesel: 12 EU per mB\nFuel oil: 8 EU per mB\nBiofuel: 8 EU per mB\n\nRight-click it with a bucket or "
         "pipe fuel in. It closes the loop: pumpjack, distillation, diesel, power.",
         "Quema los combustibles líquidos de Ala Industrial y entrega hasta 128 EU/t (MV) a cables y máquinas "
         "pegadas:\nDiésel: 12 EU por mB\nFuel oil: 8 EU por mB\nBiocombustible: 8 EU por mB\n\nClic derecho con "
         "un balde o con caño. Cierra el ciclo: cigüeña, destilación, diésel, energía.",
         [Q + "diesel_generator"]),
        ("Steam Turbine", "Turbina de vapor",
         "Ala Industrial's reactor boils its coolant into steam, which goes through steam pipes to a Steam Nozzle "
         "that vents it into the air. Place the turbine right in front of the nozzle's outlet: it caps the vent "
         "and turns the steam into EU (2 EU per mB, up to 1024 EU/t).",
         "El reactor de Ala Industrial hierve su refrigerante en vapor, que va por caños de vapor a una Steam "
         "Nozzle que lo ventea al aire. Poné la turbina justo delante de la boca de la nozzle: tapa el venteo y "
         "convierte el vapor en EU (2 EU por mB, hasta 1024 EU/t).",
         [Q + "steam_turbine", A + "steam_nozzle"]),
    ]),
    ("drones", Q + "cargo_drone", "Drones", "Drones", [
        ("Drone pads", "Pads de drones",
         "A drone lives on a Drone Pad: right-click the pad with the drone. The pad has an outbox (what the drone "
         "takes; pipes and hoppers put things in and it pulls from chests and tanks next to it) and an inbox "
         "(what other drones bring; it pushes it into its neighbours). Every trip costs energy from the pad: "
         "1000 EU plus 8 EU per block.",
         "Un dron vive en un pad: clic derecho en el pad con el dron. El pad tiene un buzón de salida (lo que se "
         "lleva el dron; caños y tolvas meten cosas y tira de cofres y tanques pegados) y uno de llegada (lo que "
         "traen otros drones, que reparte a sus vecinos). Cada viaje cuesta energía del pad: 1000 EU más 8 EU por "
         "bloque.",
         [Q + "drone_pad"]),
        ("Linking", "Vincular",
         "Sneak + right-click the source pad with the Drone Linker, then right-click the destination pad (up to "
         "3000 blocks away). Transport drones fly from source to destination and back.",
         "Con el vinculador, agachado + clic derecho en el pad de origen y después clic derecho en el de destino "
         "(hasta 3000 bloques). Los drones de transporte van del origen al destino y vuelven.",
         [Q + "drone_linker"]),
        ("Transport drones", "Drones de transporte",
         "Cargo Drone: items (leaves with 4 stacks or after 10 s).\nTanker Drone: fluids, 16 buckets.\nPower "
         "Drone: a flying 400k EU battery.\n\nWhat the destination cannot take comes back with the drone.",
         "Dron de carga: items (sale con 4 stacks o a los 10 s).\nDron cisterna: fluidos, 16 cubos.\nDron de "
         "energía: una batería voladora de 400k EU.\n\nLo que el destino no puede recibir vuelve con el dron.",
         [Q + "cargo_drone", Q + "tanker_drone", Q + "power_drone"]),
        ("Maintenance and survey", "Mantenimiento y exploración",
         "Maintenance Drone: put spare bits in its pad. It watches quarries and offshore rigs within 512 blocks "
         "(up to 2048 with Survey Range Upgrades) and swaps a bit when it is 90% worn or gone, bringing the old "
         "one back.\n\nSurvey Drone: right-click its pad to launch it. It sweeps a 128-block radius and brings back "
         "a Survey Report with every oil geyser it saw and every oil lake. Geysers are infinite: a pumpjack drains "
         "them forever. Lakes are finite surface pools.",
         "Dron de mantenimiento: poné brocas de repuesto en su pad. Vigila las quarries y plataformas a 512 "
         "bloques (hasta 2048 con mejoras de alcance) y cambia la broca cuando está gastada al 90 % o falta, y "
         "trae la vieja.\n\nDron explorador: clic derecho en su pad para mandarlo. Recorre un radio de 128 bloques "
         "y vuelve con un informe de todos los géiseres y lagos de petróleo que vio. Los géiseres son infinitos: "
         "una cigüeña los bombea para siempre. Los lagos son charcos finitos de superficie.",
         [Q + "maintenance_drone", Q + "survey_drone", Q + "survey_report"]),
        ("Long-range surveys", "Exploración de largo alcance",
         "Survey Range Upgrade: right-click a pad with it. Each one adds 64 blocks to the sweep (up to 17 per pad: "
         "1216 blocks of radius) and about 90 to the Maintenance Drone's reach (2040 with all 17).\n\nYou can also "
         "send the Survey Drone somewhere else: in the Drone Control Station press Travel and click the map. It "
         "flies there (up to 10,000 blocks from its pad), sweeps the area and comes back with the report. The trip "
         "costs 1 EU per block plus the sweep, taken from the pad: the biggest sweep needs about 500,000 EU, "
         "which is what a pad stores. If the pad does not have enough yet, the drone leaves as soon as it "
         "charges.\n\nLakes run dry, so a base that lasts keeps needing fresh oil fields: with a charged pad and "
         "all the upgrades you can keep finding them, however far they are.",
         "Mejora de alcance del explorador: clic derecho en un pad con ella. Cada una suma 64 bloques al barrido "
         "(hasta 17 por pad: 1216 bloques de radio) y unos 90 al alcance del dron de mantenimiento (2040 con las "
         "17).\n\nTambién podés mandar al explorador a otro lado: en la centralita de drones apretá Viajar y hacé "
         "clic en el mapa. Vuela hasta ahí (hasta 10.000 bloques de su pad), barre la zona y vuelve con el "
         "informe. El viaje cuesta 1 EU por bloque más el barrido, que sale de la energía del pad: el barrido más "
         "grande pide unos 500.000 EU, que es lo que guarda un pad. Si todavía no le alcanza, el dron sale apenas "
         "se cargue.\n\nLos lagos se secan, así que una base que dura necesita yacimientos nuevos: con el pad "
         "cargado y todas las mejoras podés seguir encontrándolos, por lejos que estén.",
         [Q + "survey_range_upgrade", Q + "survey_drone", Q + "drone_console"]),
        ("Drone Control Station", "Centralita de drones",
         "Right-click the station to open a map of every pad within 1024 blocks. The map shows the terrain you "
         "have explored (it remembers it), each pad's link as a dashed line, drones in flight (with the survey "
         "drone's remaining sweep), the range ring of the selected pad, and every oil geyser (violet drop) and lake "
         "(grey pool) your survey drones found.\n\nClick a pad to select it, then Link and click the destination "
         "pad. The Drones tab lists every pad with its status; the Wells tab lists the geysers and lakes. Press "
         "Travel to send the survey drone to any spot of the map. Radar Repeaters extend the station's coverage.",
         "Clic derecho en la centralita abre un mapa de todos los pads a menos de 1024 bloques. Muestra el "
         "terreno que exploraste (lo recuerda), el vínculo de cada pad como una línea a trazos, los drones en "
         "vuelo (con lo que le falta del barrido al explorador), el anillo de alcance del pad elegido y cada "
         "géiser (gota violeta) y lago (charco gris) que encontraron tus exploradores.\n\nClic en un pad para "
         "elegirlo, después Vincular y clic en el pad de destino. La pestaña Drones lista todos los pads con su "
         "estado; la de Pozos, los géiseres y lagos. Apretá Viajar para mandar al explorador a cualquier punto "
         "del mapa. Los radares repetidores amplían la cobertura de la centralita.",
         [Q + "drone_console", Q + "drone_radar"]),
        ("If something goes wrong", "Si algo sale mal",
         "Drones fly above everything and keep the chunk they are in loaded. Hit a drone to bring it down: it "
         "drops what it carried and reappears on its pad. If a pad hears nothing from its drone for 5 minutes, it "
         "gives it up for lost and the drone comes back (without its cargo).",
         "Los drones vuelan por arriba de todo y mantienen cargado el chunk donde están. Si golpeás un dron, cae: "
         "suelta lo que llevaba y vuelve a aparecer en su pad. Si un pad no sabe nada de su dron por 5 minutos, lo "
         "da por perdido y el dron vuelve (sin la carga).",
         []),
        ("Energy Distributor", "Distribuidor de energía",
         "Splits the EU of a cable equally among everything touching it: cables, drone pads and machines. Energy "
         "only comes in through its input arm, the one with the amber collar (it faces the block you placed it "
         "against), and leaves through the other five sides, so an output can never be mistaken for an input. If one "
         "destination fills up, the rest goes to the others: with several pads on one reactor's output, the nearest "
         "no longer fills first.\n\nDistributors chain (an output into the next one's amber arm, touching or with a "
         "cable between) and cables draw an arm to them. It stores 8192 EU and moves up to 2048 EU/t. Cheap to "
         "make: 2 electrum cables, an electronic circuit and yellow concrete.",
         "Reparte los EU de un cable en partes iguales entre todo lo que tenga pegado: cables, pads de drones y "
         "máquinas. La energía entra solo por su brazo de entrada, el del collar ámbar (queda del lado del bloque "
         "contra el que lo pusiste), y sale por los otros cinco lados, así que una salida nunca se confunde con "
         "una entrada. Si un destino se llena, lo que sobra va a los otros: con varios pads en la salida de un "
         "reactor, ya no se llena primero el más cercano.\n\nSe pueden encadenar (una salida al brazo ámbar del "
         "siguiente, pegados o con un cable en el medio) y los cables dibujan un brazo hacia ellos. Guarda 8192 EU "
         "y mueve hasta 2048 EU/t. Barato de hacer: 2 cables de electrum, un circuito electrónico y hormigón "
         "amarillo.",
         [Q + "energy_splitter"]),
    ]),
    ("filters", Q + "pipe_filter", "Pipe Filter", "Filtro de caño", [
        ("Pipe Filter", "Filtro de caño",
         "Clamps onto the arm of an Ala Industrial item pipe (basic or advanced) that touches an inventory: "
         "right-click the arm with the filter in hand. From then on that side only lets the items you mark go "
         "through, both when the pipe pulls from the inventory and when it pushes into it. It needs no energy "
         "and you can see it on the pipe: an iron sleeve with a cyan band.\n\nRight-click it with an empty hand to "
         "open its screen; put an item on a slot to mark it (nothing is consumed). Sneak + right-click takes it "
         "off with its settings, and it drops if the pipe breaks. It only works with Ala Industrial's item pipes.",
         "Se engancha al brazo de un caño de ítems de Ala Industrial (común o avanzado) que toque un inventario: "
         "clic derecho en el brazo con el filtro en la mano. Desde ahí ese lado solo deja pasar los ítems que "
         "marques, tanto cuando el caño saca del inventario como cuando le mete. No usa energía y se ve en el "
         "caño: un manguito de hierro con una banda celeste.\n\nClic derecho con la mano vacía abre su pantalla; "
         "poné un ítem en un casillero para marcarlo (no se gasta). Agachado + clic derecho lo saca con su "
         "configuración, y se suelta si el caño se rompe. Solo funciona con los caños de ítems de Ala Industrial.",
         [Q + "pipe_filter", A + "item_pipe", A + "item_pipe_advanced"]),
        ("Expansion and lists", "Expansión y listas",
         "Out of the box the filter has one slot. The Pipe Filter Expansion goes in its own panel on the right of "
         "the screen and unlocks all five.\n\nWith one slot the button reads Allow / Deny; with the expansion it "
         "becomes an Allow list / Deny list. Allow lets through only what is marked; Deny lets through everything "
         "except what is marked. An empty allow list lets nothing through, an empty deny list lets everything "
         "through. Both filter and expansion are crafted with an Ala Industrial Empty Chip.",
         "De fábrica el filtro tiene un casillero. La Expansión de filtro va en su propio panel a la derecha de la "
         "pantalla y desbloquea los cinco.\n\nCon un casillero el botón dice Permitir / Denegar; con la expansión "
         "pasa a Lista blanca / Lista negra. Permitir deja pasar solo lo marcado; Denegar deja pasar todo menos "
         "lo marcado. Una lista blanca vacía no deja pasar nada y una negra vacía lo deja pasar todo. El filtro y "
         "la expansión se fabrican con un Chip vacío de Ala Industrial.",
         [Q + "pipe_filter_upgrade", A + "empty_chip"]),
        ("By item or by category", "Por ítem o por categoría",
         "By item matches only the exact item you marked. By category matches every item that shares a category "
         "(a tag) with it: mark an oak log and every kind of log goes through.\n\nAn item can belong to several "
         "categories: click its slot to switch which one is used. While you are in category mode, a panel on the "
         "right lists every item in the chosen category (scroll to see more) and hovering a slot shows its own. "
         "Each button explains itself in a tooltip.",
         "Por ítem coincide solo con el ítem exacto que marcaste. Por categoría coincide con todo ítem que "
         "comparta una categoría (un tag) con él: marcás un tronco de roble y pasa cualquier tipo de "
         "tronco.\n\nUn ítem puede tener varias categorías: clic en su casillero para cambiar cuál se usa. En modo "
         "categoría, un panel a la derecha lista todos los ítems de la categoría elegida (rueda para ver más) y "
         "al pasar el mouse por un casillero muestra la suya. Cada botón se explica en un tooltip.",
         [Q + "pipe_filter", "minecraft:oak_log"]),
    ]),
    ("extras", "minecraft:spyglass", "Tips & Compatibility", "Consejos y compatibilidad", [
        ("Jade, JEI, REI", "Jade, JEI, REI",
         "With Jade installed, looking at any machine shows its status, energy, bit life, fluids and progress. "
         "JEI and REI show every recipe plus an information page per machine.",
         "Con Jade, mirar cualquier máquina muestra su estado, energía, vida de la broca, fluidos y avance. JEI y "
         "REI muestran todas las recetas y una página de información por máquina.",
         []),
        ("Configuration", "Configuración",
         "Every number is in config/quarryindustrial.json: costs, tiers, bit life, cooling, generators, drones, "
         "the offshore rig and the pumpjack. Missing options are added with their defaults when the game starts.",
         "Todos los números están en config/quarryindustrial.json: costes, tiers, vida de las brocas, "
         "refrigeración, generadores, drones, la plataforma offshore y la cigüeña. Las opciones que falten se "
         "agregan con su valor por defecto al arrancar.",
         []),
    ]),
]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for code, es in (("en_us", False), ("es_ar", True), ("es_es", True)):
        convert = to_tu if code == "es_es" else str
        chapters = []
        for cid, icon, title_en, title_es, pages in CHAPTERS:
            chapters.append({
                "id": cid,
                "title": convert(title_es if es else title_en),
                "icon": icon,
                "pages": [{"title": convert(p_es if es else p_en), "text": convert(t_es if es else t_en),
                           "items": items}
                          for p_en, p_es, t_en, t_es, items in pages],
            })
        (OUT / f"{code}.json").write_text(json.dumps({"chapters": chapters}, indent=2, ensure_ascii=False) + "\n",
                                          encoding="utf-8")
    print(f"{len(CHAPTERS)} capítulos")


if __name__ == "__main__":
    main()
