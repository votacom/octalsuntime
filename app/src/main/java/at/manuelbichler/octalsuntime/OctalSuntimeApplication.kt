package at.manuelbichler.octalsuntime

import android.app.Application
import at.manuelbichler.octalsuntime.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class OctalSuntimeApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
