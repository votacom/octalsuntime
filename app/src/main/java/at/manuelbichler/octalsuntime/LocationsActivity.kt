package at.manuelbichler.octalsuntime

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import at.manuelbichler.octalsuntime.adapter.LocationAdapter
import at.manuelbichler.octalsuntime.data.AppDatabase
import at.manuelbichler.octalsuntime.model.Location

class LocationsActivity : AppCompatActivity() {

    private val viewModel : LocationViewModel by viewModels<LocationViewModel> {
        LocationsViewModelFactory( (application as OctalSuntimeApplication).database.locationDao() )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locations)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // allow navigating back

        val recyclerView = findViewById<RecyclerView>(R.id.locations_recycler_view)
        val adapter = LocationAdapter()
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        // Add an observer on the LiveData returned by the view model.
        // The onChanged() method fires when the observed data changes and the activity is
        // in the foreground.
        viewModel.locations.observe(this) { locations ->
            // Update the cached copy of the locations in the adapter.
            locations.let { adapter.submitList(it) }
        }

    }
}