package at.manuelbichler.octalsuntime

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import at.manuelbichler.octalsuntime.adapter.LocationAdapter
import at.manuelbichler.octalsuntime.data.AppDatabase
import at.manuelbichler.octalsuntime.model.Location

class LocationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locations)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // allow navigating back

        val locationDataset = emptyList<Location>()
        val recyclerView = findViewById<RecyclerView>(R.id.locations_recycler_view)
        recyclerView.adapter = LocationAdapter(this, locationDataset)
        recyclerView.setHasFixedSize(true)
    }
}