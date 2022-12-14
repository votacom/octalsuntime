package at.manuelbichler.octalsuntime

import androidx.lifecycle.*
import at.manuelbichler.octalsuntime.model.Location
import at.manuelbichler.octalsuntime.data.LocationDao
import kotlinx.coroutines.launch

class LocationViewModel(private val dao : LocationDao) : ViewModel() {

    val locations: LiveData<List<Location>> = dao.getAll().asLiveData()

    private fun insert(location : Location) { // suspend function
        viewModelScope.launch {
            dao.insert(location)
        }
    }

    private fun deleteAll() { // suspend function
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    fun delete(loc: Location) { // suspend function
        viewModelScope.launch {
            dao.delete(loc)
        }
    }

    fun getByName(name: String) : LiveData<Location?> = liveData {
        emit(dao.getByName(name))
    }

    fun addNewLocation(name: String, latitude: Float, longitude: Float) {
        addNewLocation( Location( name, latitude, longitude ) )
    }

    fun addNewLocation(loc: Location) {
        insert(loc)
    }

    fun clear() {
        deleteAll()
    }

}

class LocationsViewModelFactory(private val dao : LocationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
