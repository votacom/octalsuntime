package at.manuelbichler.octalsuntime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.manuelbichler.octalsuntime.model.Location
import at.manuelbichler.octalsuntime.data.LocationDao
import kotlinx.coroutines.launch

class LocationViewModel(private val locationDao : LocationDao) : ViewModel() {
    private fun insertLocation(location : Location) { // suspend function
        viewModelScope.launch {
            locationDao.insert(location)
        }
    }

    private fun getNewLocationEntry(name: String, latitude: Float, longitude: Float): Location {
        return Location(
            name, latitude, longitude
        )
    }

    fun addNewLocation(name: String, latitude: Float, longitude: Float) {
        val newLocation = getNewLocationEntry(name, latitude, longitude)
        insertLocation(newLocation)
    }
}

class LocationsViewModelFactory(private val locationDao : LocationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(locationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
