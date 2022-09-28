package at.manuelbichler.octalsuntime.wikidata

import org.json.JSONObject

/**
 * a wikidata item together with zero or one of its locations.
 */
data class LocatedItem(
    val item : Item,
    val location : CoordinateLocation
)