package at.manuelbichler.octalsuntime

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import at.manuelbichler.octalsuntime.adapter.LocationAdapter
import at.manuelbichler.octalsuntime.model.Location
import at.manuelbichler.octalsuntime.wikidata.WikidataGeoListAdapter


class LocationsActivity : AppCompatActivity(), AddLocationAutoCompletionDialogFragment.LocationDialogListener {

    private lateinit var recyclerView : RecyclerView

    private val viewModel : LocationViewModel by viewModels {
        LocationsViewModelFactory( (application as OctalSuntimeApplication).database.locationDao() )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locations)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // allow navigating back
        setSupportActionBar(findViewById(R.id.locations_toolbar))

        recyclerView = findViewById(R.id.locations_recycler_view)
        val adapter = LocationAdapter( fun (v:View):Nothing? {
            registerForContextMenu(v)
            return null
        }, fun(item: MenuItem, location: Location) : Boolean {
            return when (item.itemId) {
                R.id.share_location -> {
                    Intent().apply {
                        data = location.toUri()
                        action = Intent.ACTION_VIEW
                    }.also {
                        try{
                            startActivity(it)
                        } catch(e : ActivityNotFoundException) {
                            Toast.makeText(this, getString(R.string.message_no_location_app), Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                R.id.delete_location -> {
                    viewModel.delete(location)
                    true
                }
                else -> false
            }

        })
        adapter.onClickListener = View.OnClickListener { view ->
            // select this location. Return it to the calling activity (the clock).
            val holder = recyclerView.findContainingViewHolder(view) as LocationAdapter.LocationViewHolder
            val returnIntent = Intent()
            returnIntent.data = holder.location.toUri()
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

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.contextmenu_location, menu)
        if(v != null && menu != null)
            ( recyclerView.findContainingViewHolder(v) as LocationAdapter.LocationViewHolder )
                .registerAsContextMenuClickListener(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_locations, menu)
        return true
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return false
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
                    return intent?.data?.let { Location.fromUri(it) }
                }
            }
            return null
        }

    }
}