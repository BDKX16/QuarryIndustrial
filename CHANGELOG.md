# Changelog

## 1.3.0 — Filtro de caño

### Nuevo

- **Filtro de caño** (requiere Ala Industrial). Se engancha con clic derecho al brazo de un caño de ítems (común o
  avanzado) que toca un inventario, y esa cara solo deja pasar los ítems marcados, tanto al extraer como al insertar.
  No usa energía. Clic derecho con la mano vacía abre su GUI; agachado + clic derecho lo saca con su configuración, y
  al romper el caño cae como ítem.
  - Se ve enganchado al caño: un manguito de hierro con una banda celeste sobre el brazo, en cada cara con filtro.
  - 1 casillero fantasma (el ítem de muestra no se gasta); con la **Expansión de filtro**, que va en un panel lateral
    a la derecha de la GUI (como las mejoras de la quarry), se activan los 5. Sin ella se ve un solo casillero.
  - Permitir o denegar (con un solo casillero) o lista blanca / lista negra (con la expansión), y comparar por ítem o
    por categoría (tag): marcás un tronco y pasan todos los troncos; si el ítem tiene varias categorías, clic en el
    casillero cambia cuál se usa. En modo categoría, un panel a la derecha lista todos los ítems que entran en esa
    categoría (con rueda para ver más; pasar el mouse sobre un casillero muestra la suya). Cada botón explica en un
    tooltip qué hace.
  - Receta con un Empty Chip de Ala (sin Ala: comparador). La expansión también lleva un chip.

### Cambios

- **Manual del operador ampliado.** Capítulo nuevo del filtro de caño (cómo se usa, expansión y listas, ítem o
  categoría), página del distribuidor de energía en Generadores, página de exploración de largo alcance (mejoras,
  viaje desde la centralita, por qué importa en el late game) y la centralita al día (lagos, alcance, repetidores).
  También se corrigió el generador del manual, que se había quedado atrás del texto del explorador.

## 1.2.9 — Explorador de largo alcance, géiseres y lagos

### Nuevo

- **Más mejoras de alcance del explorador.** Entran 17 por pad (antes 4), +64 bloques de radio de barrido cada una
  (`surveyRangeMax`, `surveyRangeBonus`): con todas barre hasta 1.216 bloques de radio, que cuesta unos 500.000 EU
  (lo que guarda un pad) y tarda muchísimo. Además se lo puede mandar a barrer hasta 10.000 bloques del pad
  (`surveyMaxTravel`; la energía del pad es el límite real, 1 EU por bloque). Las mejoras también suman al
  radio del dron de mantenimiento: ahora +90 por mejora, unos 2.040 bloques con las 17 (`maintenanceRangeBonus`; si
  tenías la config vieja con 384, bajalo a mano).
- **Panel desplegable de mejoras en el pad.** El botón «Mejoras» de la pantalla del pad abre un panel con los ítems de
  lo instalado (cargador de chunks y mejoras de alcance), apagados los que faltan, con tooltip. Reemplaza las líneas de
  texto.
- **El explorador distingue géiseres de lagos de petróleo.** Un géiser (caño de 1×1 sellado y domo debajo) se bombea
  para siempre con una cigüeña; un lago es un charco finito de superficie, sin caño. En el mapa de la centralita los
  géiseres son la gota violeta y los lagos un charco gris, con su propia entrada en la leyenda, el tooltip y la
  pestaña de pozos. El informe del explorador lista los dos. Antes todo petróleo de superficie contaba como géiser.

### Cambios

- **Distribuidor de energía rediseñado.** Ahora es un modelo 3D discreto (cuerpo con brazos y collares, como el
  interruptor de Ala) en vez de un cubo llamativo. La entrada es el brazo con el collar ámbar, del lado del bloque
  contra el que lo colocás. Receta más barata y de drones: 2 cables de electrum, 1 circuito electrónico y 1 hormigón
  amarillo (sin Ala Industrial: 2 lingotes de cobre, redstone y hormigón amarillo).

- **La config se migra sola al actualizar.** El archivo guarda un `configVersion`; al pasar a la 1.2.9, el máximo de
  mejoras del explorador (`surveyRangeMax`) pasa de 4 a 17 y `maintenanceRangeBonus` de 384 a 90, pero solo si todavía
  tienen el valor viejo: lo que el admin personalizó no se toca. No hace falta editar nada a mano.

### Arreglos

- **Los distribuidores ahora se pasan la energía entre sí** (pegados o con un cable en el medio). Antes el pad
  empujaba la energía de sus drones al distribuidor y este la tomaba por una entrada, y los cables de Ala no podían
  sacarle energía a las salidas.
