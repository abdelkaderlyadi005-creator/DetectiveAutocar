package com.radarpieces.app

/** Lists the user picks from. Names are the ones used on French classifieds. */
object Catalog {
    val makes: Map<String, List<String>> = linkedMapOf(
        "Renault" to listOf("Clio", "Mégane", "Captur", "Twingo", "Scénic", "Kangoo", "Trafic", "Master", "Laguna", "Talisman", "Kadjar", "Espace", "Fluence", "Modus"),
        "Peugeot" to listOf("106", "206", "207", "208", "301", "307", "308", "407", "508", "2008", "3008", "5008", "Partner", "Expert", "Boxer", "Rifter"),
        "Citroën" to listOf("C1", "C2", "C3", "C4", "C4 Picasso", "C5", "Berlingo", "Jumpy", "Jumper", "DS3", "DS4", "Xsara", "Saxo"),
        "Dacia" to listOf("Sandero", "Duster", "Logan", "Lodgy", "Dokker", "Spring", "Jogger"),
        "Volkswagen" to listOf("Golf 4", "Golf 5", "Golf 6", "Golf 7", "Golf 8", "Polo", "Passat", "Tiguan", "Touran", "Caddy", "Transporter", "Jetta", "T-Roc", "Scirocco"),
        "Audi" to listOf("A1", "A3", "A4", "A5", "A6", "Q2", "Q3", "Q5", "Q7", "TT"),
        "BMW" to listOf("Série 1", "Série 3", "Série 5", "X1", "X3", "X5", "Mini"),
        "Mercedes-Benz" to listOf("Classe A", "Classe B", "Classe C", "Classe E", "CLA", "GLA", "GLC", "Vito", "Sprinter"),
        "Opel" to listOf("Corsa", "Astra", "Insignia", "Mokka", "Zafira", "Meriva", "Vivaro"),
        "Ford" to listOf("Fiesta", "Focus", "Mondeo", "Kuga", "C-Max", "Transit", "Puma", "Ka"),
        "Toyota" to listOf("Yaris", "Corolla", "Auris", "Aygo", "RAV4", "C-HR", "Hilux", "Prius"),
        "Fiat" to listOf("500", "Panda", "Punto", "Tipo", "Doblo", "Ducato", "Scudo"),
        "Seat" to listOf("Ibiza", "Leon", "Ateca", "Arona", "Altea", "Cordoba"),
        "Skoda" to listOf("Fabia", "Octavia", "Superb", "Kodiaq", "Rapid", "Yeti"),
        "Nissan" to listOf("Micra", "Qashqai", "Juke", "Note", "X-Trail", "Navara"),
        "Hyundai" to listOf("i10", "i20", "i30", "Tucson", "Kona", "ix35"),
        "Kia" to listOf("Picanto", "Rio", "Ceed", "Sportage", "Sorento", "Stonic"),
        "Mini" to listOf("Cooper", "One", "Countryman", "Clubman"),
        "Volvo" to listOf("V40", "V60", "XC60", "XC90", "S60"),
        "Mazda" to listOf("Mazda 2", "Mazda 3", "Mazda 6", "CX-5"),
        "Honda" to listOf("Civic", "Jazz", "CR-V", "Accord"),
        "Suzuki" to listOf("Swift", "Vitara", "Jimny", "Ignis"),
        "Jeep" to listOf("Renegade", "Compass", "Wrangler", "Cherokee"),
        "Land Rover" to listOf("Defender", "Discovery", "Freelander", "Range Rover Evoque"),
        "Alfa Romeo" to listOf("Giulietta", "MiTo", "159", "Giulia"),
        "Mitsubishi" to listOf("Outlander", "ASX", "L200", "Colt"),
        "Smart" to listOf("Fortwo", "Forfour"),
    )

    val partGroups: Map<String, List<String>> = linkedMapOf(
        "Carrosserie" to listOf("pare-chocs avant", "pare-chocs arrière", "capot", "aile avant", "porte avant", "porte arrière", "hayon", "coffre", "calandre", "rétroviseur", "pare-brise", "lunette arrière", "vitre de porte"),
        "Éclairage" to listOf("phare avant", "feu arrière", "antibrouillard", "clignotant", "optique complète"),
        "Moteur et échappement" to listOf("moteur complet", "culasse", "turbo", "injecteur", "pompe à injection", "alternateur", "démarreur", "radiateur", "volant moteur", "collecteur d'échappement", "échappement complet", "catalyseur", "FAP", "vanne EGR", "compresseur de clim", "boîtier papillon"),
        "Transmission" to listOf("boîte de vitesses", "boîte automatique", "embrayage", "cardan", "pont arrière", "différentiel", "arbre de transmission"),
        "Freinage et suspension" to listOf("étrier de frein", "disque de frein", "amortisseur", "ressort", "triangle de suspension", "rotule", "crémaillère de direction", "pompe de direction assistée", "essieu"),
        "Électronique" to listOf("calculateur moteur", "calculateur ABS", "autoradio", "compteur tableau de bord", "commodo", "faisceau électrique", "airbag", "ceinture de sécurité"),
        "Habitacle" to listOf("siège avant", "banquette arrière", "volant", "console centrale", "panneau de porte", "planche de bord"),
        "Roues" to listOf("jante", "pneu", "roue de secours"),
    )

    val engineFuels: Map<String, List<String>> = linkedMapOf(
        "Diesel" to listOf("1.5 dCi", "1.6 HDi", "1.6 BlueHDi", "1.5 BlueHDi", "2.0 HDi", "2.0 BlueHDi", "2.2 HDi", "1.9 TDI", "2.0 TDI", "3.0 TDI", "1.7 CDTI", "2.0 CDTI"),
        "Essence" to listOf("1.0 TCe", "1.2 TCe", "1.2 PureTech", "1.2 TSI", "1.4 TSI", "1.6 VTi", "1.6 THP", "1.8 TFSI", "2.0 TFSI", "1.4 16V", "1.6 16V"),
        "Hybride" to listOf("hybride", "hybride rechargeable"),
        "Électrique" to listOf("moteur électrique"),
    )

    val carFuels = listOf("diesel", "essence", "hybride", "électrique")
    val gearboxes = listOf("manuelle", "automatique")
    val years: List<String> = (2025 downTo 1990).map { it.toString() }
}
