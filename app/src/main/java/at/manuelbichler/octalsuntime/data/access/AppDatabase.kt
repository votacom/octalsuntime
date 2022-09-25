package at.manuelbichler.octalsuntime.data.access

import androidx.room.Database
import androidx.room.RoomDatabase
import at.manuelbichler.octalsuntime.data.Location

@Database(entities = [Location::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
