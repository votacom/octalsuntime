package at.manuelbichler.octalsuntime.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["name"])])
data class Location(
    @PrimaryKey val name: String,
    val latitude: Float,
    val longitude: Float
    // lat and lon: on Earth, in degrees.
)
