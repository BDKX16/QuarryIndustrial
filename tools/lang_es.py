"""Pasa los imperativos con voseo (es_ar) a tuteo (es_es) en los textos del mod."""

VOSEO = {
    "Fabricá": "Fabrica", "Mirá": "Mira", "Usá": "Usa", "usá": "usa", "Conseguí": "Consigue",
    "Dejá": "Deja", "Empezá": "Empieza", "Vaciá": "Vacía", "Hacé": "Haz", "hacé": "haz",
    "Perforá": "Perfora", "Convertí": "Convierte", "Armá": "Arma", "Encontrá": "Encuentra",
    "Poné": "Pon", "poné": "pon", "Agregá": "Añade", "Llevá": "Lleva", "Conectá": "Conecta",
    "conectá": "conecta", "Elegí": "Elige", "Ponela": "Ponla", "Fijate": "Fíjate", "Ponelo": "Ponlo", "vinculá": "vincula",
    "hacia vos": "hacia ti", "por vos": "por ti", "Parate": "Ponte", "Mandalo": "Mándalo",
    "Usala": "Úsala", "Instalale": "Instálale", "vinculalo": "vincúlalo", "golpeás": "golpeas", "Entrá": "Entra",
    "mandalo": "mándalo", "arrastrá": "arrastra", "mandás": "mandas", "tenés": "tienes", "podés": "puedes",
}


def to_tu(text):
    for vos, tu in VOSEO.items():
        text = text.replace(vos, tu)
    return text