- **El dron de mantenimiento ahora toma las brocas de los cofres pegados a su pad.** Antes solo llevaba las que ya
  estaban dentro del pad, y un cofre con brocas al lado no servía.

## 1.2.8 — Distribuidor de energía y consola portátil

### Nuevo

- **Distribuidor de energía** (`energy_splitter`). Reparte los EU de un cable en partes iguales entre todo lo que tenga
  pegado (cables, pads de drones, máquinas). La energía entra solo por su cara de entrada, el brazo con el collar ámbar
  (queda del lado del bloque contra el que lo colocás), y sale por las otras cinco: los pads o cables de salida no
  pueden confundirse con una entrada. Si un destino se llena, lo que sobra va a los otros, así que con varios pads en
  la salida de un reactor ya no se llena primero el que está más cerca. Se pueden encadenar (una salida al brazo ámbar
  del siguiente), pegados o con un cable en el medio. Guarda 8.192 EU y mueve hasta 2.048 EU/t (`splitterCapacity` y
  `splitterMaxTransfer`). Receta barata: 2 cables de electrum, 1 circuito electrónico y 1 hormigón amarillo (sin Ala
  Industrial: 2 lingotes de cobre, redstone y hormigón amarillo).
- **Consola portátil.** Clic derecho al aire con el vinculador de drones abre el mismo mapa que la centralita (pads,
  drones, vinculación y viajes), centrado donde estás y con el alcance de la centralita. Se cierra si soltás el
  vinculador o te alejás más de 48 bloques. Sobre un pad, el vinculador funciona igual que antes.

### Cambios

- **Los cables de Ala Industrial ahora se ven conectados** a los pads de drones, las quarrys (LV, MV y HV), el puerto de
  energía, el distribuidor, la cigüeña, la plataforma offshore y los generadores. Antes recibían energía igual, pero el
  cable no dibujaba el brazo. Se puede sumar otro bloque con el tag `quarryindustrial:cable_connectable`. Sin Ala
  Industrial no cambia nada.
- **La turbina de vapor suena a la mitad de volumen.**

## 1.2.7 — Viajes del explorador

### Nuevo

- **Botón "Viajar" en la centralita.** Con un pad de explorador elegido (en el mapa o en la lista de drones), "Viajar"
  deja elegir en el mapa el centro de la zona: el dron vuela hasta ahí, hace la misma espiral de siempre y vuelve. Antes
  de elegir se ve el cuadrado que va a barrer. La zona tiene que estar dentro del alcance de la centralita.
- **El pad calcula la energía de todo el recorrido** (ida, espiral y vuelta) y el dron no sale hasta tenerla: si ya la
  tiene sale enseguida, si no queda en estado "Juntando energía" y sale solo cuando se carga. La ficha del pad muestra
  `energía / costo` y la zona pendiente en el mapa; "Cancelar" anula la salida. La salida pendiente se guarda con el pad.
  Si el recorrido cuesta más de lo que el pad puede guardar, se rechaza con un aviso.

### Cambios

- **La distancia máxima entre pads vinculados sube de 512 a 3000 bloques** (`maxLinkDistance`). Quien ya tenga el
  archivo de configuración con el valor viejo debe subirlo a mano.
- El costo del explorador pasó a ser el recorrido real: `tripBaseEu` + `surveyEuPerBlock` (1 por defecto) por cada bloque
  volado. Con los valores por defecto, una exploración alrededor del pad cuesta unos 6.500 EU en vez de unos 4.400.

## 1.2.6 — Destilerías limpias y alcance en el mapa

### Cambios

- **El dron de mantenimiento llega mucho más lejos**: de 48 a 512 bloques de radio (`maintenanceRadius`), pensado para
  una planta entera. Cada mejora de alcance instalada en el pad suma 384 bloques (`maintenanceRangeBonus`), hasta
  2048 con las 4. Quien ya tenga el archivo de configuración con el valor viejo debe subir `maintenanceRadius` a mano.

### Nuevo

- **El dron de mantenimiento limpia las columnas de destilación de Ala Industrial.** Cuando una columna de los
  alrededores del pad llega al 50 % de ensuciamiento (`maintenanceFouling`), el dron vuela hasta ahí, la deja limpia
  y vuelve con el carbón que rinde la limpieza, que cae en el buzón de llegada del pad (igual que con la llave
  inglesa). Para esto no necesita brocas. Sin Ala Industrial no hace nada distinto.
- **El mapa de la centralita muestra el alcance del dron del pad elegido**, según sus mejoras: un círculo para el
  vínculo de los drones de transporte y para el radio de búsqueda del de mantenimiento, y un cuadrado para el barrido
  del explorador, que crece con cada mejora de alcance instalada. La ficha del pad dice el alcance en bloques.

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
