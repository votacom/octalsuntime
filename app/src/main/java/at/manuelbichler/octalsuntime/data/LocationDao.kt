package at.manuelbichler.octalsuntime.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import at.manuelbichler.octalsuntime.model.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM location")
    fun getAll(): Flow<List<Location>>

    @Query("SELECT * FROM location WHERE name LIKE :name LIMIT 1")
    suspend fun findByName(name: String): Location

    @Query("SELECT * FROM location WHERE name LIKE :name")
    fun searchByName(name: String): Flow<List<Location>>

    @Insert
    suspend fun insertAll(vararg locations: Location)

    @Insert
    suspend fun insert(location: Location)

    @Delete
    suspend fun delete(location: Location)

    @Query("DELETE FROM location")
    suspend fun deleteAll()
}