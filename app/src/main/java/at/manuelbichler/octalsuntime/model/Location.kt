package at.manuelbichler.octalsuntime.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["name"])])
data class Location(
    @PrimaryKey val name: String,
    val latitude: Float,
    val longitude: Float
    // lat and lon: on Earth, in degrees.
) {
    fun toUri() : Uri = Uri.Builder().scheme("geo").opaquePart("0,0?q=%f,%f(%s)".format(latitude, longitude, name)).build()
    companion object {
        /**
         * creates and returns a location based on a geo: Uri.
         * The given Uri must be in the format "geo:0,0?q=15.67,-43.18(University, Calais)"
         * For format ref. https://developer.android.com/guide/components/intents-common#ViewMap
         * If the given Uri is not in this format, an IllegalArgumentException is thrown.
         */
        fun fromUri(uri : Uri) : Location {
            val exception = IllegalArgumentException("The given URI $uri does not match the geo:0,0?q=lat,lng(label) format.")
            if (!uri.scheme.equals("geo")) throw exception
            val schemeSpecificPart = uri.schemeSpecificPart
            // check if the schemeSpecificPart matches the URI scheme we expect (lat,lon(label))
            if ((schemeSpecificPart == null) || !schemeSpecificPart.matches(Regex("^0,0\\?q=-?[0-9]+(\\.[0-9]*)?,-?[0-9]+(.[0-9]*)?\\(.*\\)$"))) throw exception
            try {
                val (lat, lon, labelWithParens) = schemeSpecificPart.substring("0,0?q=".length)
                    .split(',', '(', ')', limit = 3)
                val label = labelWithParens.dropLast(1)
                return Location(label, lat.toFloat(), lon.toFloat())
            } catch (e: Exception) {
                throw IllegalArgumentException(exception.message, e)
            }
        }
    }
}
