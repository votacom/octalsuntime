package at.manuelbichler.octalsuntime

import android.app.Application
import androidx.lifecycle.viewModelScope
import at.manuelbichler.octalsuntime.data.AppDatabase
import at.manuelbichler.octalsuntime.model.Location
import kotlinx.coroutines.launch

class OctalSuntimeApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
