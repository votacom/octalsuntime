package at.manuelbichler.octalsuntime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import at.manuelbichler.octalsuntime.adapter.LocationAdapter
import at.manuelbichler.octalsuntime.model.Location
import at.manuelbichler.octalsuntime.wikidata.WikidataGeoListAdapter


class LocationsActivity : AppCompatActivity(), AddLocationAutoCompletionDialogFragment.LocationDialogListener {

    private val viewModel : LocationViewModel by viewModels<LocationViewModel> {
        LocationsViewModelFactory( (application as OctalSuntimeApplication).database.locationDao() )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locations)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // allow navigating back
        setSupportActionBar(findViewById<Toolbar>(R.id.locations_toolbar))

        val recyclerView = findViewById<RecyclerView>(R.id.locations_recycler_view)
        val adapter = LocationAdapter()
        adapter.onClickListener = View.OnClickListener { view ->
            // select this location. Return it to the callign activity.
            val holder = recyclerView.findContainingViewHolder(view) as LocationAdapter.LocationViewHolder
            val location = holder.location
            val returnIntent = Intent()
            returnIntent.data = Uri.Builder().scheme("geo").opaquePart("0,0?q=%f,%f(%s)".format(location.latitude, location.longitude, location.name)).build()
            setResult(RESULT_OK, returnIntent)
            finish()
        }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_locations, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.add_location -> {
                // show the search location dialog:
                val adapter = WikidataGeoListAdapter(this)
                val newFragment = AddLocationAutoCompletionDialogFragment(adapter)
                newFragment.show(supportFragmentManager, "location finder")
                true
            }
            R.id.clear_locations -> {
                viewModel.clear()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onLocationChosen(dialog: DialogFragment, chosenObject: Location) {
        viewModel.addNewLocation(chosenObject)
    }

    /**
     * ActivityResultContract that delivers back to the caller a user-selected Location
     */
    class SelectLocationContract : ActivityResultContract<Nothing?, Location?>() {
        override fun createIntent(context: Context, input: Nothing?): Intent {
            return Intent(context, LocationsActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Location? {
            when(resultCode) {
                RESULT_OK -> {
                    if (intent?.data?.scheme == "geo") {
                        val schemeSpecificPart = intent.data?.schemeSpecificPart
                        // check if the schemeSpecificPart matches the URI scheme we expect (lat,lon(label))
                        if ((schemeSpecificPart != null) && schemeSpecificPart.matches(Regex("^0,0\\?q=-?[0-9]+(\\.[0-9]*)?,-?[0-9]+(.[0-9]*)?\\(.*\\)$"))) {
                            val (lat, lon, labelWithParens) = schemeSpecificPart.substring("0,0?q=".length)
                                .split(',','(',')', limit=3)
                            val label = labelWithParens.dropLast(1)
                            return Location(label, lat.toFloat(), lon.toFloat())
                        }
                    }
                }
            }
            return null
        }

    }

}