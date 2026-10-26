"""Agrega (o actualiza) las traducciones de la expansión en en_us, es_ar y es_es.

Uso:  python tools/gen_lang.py
Las claves que ya existen en los archivos y no están acá quedan como están.
"""
import json
from pathlib import Path

from lang_es import to_tu

LANG = Path(__file__).resolve().parent.parent / "src/main/resources/assets/quarryindustrial/lang"

# clave: (inglés, español)
KEYS = {
    # ── Brocas, refrigerante, calor ──
    "item.quarryindustrial.drill_bit_iron": ("Iron Tricone Bit", "Broca tricónica de hierro"),
    "item.quarryindustrial.drill_bit_diamond": ("Diamond Tricone Bit", "Broca tricónica de diamante"),
    "item.quarryindustrial.drill_bit_netherite": ("Netherite Tricone Bit", "Broca tricónica de netherite"),
    "item.quarryindustrial.drill_bit_pdc": ("PDC Bit", "Broca PDC"),
    "tooltip.quarryindustrial.bit.speed": ("Drilling speed: %s%%", "Velocidad de perforación: %s%%"),
    "tooltip.quarryindustrial.bit.life": ("Life: %s / %s", "Vida: %s / %s"),
    "item.quarryindustrial.upgrade_ore_seeker": ("Ore Seeker Upgrade", "Upgrade buscavetas"),
    "status.quarryindustrial.no_bit": ("No drill bit", "Sin broca"),
    "status.quarryindustrial.overheating": ("Overheating", "Recalentada"),
    "message.quarryindustrial.bit.installed": ("Bit installed: %s", "Broca instalada: %s"),
    "gui.quarryindustrial.coolant": ("Coolant: %s / %s mB of water", "Refrigerante: %s / %s mB de agua"),
    "gui.quarryindustrial.coolant_hint": ("Pipe water in: +25%% speed, half the bit wear",
                                          "Con agua por caño: +25 %% de velocidad y la mitad de desgaste"),
    "gui.quarryindustrial.heat": ("Bit heat: %s%%", "Calor de la broca: %s %%"),
    "gui.quarryindustrial.heat_hint": ("Above 70%% the drill slows down. Water keeps it cool.",
                                       "Arriba del 70 %% la quarry frena. El agua la enfría."),
    # ── Generadores y cigüeña diésel ──
    "block.quarryindustrial.diesel_generator": ("Diesel Generator", "Generador diésel"),
    "block.quarryindustrial.steam_turbine": ("Steam Turbine", "Turbina de vapor"),
    "status.quarryindustrial.generator.running": ("Running", "Funcionando"),
    "status.quarryindustrial.generator.idle": ("Idle", "Parado"),
    "message.quarryindustrial.diesel_generator.status": (
        "%s · %s EU/t · Energy: %s / %s EU · Fuel: %s / %s mB",
        "%s · %s EU/t · Energía: %s / %s EU · Combustible: %s / %s mB"),
    "message.quarryindustrial.steam_turbine.status": (
        "%s · %s EU/t · Energy: %s / %s EU · Steam: %s / %s mB",
        "%s · %s EU/t · Energía: %s / %s EU · Vapor: %s / %s mB"),
    "message.quarryindustrial.pumpjack.status": (
        "%s · Energy: %s / %s EU · Oil: %s / %s mB · Speed upgrades: %s · Diesel: %s / %s mB",
        "%s · Energía: %s / %s EU · Petróleo: %s / %s mB · Upgrades de velocidad: %s · Diésel: %s / %s mB"),
    # ── Plataforma offshore ──
    "block.quarryindustrial.offshore_rig": ("Offshore Drilling Rig", "Plataforma petrolera offshore"),
    "block.quarryindustrial.offshore_deck": ("Offshore Rig Deck", "Cubierta de plataforma offshore"),
    "block.quarryindustrial.offshore_leg": ("Offshore Rig Leg", "Pata de plataforma offshore"),
    "status.quarryindustrial.offshore.idle": ("Idle", "En espera"),
    "status.quarryindustrial.offshore.no_oil_fluid": ("No oil fluid (#c:oil) installed", "No hay fluido de petróleo (#c:oil)"),
    "status.quarryindustrial.offshore.damaged": ("Damaged: a deck block or leg is missing", "Dañada: falta una pata o un bloque de la cubierta"),
    "status.quarryindustrial.offshore.no_power": ("No power", "Sin energía"),
    "status.quarryindustrial.offshore.no_bit": ("Drilling needs a drill bit", "Para perforar hace falta una broca"),
    "status.quarryindustrial.offshore.output_full": ("Tank full", "Tanque lleno"),
    "status.quarryindustrial.offshore.drilling": ("Drilling the seabed", "Perforando el lecho marino"),
    "status.quarryindustrial.offshore.producing": ("Producing", "Produciendo"),
    "message.quarryindustrial.offshore.status": (
        "%s · Drilled: %s / %s m · Energy: %s / %s EU · Oil: %s / %s mB · Diesel: %s mB · Speed upgrades: %s",
        "%s · Perforado: %s / %s m · Energía: %s / %s EU · Petróleo: %s / %s mB · Diésel: %s mB · Upgrades: %s"),
    "message.quarryindustrial.offshore.not_ocean": ("Offshore rigs only work in an ocean biome",
                                                     "La plataforma solo funciona en un bioma de océano"),
    "message.quarryindustrial.offshore.not_on_water": ("Place it on the water surface", "Se coloca sobre la superficie del agua"),
    "message.quarryindustrial.offshore.too_shallow": ("Too shallow: it needs at least %2$s blocks of water below the deck",
                                                       "Muy poco profundo: hacen falta al menos %2$s bloques de agua debajo"),
    "message.quarryindustrial.offshore.no_room": ("Not enough room: the deck is %1$s×%1$s above the water",
                                                   "No hay lugar: la cubierta mide %1$s×%1$s sobre el agua"),
    # ── Drones ──
    "block.quarryindustrial.drone_pad": ("Drone Pad", "Pad de drones"),
    "item.quarryindustrial.cargo_drone": ("Cargo Drone", "Dron de carga"),
    "item.quarryindustrial.tanker_drone": ("Tanker Drone", "Dron cisterna"),
    "item.quarryindustrial.power_drone": ("Power Drone", "Dron de energía"),
    "item.quarryindustrial.maintenance_drone": ("Maintenance Drone", "Dron de mantenimiento"),
    "item.quarryindustrial.survey_drone": ("Survey Drone", "Dron explorador"),
    "item.quarryindustrial.drone_linker": ("Drone Linker", "Vinculador de drones"),
    "item.quarryindustrial.survey_report": ("Survey Report", "Informe de exploración"),
    "item.quarryindustrial.manual": ("Operator's Manual", "Manual del operador"),
    "entity.quarryindustrial.drone": ("Drone", "Dron"),
    "tooltip.quarryindustrial.drone.cargo": ("Carries items from its pad to the linked pad",
                                            "Lleva items de su pad al pad vinculado"),
    "tooltip.quarryindustrial.drone.tanker": ("Carries up to 16 buckets of any fluid to the linked pad",
                                             "Lleva hasta 16 cubos de cualquier fluido al pad vinculado"),
    "tooltip.quarryindustrial.drone.power": ("A flying battery: carries EU to the linked pad",
                                            "Una batería voladora: lleva EU al pad vinculado"),
    "tooltip.quarryindustrial.drone.maintenance": ("Swaps worn drill bits on nearby quarries and rigs (spare bits go in its pad)",
                                                  "Cambia las brocas gastadas de quarries y plataformas cercanas (los repuestos van en su pad)"),
    "tooltip.quarryindustrial.drone.survey": ("Flies over the area looking for oil geysers and brings back a report",
                                             "Sobrevuela la zona buscando géiseres de petróleo y vuelve con un informe"),
    "tooltip.quarryindustrial.drone.needs_link": ("Needs a destination: use a Drone Linker",
                                                 "Necesita un destino: usá un vinculador de drones"),
    "tooltip.quarryindustrial.drone.works_alone": ("Works from its own pad", "Trabaja desde su propio pad"),
    "tooltip.quarryindustrial.linker.source": ("Source pad: %s %s %s", "Pad de origen: %s %s %s"),
    "tooltip.quarryindustrial.linker.help": ("Sneak + click: pick the source pad. Click: link it to this pad.",
                                            "Agachado + clic: elegir el pad de origen. Clic: vincularlo a este pad."),
    "tooltip.quarryindustrial.survey.none": ("No geysers found", "No se encontraron géiseres"),
    "tooltip.quarryindustrial.survey.found": ("Oil geysers found: %s", "Géiseres de petróleo: %s"),
    "tooltip.quarryindustrial.survey.site": ("• %s, %s, %s", "• %s, %s, %s"),
    "tooltip.quarryindustrial.survey.more": ("...and %s more", "...y %s más"),
    "message.quarryindustrial.linker.source": ("Source pad: %s %s %s. Now click the destination pad.",
                                               "Pad de origen: %s %s %s. Ahora hacé clic en el de destino."),
    "message.quarryindustrial.linker.linked": ("Linked %s %s %s → %s %s %s (%s blocks)",
                                               "Vinculado %s %s %s → %s %s %s (%s bloques)"),
    "message.quarryindustrial.linker.cleared": ("Link cleared", "Vínculo borrado"),
    "message.quarryindustrial.linker.no_source": ("Sneak + click the source pad first",
                                                  "Primero, agachado + clic en el pad de origen"),
    "message.quarryindustrial.linker.same": ("A pad cannot be linked to itself", "Un pad no se puede vincular consigo mismo"),
    "message.quarryindustrial.linker.too_far": ("Too far: pads can be up to %s blocks apart",
                                                "Muy lejos: los pads pueden estar a %s bloques como mucho"),
    "message.quarryindustrial.linker.source_gone": ("The source pad is gone", "El pad de origen ya no está"),
    "message.quarryindustrial.drone_pad.occupied": ("This pad already has a drone", "Este pad ya tiene un dron"),
    "message.quarryindustrial.drone_pad.status": (
        "%s · %s → %s · Energy: %s / %s EU · Out: %s slots, %s mB · In: %s slots, %s mB",
        "%s · %s → %s · Energía: %s / %s EU · Salida: %s slots, %s mB · Llegada: %s slots, %s mB"),
    "status.quarryindustrial.drone_pad.none": ("no drone", "sin dron"),
    "status.quarryindustrial.drone_pad.no_drone": ("No drone", "Sin dron"),
    "status.quarryindustrial.drone_pad.no_link": ("No destination linked", "Sin destino vinculado"),
    "status.quarryindustrial.drone_pad.bad_link": ("Destination is gone or too far", "El destino ya no está o queda muy lejos"),
    "status.quarryindustrial.drone_pad.no_power": ("Not enough energy for the trip", "No alcanza la energía para el viaje"),
    "status.quarryindustrial.drone_pad.loading": ("Loading", "Cargando"),
    "status.quarryindustrial.drone_pad.destination_full": ("Destination full", "Destino lleno"),
    "status.quarryindustrial.drone_pad.standby": ("Standing by", "En espera"),
    "status.quarryindustrial.drone_pad.ready": ("Ready: click the pad to launch a survey",
                                                "Listo: clic en el pad para mandarlo a explorar"),
    "status.quarryindustrial.drone_pad.in_flight": ("In flight", "En vuelo"),
    "status.quarryindustrial.drone_pad.lost": ("Drone lost (it came back without its cargo)",
                                               "Dron perdido (volvió sin la carga)"),
    # ── Jade ──
    "jade.quarryindustrial.status": ("Status: %s", "Estado: %s"),
    "jade.quarryindustrial.energy": ("Energy: %s / %s EU", "Energía: %s / %s EU"),
    "jade.quarryindustrial.energy_in": ("Energy: %s / %s EU (in: %s EU/t)", "Energía: %s / %s EU (entra: %s EU/t)"),
    "jade.quarryindustrial.output": ("Output: %s EU/t", "Salida: %s EU/t"),
    "jade.quarryindustrial.oil": ("Oil: %s / %s mB", "Petróleo: %s / %s mB"),
    "jade.quarryindustrial.fuel": ("Fuel: %s / %s mB", "Combustible: %s / %s mB"),
    "jade.quarryindustrial.steam": ("Steam: %s mB", "Vapor: %s mB"),
    "jade.quarryindustrial.drilled": ("Drilled: %s / %s m", "Perforado: %s / %s m"),
    "jade.quarryindustrial.bit": ("Bit: %s (%s%% left)", "Broca: %s (%s %% de vida)"),
    "jade.quarryindustrial.no_bit": ("No drill bit", "Sin broca"),
    "jade.quarryindustrial.coolant": ("Coolant: %s / %s mB", "Refrigerante: %s / %s mB"),
    "jade.quarryindustrial.heat": ("Bit heat: %s%%", "Calor de la broca: %s %%"),
    "jade.quarryindustrial.ports": ("Energy ports: %s", "Puertos de energía: %s"),
    "jade.quarryindustrial.layer": ("Layer Y %s · %s%%", "Capa Y %s · %s %%"),
    "jade.quarryindustrial.speed_upgrades": ("Speed upgrades: %s", "Upgrades de velocidad: %s"),
    "jade.quarryindustrial.drone": ("Drone: %s", "Dron: %s"),
    "jade.quarryindustrial.link": ("Destination: %s %s %s", "Destino: %s %s %s"),
    "config.jade.plugin_quarryindustrial.machine": ("Quarry Industrial machines", "Máquinas de Quarry Industrial"),
    # ── Páginas de información (JEI / REI) ──
    "info.quarryindustrial.quarry": (
        "Place it facing you: it mines a square area behind it. First it builds a steel gantry frame, then it digs "
        "layer by layer to the bottom of the world. Needs EU (any Team Reborn Energy cable) and a drill bit. Pipe "
        "water in for +25%% speed and half the bit wear. Drops go to an adjacent chest or pipe.",
        "Ponela mirando hacia vos: mina un cuadrado detrás de ella. Primero arma un pórtico de acero y después "
        "excava capa por capa hasta el fondo del mundo. Necesita EU (cualquier cable de Team Reborn Energy) y una "
        "broca. Con agua por caño: +25 %% de velocidad y la mitad de desgaste. Los drops van a un cofre o caño pegado."),
    "info.quarryindustrial.quarry_core": (
        "Nuclear Quarry: put the core in the middle of the front face of a 3x3x3 cube of Quarry Casing, with 1 to 8 "
        "HV Energy Ports instead of some casings (one reactor per port). Missing blocks show up as a hologram. "
        "It overheats without water.",
        "Quarry nuclear: el núcleo va en el centro de la cara delantera de un cubo de 3x3x3 de Carcasa de quarry, "
        "con 1 a 8 Puertos de energía HV en lugar de algunas carcasas (un reactor por puerto). Los bloques que "
        "faltan se ven como holograma. Sin agua se recalienta."),
    "info.quarryindustrial.energy_port": (
        "HV input for the Nuclear Quarry. Each port takes up to 512 EU/t: one Ala Industrial reactor per port.",
        "Entrada HV de la quarry nuclear. Cada puerto toma hasta 512 EU/t: un reactor de Ala Industrial por puerto."),
    "info.quarryindustrial.pumpjack": (
        "Place it over the 1x1 shaft of an Ala Industrial oil geyser, facing the shaft. It drains the dome, then "
        "pumps oil forever. Runs on EU or on diesel fed into the motor row (the back of the base).",
        "Ponela sobre el caño de 1x1 de un géiser de Ala Industrial, mirando hacia él. Vacía el domo y después "
        "bombea petróleo para siempre. Anda con EU o con diésel por la fila del motor (atrás de la base)."),
    "info.quarryindustrial.offshore_rig": (
        "Place it on the ocean surface (at least 8 blocks deep): it builds a 7x7 deck and legs down to the seabed. "
        "It drills the seabed (EU + a drill bit) and then produces oil forever. Oil comes out of any deck block.",
        "Ponela sobre la superficie del océano (al menos 8 bloques de profundidad): arma una cubierta de 7x7 y "
        "patas hasta el fondo. Perfora el lecho marino (EU + una broca) y después produce petróleo para siempre. "
        "El petróleo sale por cualquier bloque de la cubierta."),
    "info.quarryindustrial.diesel_generator": (
        "Burns Ala Industrial diesel, fuel oil or biofuel and outputs up to 128 EU/t to adjacent cables and machines.",
        "Quema diésel, fuel oil o biocombustible de Ala Industrial y entrega hasta 128 EU/t a cables y máquinas pegadas."),
    "info.quarryindustrial.steam_turbine": (
        "Place it right in front of an Ala Industrial Steam Nozzle outlet: it caps the vent and turns the "
        "reactor steam into EU (2 EU per mB). Also accepts steam from any pipe.",
        "Ponela justo delante de la boca de una Steam Nozzle de Ala Industrial: tapa el venteo y convierte el vapor "
        "del reactor en EU (2 EU por mB). También acepta vapor de cualquier caño."),
    "info.quarryindustrial.drone_pad": (
        "Home of a drone. It has an outbox (pipes put things in, and it pulls from chests and tanks next to it) and "
        "an inbox (what other drones bring, pushed to its neighbours). Each trip costs energy from the pad.",
        "La casa de un dron. Tiene un buzón de salida (los caños meten cosas y tira de cofres y tanques pegados) y "
        "uno de llegada (lo que traen otros drones, que reparte a sus vecinos). Cada viaje gasta energía del pad."),
    "info.quarryindustrial.drone_linker": (
        "Sneak + right-click the source pad, then right-click the destination pad.",
        "Agachado + clic derecho en el pad de origen, después clic derecho en el de destino."),
    "info.quarryindustrial.manual": (
        "Everything about the mod, in game. You also get one with your first quarry.",
        "Todo sobre el mod, dentro del juego. También te llega uno con tu primera quarry."),
    "info.quarryindustrial.drill_bit": (
        "Quarries and offshore rigs need a drill bit. It wears with every block (more with hard ones, half with "
        "water) and breaks at the end of its life. Each bit is crafted from the previous one.",
        "Las quarries y las plataformas necesitan broca. Se gasta con cada bloque (más con los duros, la mitad con "
        "agua) y al final se rompe. Cada broca se fabrica con la anterior."),
    "info.quarryindustrial.cargo_drone": ("Put it on a pad and link the pad: it carries items between them.",
                                          "Ponelo en un pad y vinculá el pad: lleva items entre los dos."),
    "info.quarryindustrial.tanker_drone": ("Carries fluids between two linked pads: perfect for offshore oil.",
                                           "Lleva fluidos entre dos pads vinculados: ideal para el petróleo offshore."),
    "info.quarryindustrial.power_drone": ("Carries EU between two linked pads: power for quarries far from the grid.",
                                          "Lleva EU entre dos pads vinculados: energía para quarries lejos de la red."),
    "info.quarryindustrial.maintenance_drone": (
        "Put spare drill bits in its pad: it swaps worn bits on quarries and rigs within 48 blocks.",
        "Poné brocas de repuesto en su pad: cambia las brocas gastadas de quarries y plataformas a 48 bloques."),
    "info.quarryindustrial.survey_drone": (
        "Right-click its pad to launch a survey: it sweeps a 128-block radius and brings back a report with every "
        "oil geyser it saw.",
        "Clic derecho en su pad para mandarlo: recorre un radio de 128 bloques y vuelve con un informe de todos los "
        "géiseres que vio."),
    # ── Centralita de drones ──
    "block.quarryindustrial.drone_console": ("Drone Control Station", "Centralita de drones"),
    "gui.quarryindustrial.console.tab.map": ("Map", "Mapa"),
    "gui.quarryindustrial.console.tab.drones": ("Drones (%s)", "Drones (%s)"),
    "gui.quarryindustrial.console.tab.wells": ("Wells (%s)", "Pozos (%s)"),
    "gui.quarryindustrial.console.connecting": ("Connecting to the drone network…", "Conectando con la red de drones…"),
    "gui.quarryindustrial.console.empty_pad": ("Empty pad", "Pad vacío"),
    "gui.quarryindustrial.console.no_pads": (
        "No drone pads within %s blocks of this station. Place a pad and put a drone on it.",
        "No hay pads a menos de %s bloques de la centralita. Poné un pad y un dron arriba."),
    "gui.quarryindustrial.console.no_wells": (
        "No oil geysers found yet. Put a survey drone on a pad and send it out: everything it finds shows up here "
        "and on the map.",
        "Todavía no se encontró ningún géiser. Poné un dron explorador en un pad y mandalo a explorar: todo lo que "
        "encuentre aparece acá y en el mapa."),
    "gui.quarryindustrial.console.no_signal": ("No signal (chunk unloaded)", "Sin señal (chunk descargado)"),
    "gui.quarryindustrial.console.energy": ("Energy: %s%%", "Energía: %s %%"),
    "gui.quarryindustrial.console.link": ("→ %s (%s m)", "→ %s (%s m)"),
    "gui.quarryindustrial.console.no_link": ("No destination", "Sin destino"),
    "gui.quarryindustrial.console.distance": ("%s m away", "a %s m"),
    "gui.quarryindustrial.console.range": ("Sees everything within %s blocks", "Ve todo a menos de %s bloques"),
    "gui.quarryindustrial.console.button.view": ("View", "Ver"),
    "gui.quarryindustrial.console.button.link": ("Link", "Vincular"),
    "gui.quarryindustrial.console.button.target": ("Pick", "Elegir"),
    "gui.quarryindustrial.console.button.unlink": ("Unlink", "Desvincular"),
    "gui.quarryindustrial.console.button.survey": ("Survey", "Explorar"),
    "gui.quarryindustrial.console.button.forget": ("Forget", "Olvidar"),
    "gui.quarryindustrial.console.button.cancel": ("Cancel", "Cancelar"),
    "gui.quarryindustrial.console.linking": ("Pick the destination pad · right-click to cancel",
                                             "Elegí el pad de destino · clic derecho para cancelar"),
    "gui.quarryindustrial.console.link_here": ("Click: send the cargo here", "Clic: mandar la carga acá"),
    "gui.quarryindustrial.console.map_hint": ("Click a pad to select it · drag to move · wheel to zoom",
                                              "Clic en un pad para elegirlo · arrastrá para mover · rueda para zoom"),
    "gui.quarryindustrial.console.list_hint": ("Every pad within range of the station",
                                               "Todos los pads al alcance de la centralita"),
    "gui.quarryindustrial.console.cursor": ("X %s  Z %s  ·  %s", "X %s  Z %s  ·  %s"),
    "gui.quarryindustrial.console.legend.pad": ("Pad", "Pad"),
    "gui.quarryindustrial.console.legend.well": ("Geyser", "Géiser"),
    "gui.quarryindustrial.console.legend.route": ("Route", "Ruta"),
    "gui.quarryindustrial.console.site": ("Oil geyser", "Géiser de petróleo"),
    "gui.quarryindustrial.console.site_finder": ("found from the pad at %s", "lo encontró el pad de %s"),
    "gui.quarryindustrial.console.site_hint": ("Right-click: remove from the map", "Clic derecho: sacarlo del mapa"),
    "gui.quarryindustrial.console.flight_to": ("Flying to %s", "Volando a %s"),
    "gui.quarryindustrial.console.flight_home": ("Returning to its pad at %s", "Volviendo a su pad en %s"),
    "gui.quarryindustrial.console.surveying": ("Surveying: %s waypoints left", "Explorando: le faltan %s puntos"),
    "message.quarryindustrial.console.not_a_pad": ("That is not a drone pad anymore", "Eso ya no es un pad de drones"),
    "message.quarryindustrial.console.not_survey": ("That pad has no survey drone", "Ese pad no tiene un dron explorador"),
    "message.quarryindustrial.console.survey_busy": ("The survey drone is already out", "El explorador ya está volando"),
    "message.quarryindustrial.console.survey_sent": ("Survey drone on its way", "Explorador en camino"),
    "message.quarryindustrial.console.site_forgotten": ("Geyser removed from the map", "Géiser sacado del mapa"),
    "info.quarryindustrial.drone_console": (
        "A map of every drone pad within 1024 blocks: links, drones in flight (with the survey drone's sweep) and "
        "the oil geysers your survey drones found. Link pads and launch surveys from here, without walking to them.",
        "Un mapa de todos los pads a menos de 1024 bloques: vínculos, drones en vuelo (con el barrido del explorador) "
        "y los géiseres que encontraron tus exploradores. Desde acá vinculás pads y mandás a explorar sin ir hasta "
        "ellos."),
    # ── Sonidos ──
    "subtitles.quarryindustrial.drill": ("Quarry drilling", "Quarry perforando"),
    "subtitles.quarryindustrial.diesel": ("Diesel engine runs", "Motor diésel en marcha"),
    "subtitles.quarryindustrial.drone": ("Drone whirs", "Zumbido de dron"),
    "subtitles.quarryindustrial.turbine": ("Turbine whines", "Silbido de turbina"),
    "subtitles.quarryindustrial.pumpjack_stroke": ("Pumpjack thumps", "Golpe de la cigüeña"),
    "subtitles.quarryindustrial.alarm": ("Machine alarm", "Alarma de máquina"),
}


def main():
    for code, index in (("en_us", 0), ("es_ar", 1), ("es_es", 1)):
        path = LANG / f"{code}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, texts in KEYS.items():
            data[key] = to_tu(texts[index]) if code == "es_es" else texts[index]
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(KEYS)} claves en {LANG}")


if __name__ == "__main__":
    main()
