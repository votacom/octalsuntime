package at.manuelbichler.octalsuntime.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import at.manuelbichler.octalsuntime.model.Location

@Dao
interface LocationDao {
    @Query("SELECT * FROM location")
    fun getAll(): List<Location>

    @Query("SELECT * FROM location WHERE name LIKE :name LIMIT 1")
    fun findByName(name: String): Location

    @Query("SELECT * FROM location WHERE name LIKE :name")
    fun searchByName(name: String): List<Location>

    @Insert
    fun insertAll(vararg locations: Location)

    @Insert
    fun insert(location: Location)

    @Delete
    fun delete(location: Location)
}