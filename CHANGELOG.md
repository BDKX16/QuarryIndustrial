# Changelog

## 1.2.5 — El mapa como imagen

### Mejorado

- **El mapa de la centralita se dibuja como imágenes ya pintadas**, a la manera de Xaero. El terreno se divide en
  regiones de 256×256 píxeles (una por nivel de zoom) que se pintan una sola vez y se guardan como texturas; mover o
  acercar el mapa es solo dibujarlas en otro lugar, sin recalcular nada ni dibujar miles de rectángulos. Una región
  se repinta solo cuando cambia algún chunk que cubre, y pintarla cuesta de 1 a 4 ms. Además el mapa gana detalle:
  al acercar, un píxel es un bloque. Es solo del cliente.

## 1.2.4 — Mapa más fluido

### Mejorado

- **El mapa de la centralita va más rápido**, sobre todo con la pantalla grande y el zoom alejado. Lee el terreno de la
  memoria del mapa (sin consultar el mundo en cada muestra), con los colores precalculados, y mientras se arrastra
  desplaza el terreno ya armado y lo rearma cada ~0,1 s en vez de en cada cuadro. En un mapa de 640×345 celdas,
  rearmarlo pasó de 33 a 11 ms (zoom 2) y de 60 a 27 ms (alejado).

## 1.2.3 — El mapa por fin muestra lo recorrido

### Arreglado

- **El mapa de la centralita no dibujaba el terreno recordado.** `ClientLevel.hasChunk` responde siempre «sí», así que
  el mapa nunca llegaba a consultar la memoria y solo se veía lo que el cliente tenía cargado en ese momento. Ahora
  pregunta al origen de chunks. Es solo del cliente.
- **El mapa perdía resolución al alejar el zoom.** Cada celda se quedaba con un solo bloque de los que cubría. Ahora
  promedia hasta 4×4 bloques por celda, y la clave del mundo recordado se calcula una vez en vez de en cada consulta.

## 1.2.2 — Mapa recordado

### Arreglado

- **El mapa de la centralita no recordaba todo lo explorado.** La altura de cada columna se pedía al mundo cuando el
  chunk ya podía no estar, y esas columnas se descartaban. Ahora se lee del propio chunk y se anota al cargarse y al
  descargarse.
- **Lo explorado se perdía si el juego se cerraba de golpe.** Ahora se guarda a disco cada minuto (si hubo cambios) y
  al cerrar el cliente, además de al salir del mundo. Es solo del cliente.

## 1.2.1 — Radares, cargadores y GUIs

### Nuevo

- **GUIs de solo lectura** para el pad de drones, la cigüeña, la plataforma offshore, el generador diésel y la
  turbina de vapor (clic derecho sin agacharse): estado, barras de energía y fluidos, mejoras instaladas. El pad
  suma el botón «Mandar a explorar» (ya no se lanza con clic derecho en el pad).
- **El mapa de la centralita recuerda el terreno** que ya viste, aunque hoy no esté cargado (se guarda en
  `quarryindustrial_map/`, por mundo y dimensión); lo recordado se ve más oscuro.
- **Cargador de chunks para pad:** clic derecho en un pad y queda instalado; mantiene su chunk cargado (mismo ticket
  que las quarries, respeta `chunkLoading`) aunque estés lejos. Se suelta al romper el pad.
- **Mejora de alcance del explorador:** hasta 4 por pad, cada una suma 64 bloques al radio del barrido
  (`surveyRangeBonus` / `surveyRangeMax`).
- **Radar repetidor:** bloque que amplía la centralita. Dentro de la zona que ya ve la centralita (u otro radar) suma
  su propio radio (`radarRange`, 1024); se encadenan para cubrir cualquier distancia.
- **Recetas con variante Ala Industrial** para el radar, la mejora de alcance y el cargador de chunks (con circuitos y
  placas); sin Ala se craftean con materiales vanilla.
- Texturas propias para el cargador de chunks, la mejora de alcance del explorador y el radar repetidor.

## 1.2.0 — Centralita de drones

### Nuevo

- **Centralita de drones:** un mapa de todos los pads a menos de 1024 bloques con el terreno, los vínculos, los
  drones en vuelo (con el barrido que le falta al explorador) y los géiseres encontrados. Pestañas con la lista de
  pads (estado, energía, destino) y de pozos. Desde ahí se vinculan pads, se manda a explorar y se borran géiseres
  del mapa, sin ir hasta ellos.

### Cambios

- Íconos nuevos para los cinco drones: en 3/4, con las hélices girando, luces de navegación y la carga de cada
  tipo colgando.

## 1.1.1

### Arreglos

- El server no arrancaba con versiones de Ala Industrial anteriores a la 0.1.218 ("Failed to parse
  quarryindustrial:upgrade_ore_seeker"): la receta del buscavetas usaba `drill_row_module`, que esas versiones no
  tienen. Ahora usa el `magnet_filter_module`.
- Cada receta con Ala comprueba que existan todos los items de Ala que usa. Si a la versión instalada le falta
  alguno, se carga la receta vanilla en lugar de cortar la carga de datapacks.

## 1.1.0 — Expansión

### Nuevo

- **Brocas** de hierro, diamante, netherite y PDC, con desgaste según la dureza del bloque. Los dientes se van
  cayendo en el modelo, la broca tira chispas cuando está por romperse y la PDC se pone al rojo con el calor.
- **Refrigeración con agua** (+25 % de velocidad y la mitad de desgaste) y **calor** en la quarry nuclear.
- **Visualización del minado:** esquirlas del bloque, lodo y una tolva en el carro que se llena con lo que se mina.
- Upgrade **buscavetas**: solo rompe minerales; la receta con Ala usa los módulos del taladro eléctrico.
- **Plataforma petrolera offshore**, con torre en celosía, antorcha de gas y patas hasta el fondo del mar.
- **Generador diésel** y **turbina de vapor**, que toma el vapor de la Steam Nozzle de Ala.
- **Motor diésel en la cigüeña** y **Oil Slimes** de Ala que salen de los pozos.
- **Drones:** pad, vinculador y cinco tipos (carga, cisterna, energía, mantenimiento y explorador).
- **Manual del operador** dentro del juego, en inglés y español.
- **Sonidos propios** y **14 logros**.
- Compatibilidad con **Jade**, **JEI** y **REI**.

### Cambios

- Las quarries necesitan una broca para minar. Las de mundos de la 1.0 quedan en "Sin broca" hasta que se les
  ponga una.
- Fabric Loader mínimo: 0.19.5 (antes pedía 0.20.1 sin necesitarlo).

### Arreglos

- La carga de chunks de la quarry usa un ticket propio. Antes, al terminar o al romperse, podía descargar chunks
  forzados por otros (`/forceload` u otros mods).

## 1.0.0

Primera versión: quarries LV, MV y HV, quarry nuclear multibloque, cigüeñas petroleras para los géiseres de Ala
Industrial, upgrades, GUI con minimapa y recetas con y sin Ala.
