package at.manuelbichler.octalsuntime.wikidata

/**
 * a wikidata item.
 */
data class Item(
    val id: Int, // a Q-number, e.g. 2 for Earth (https://www.wikidata.org/wiki/Q2)
    val label: String, // the English label
    val description: String?, // an English description. may be null.
)