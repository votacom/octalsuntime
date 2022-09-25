package at.manuelbichler.octalsuntime.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import at.manuelbichler.octalsuntime.model.Location

@Database(entities = [Location::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    companion object {
        @Volatile
        private var INSTANCE : AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
