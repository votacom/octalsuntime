package at.manuelbichler.octalsuntime.wikidata

import org.json.JSONObject
import java.util.*

data class CoordinateLocation(
    val lat : Float, // degrees on Earth
    val lon : Float, // degrees on Earth
    var source : String, // may be null
) {
    companion object {
        /**
         * argument must be a JSON structure as returned by the SPARQL query in strings.xml.
         * Returns a collection of the locations returned.
         */
        fun fromJSON(json : JSONObject) : Collection<CoordinateLocation> {
            val returnList = emptyList<CoordinateLocation>().toMutableList()
            val rows = json.getJSONObject("results").getJSONArray("bindings")
            var prevStmt : String? = null
            var prevLocation : CoordinateLocation? = null
            for(i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val stmt = row.getJSONObject("stmt").getString("value")
                val lat = row.getJSONObject("lat").getString("value").toFloat()
                val lon = row.getJSONObject("lon").getString("value").toFloat()
                val propRefLabel = row.optJSONObject("propRefLabel")?.getString("value")
                val propObjectLabel = row.optJSONObject("propObjectLabel")?.getString("value")
                val sources = emptyList<String>().toMutableList()
                propObjectLabel?.let { sources.add(it) }
                propRefLabel?.let { sources.add(it) }
                val source = sources.joinToString(separator = "; ")
                if(prevLocation != null && stmt.equals(prevStmt)) {
                    // no new location, update the previous one with this additional source information
                    prevLocation.source+="; $source"
                } else {
                    prevStmt = stmt
                    prevLocation = CoordinateLocation(lat, lon, source)
                    returnList.add(prevLocation)
                }
            }
            return returnList
        }
    }
}