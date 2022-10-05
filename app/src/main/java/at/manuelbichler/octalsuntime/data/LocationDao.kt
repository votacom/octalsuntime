package at.manuelbichler.octalsuntime.data

import androidx.room.*
import at.manuelbichler.octalsuntime.model.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM location")
    fun getAll(): Flow<List<Location>>

    @Query("SELECT * FROM location WHERE name = :name")
    suspend fun getByName(name: String): Location?

    @Query("SELECT * FROM location WHERE name LIKE :name")
    fun searchByName(name: String): Flow<List<Location>>

    @Insert
    suspend fun insertAll(vararg locations: Location)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(location: Location)

    @Delete
    suspend fun delete(location: Location)

    @Query("DELETE FROM location")
    suspend fun deleteAll()

}