# Quarry Industrial

Mod de Fabric para **Minecraft 26.3**. Agrega quarries de minería automática que escalan desde una máquina LV
hasta una **quarry nuclear multibloque** alimentada por varios reactores, **cigüeñas petroleras** (pumpjacks)
que extraen petróleo de los géiseres de [Ala Industrial](https://github.com/AlaMine-Team-Org/AlaIndustrial),
**plataformas offshore**, generadores diésel y de vapor, y una flota de **drones** para mover todo.

Funciona con energía **EU** a través de Team Reborn Energy. Se conecta solo con los cables, generadores y reactores
de Ala Industrial (y de Tech Reborn), sin depender de su código.

![Quarry HV trabajando](docs/screenshots/01_quarry_hv.jpg)

## Qué trae

### Quarries por tier

La quarry primero levanta un **pórtico** de vigas alrededor del área y después excava capa por capa, desde
debajo del pórtico hasta el fondo del mundo. Sobre el pórtico corre un puente con un carro, y del carro cuelga
una **sarta de perforación** (kelly, tubos con tool joints) que termina en una **broca tricónica** con conos que
giran sobre su propio eje.

| Tier | Bloque | Entrada | Área (base → máx.) | Cabezales | Bloques/s máx. |
|---|---|---|---|---|---|
| LV | Quarry (LV) | 32 EU/t | 9×9 → 16×16 | 1 | 5 |
| MV | Quarry (MV) | 128 EU/t | 16×16 → 48×48 | 1 | 20 |
| HV | Quarry (HV) | 512 EU/t | 32×32 → 80×80 | 2 | 60 |
| Nuclear | Núcleo + cubo 3×3×3 | 512 EU/t **por puerto** (1–8) | 64×64 → 144×144 | 4 | 400 |

- **La velocidad sale de la energía:** cada bloque cuesta `50 × (1 + dureza)` EU (más con upgrades). Una quarry
  con poca energía simplemente mina más lento.
- **Los drops** van a un buffer interno de 27 slots, que se vacía solo en cualquier cofre o tubería pegado a la
  quarry. Si se llena, la quarry se pausa.
- **Rompe como un jugador falso** con el UUID del dueño, así que respeta los mods de claims/protección.
- **Mantiene cargados** solo el chunk de la quarry y los de los cabezales.
- **No se inunda:** el agua o lava que encuentra dentro del área se borra, y la que está pegada al borde por
  afuera se tapa con un dique de adoquín.

### Quarry nuclear (multibloque)

Los cables más gruesos de Ala Industrial (electrum, HV) y sus reactores llegan a **512 EU/t**. Para escalar más
allá, la quarry nuclear tiene **puertos de energía**: cada uno recibe su propio cable, uno por reactor.

1. Poné el **Núcleo de quarry nuclear** mirando hacia vos.
2. Detrás de él, armá un cubo de **3×3×3**: el núcleo va en el centro de la cara delantera y las otras 26
   posiciones son **Carcasa de quarry** o **Puerto de energía (HV)** (de 1 a 8 puertos).
3. Si falta algo, la quarry muestra un **holograma** de los bloques que faltan.

El área a minar empieza detrás del cubo. La quarry nuclear usa una **broca PDC** (aletas con cortadores de
diamante) en vez de la tricónica.

![Quarry nuclear](docs/screenshots/04_nuclear.jpg)

### Cigüeña petrolera (pumpjack)

Los géiseres de petróleo de Ala Industrial tienen un caño de 1×1 que baja a un domo lleno de crudo. La cigüeña se
pone **sobre la boca del géiser**, mirando hacia él (la estructura de 3×7×5 se arma hacia atrás):

- Mientras quede crudo real en el caño y en el domo, lo **vacía** (1 cubo por segundo, 500 EU el cubo).
- Con el domo vacío, sigue extrayendo **para siempre** a 10 mB/t por 32 EU/t, hasta ~30 mB/t con 4 upgrades de
  velocidad.
- El petróleo (`#c:oil`, o sea `alaindustrial:oil`) va a un tanque de 16 cubos. Los **caños de Ala** pegados a la
  base lo toman solos (la cigüeña es un origen de un solo sentido, no hace falta la llave); para entregarlo a un
  tanque de Ala, poné esa cara del caño en "insertar" con la llave, como con cualquier tanque.

El balancín, la cabeza de caballo, las bielas y las manivelas con contrapesos están animados con la cinemática
real de un mecanismo de 4 barras.

![Cigüeña sobre un géiser](docs/screenshots/06_pumpjack.jpg)

### Brocas, refrigeración y calor

La quarry necesita una **broca** (el marco lo arma igual). Se gasta con cada bloque: `1 + dureza / 3` puntos
(piedra 1, pizarra profunda 2, obsidiana 17) y al final se rompe. Cada una se fabrica con la anterior:

| Broca | Velocidad | Vida |
|---|---|---|
| Tricónica de hierro | ×1.0 | 6 000 |
| Tricónica de diamante | ×1.3 | 24 000 |
| Tricónica de netherite | ×1.6 | 80 000 |
| PDC | ×2.0 | 200 000 |

En el modelo se ve: los insertos de carburo se van cayendo fila por fila y una broca casi muerta tira chispas.
Se instala con clic derecho, en el slot de la GUI o con un dron de mantenimiento.

- **Agua:** por caño, por cualquier cara (en la nuclear, por el núcleo). 1 mB por bloque: +25 % de velocidad y la
  mitad de desgaste.
- **Calor (quarry nuclear):** sin agua, la PDC se calienta. Arriba del 70 % frena (hasta el 20 % de velocidad), se
  pone al rojo y echa humo.
- **Visualización:** esquirlas del bloque que se rompe salen de la broca, salpica el lodo cuando hay agua y la
  tolva del carro se llena con el material que se está minando.

### Upgrades

Se ponen en el panel lateral de la GUI o con clic derecho sobre la quarry (agachado + clic saca el último).

| Upgrade | Efecto | Máx. |
|---|---|---|
| Área | +16 de lado (hasta el máximo del tier); rehace el marco | 3 |
| Velocidad | +50 % bloques/s, ×1.25 coste | 4 |
| Fortuna | Fortuna I–III en los drops, +50 % coste por nivel | 3 |
| Toque de seda | Drops con seda, ×2 coste (no se combina con fortuna) | 1 |
| Filtro (void) | Destruye piedra, tierra, grava, etc. (tag `quarryindustrial:void_filter`) | 1 |
| Buscavetas | El cabezal solo rompe minerales (`#c:ores`) y deja la roca en su lugar. Se fabrica con los módulos de fila y columna del taladro de Ala | 1 |

La cigüeña y la plataforma offshore solo aceptan upgrades de velocidad.

### Motor diésel y Oil Slimes

Con diésel (o fuel oil, o biocombustible) por la **fila del motor** (la de atrás de la base), la cigüeña quema su
propio combustible cuando le falta EU y echa humo por el escape. El resto de la base sigue siendo solo salida de
petróleo, así los caños de Ala lo toman sin llave. Cada tanto sale un **Oil Slime** de Ala del pozo.

### Plataforma offshore

Se pone sobre la superficie de un **océano** de al menos 8 bloques de profundidad, como un nenúfar. Arma una
cubierta de rejilla de 7×7 a nivel del mar y cuatro patas hasta el fondo, con la torre de perforación en
celosía, el aparejo móvil, la casilla, la sala de máquinas y la antorcha de gas.

1. Con una broca y energía (256 EU/t), la sarta baja por el agua y perfora 24 m dentro del lecho marino
   (50 puntos de desgaste por metro).
2. Al llegar al reservorio produce **25 mB/t de petróleo para siempre** (96 EU/t), más con upgrades, y se
   enciende la antorcha.

El petróleo sale por cualquier bloque de la cubierta (lo ideal es un dron cisterna hasta la costa) y el diésel
entra por el controlador.

### Generadores

| Bloque | Qué hace |
|---|---|
| Generador diésel | Quema diésel (12 EU/mB), fuel oil u otro biocombustible (8 EU/mB) de Ala y entrega hasta 128 EU/t |
| Turbina de vapor | Se pone tapando la boca de una **Steam Nozzle** de Ala: toma el vapor del reactor antes de que se ventee y lo convierte en EU (2 EU/mB, hasta 1024 EU/t) |

La Steam Nozzle de Ala no deja sacar vapor por la API de fluidos, así que la turbina lo lee de su tanque por
reflexión (si la API de Ala cambia, simplemente no toma vapor de la nozzle).

### Drones

Un dron vive en un **pad de drones**. El pad tiene un buzón de salida (lo llenan los caños y tolvas, y además tira
de cofres y tanques pegados) y uno de llegada (lo que traen otros drones, que reparte a sus vecinos). Cada viaje
cuesta 1000 EU + 8 EU por bloque de la energía del pad. El **vinculador** une un pad de origen con uno de destino
(agachado + clic en el origen, clic en el destino; hasta 512 bloques).

| Dron | Qué hace |
|---|---|
| Carga | Items entre dos pads (sale con 4 stacks o a los 10 s) |
| Cisterna | Fluidos, 16 cubos por viaje (el petróleo de la offshore a la costa) |
| Energía | Una batería voladora de 400 000 EU para quarries lejos de la red |
| Mantenimiento | Con brocas de repuesto en su pad, cambia las gastadas (≥ 90 %) de quarries y plataformas a 48 bloques |
| Explorador | Clic en su pad: recorre un radio de 128 bloques y vuelve con un informe de los géiseres que vio |

Despegan en vertical, vuelan en línea recta por arriba de todo y aterrizan; mantienen cargado el chunk donde
están. Un golpe los baja (sueltan la carga y vuelven a aparecer en su pad). Si un pad no sabe nada de su dron por
5 minutos, lo da por perdido y el dron vuelve sin la carga.

### Manual, sonidos y logros

- **Manual del operador:** un libro con 10 capítulos (en inglés y español) que se abre con clic derecho. Llega
  solo con la primera quarry, o se fabrica con un libro y un lingote de hierro.
- **Sonidos propios** (sintetizados con `tools/gen_sounds.py`): la broca, el motor diésel, la turbina, las hélices
  de los drones, el golpe de la cigüeña y una alarma cuando una quarry se para.
- **14 logros** en una pestaña propia, desde "Arriba el pórtico" hasta "Ocho reactores, un pozo".

### GUI

Energía, estado, EU/t de entrada (y por puerto en la nuclear), capa actual, avance, un **minimapa** de la capa
en curso con los cabezales, el buffer de salida y los upgrades.

![GUI](docs/screenshots/09_gui.png)

## Compatibilidad

- **Requiere:** Fabric Loader, Fabric API. Team Reborn Energy viene incluido en el jar.
- **Recomendado:** Ala Industrial (cables, reactores, petróleo). Sin Ala el mod funciona igual con cualquier
  fuente de EU de Team Reborn Energy; las recetas cambian a ingredientes vanilla y la cigüeña queda inactiva
  porque no hay petróleo.
- **Recetas:** cada item tiene dos recetas y se carga la que corresponde: con Ala usan sus circuitos, carcasas y
  placas; sin Ala, equivalentes vanilla.
- **Jade:** al mirar cualquier máquina muestra estado, energía, vida de la broca, fluidos y avance.
- **JEI / REI:** muestran las recetas y una página de información por máquina.

## Configuración

`config/quarryindustrial.json` se crea al primer arranque con todos los números: costes, entrada y área de cada
tier, velocidad, altura y coste del marco, chunk loading, y todo lo de la cigüeña (caudal, consumo, umbrales para
reconocer un géiser).

## Desarrollo

```bash
./gradlew build                 # compila y arma el jar en build/libs
./gradlew runClient             # cliente de desarrollo (con Ala Industrial cargado)
./gradlew runGameTest           # gametests de servidor (quarries, brocas, nuclear, cigüeña, offshore, generadores, drones, recetas, logros)
./gradlew runGameTest -PnoAla   # los mismos, sin Ala Industrial
./gradlew runClientGameTest     # pruebas visuales: abre el juego y saca capturas en build/run/clientGameTest
QUARRY_SHOWCASE=1 QUARRY_SHOWCASE_DIR=docs/screenshots ./gradlew runClientGameTest   # capturas de esta página
python tools/finish_showcase.py  # después del showcase: JPG para las capturas y recorte de la GUI
QUARRY_EXPANSION=1 QUARRY_SHOWCASE_DIR=docs/screenshots ./gradlew runClientGameTest   # capturas de la expansión
./gradlew runClient -PwithJade -PwithJei   # cliente con Jade y JEI (también -PwithRei)
```

- Java 25, Gradle 9.8 (wrapper incluido), Fabric Loom 1.19.
- Ala Industrial se carga solo en runtime (`ala_version` en `gradle.properties`); el mod no compila contra sus clases.
- `tools/gen_textures.py`, `tools/gen_textures_expansion.py`, `tools/gen_icon.py` y `tools/gen_recipes.py`
  generan texturas, ícono y recetas (requieren Python 3 con Pillow). `gen_blocks.py`, `gen_item_models.py`,
  `gen_lang.py`, `gen_manual.py` y `gen_advancements.py` generan modelos, textos, el manual y los logros;
  `gen_sounds.py` sintetiza los sonidos (necesita ffmpeg con libvorbis).
- Jade, JEI y REI se compilan como `compileOnly`: no son dependencias del mod.
- El showcase arma las escenas en un mundo normal con semilla fija (`quarryindustrial`) y saca las capturas en
  1920×1080 sin HUD; tarda unos 4 minutos.

### Estructura

| Carpeta | Qué hay |
|---|---|
| `src/main/.../block/quarry` | Quarries, marco, multibloque nuclear, GUI (menú) |
| `src/main/.../block/pumpjack` | Cigüeña, validador del pozo, partes del multibloque |
| `src/main/.../block/offshore` | Plataforma offshore (controlador, cubierta, patas) |
| `src/main/.../block/generator` | Generador diésel y turbina de vapor |
| `src/main/.../block/drone`, `drone` | Pads y drones |
| `src/main/.../compat` | Jade, el vapor de Ala, los Oil Slimes |
| `src/client/.../render` | Renderers y modelos 3D (pórtico, broca tricónica y PDC, cigüeña) |
| `src/client/.../screen`, `manual` | Pantalla de la quarry y el manual |
| `src/client/.../compat` | JEI, REI y la parte de cliente de Jade |
| `src/gametest` | Gametests de servidor y pruebas visuales de cliente |
| `docs/` | Capturas y material para CurseForge |

## Licencia

Todos los derechos reservados (ver `LICENSE.txt`). Ala Industrial es de AlaMine Team (MIT) y no se redistribuye
con este mod.
