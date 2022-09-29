package at.manuelbichler.octalsuntime.adapter

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.manuelbichler.octalsuntime.R
import at.manuelbichler.octalsuntime.model.Location

/**
 * @param registerForContextMenu a function that will be called for each individual location view that is created.
 * The activity that creates this adapter should implement this function to register the view for context menu.
 * @param onContextMenuItemClicked a function that will be executed when a context menu item has been clicked on a location. Returns true if the click was consumed.
 * For this to work, the #registerAsContextMenuClickListener method of the holder has to be called when the context menu of a view is created.
 */
class LocationAdapter(val registerForContextMenu : (View)->Nothing?, val onContextMenuItemClicked : (MenuItem, Location)->Boolean) : ListAdapter<Location, LocationAdapter.LocationViewHolder>(LocationsComparator()){

    class LocationViewHolder(view: View, val onContextMenuItemClicked : (MenuItem, Location)->Boolean) : RecyclerView.ViewHolder(view), MenuItem.OnMenuItemClickListener {

        private val nameView: TextView = view.findViewById(R.id.location_name)
        private val geoView: TextView = view.findViewById(R.id.location_geo)
        lateinit var location : Location

        fun bind(location: Location) {
            this.location = location
            nameView.text = location.name
            geoView.text = "%.2f, %.2f".format(location.latitude, location.longitude)
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            return onContextMenuItemClicked(item, location)
        }

        fun registerAsContextMenuClickListener(
            menu: ContextMenu
        ) {
            // the menu has been created. We need to set this handler as click listener on the items:
            for(i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                menuItem.setOnMenuItemClickListener(this)
            }
        }
    }

    var onClickListener : View.OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        // create a new view
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_listitem, parent, false)
        onClickListener?.let { adapterLayout.setOnClickListener(it) }
        val holder = LocationViewHolder(adapterLayout, onContextMenuItemClicked)
        registerForContextMenu(adapterLayout)
        return holder
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LocationsComparator : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem.name == newItem.name
                    && oldItem.latitude == newItem.latitude
                    && oldItem.longitude == newItem.longitude
        }

    }
}