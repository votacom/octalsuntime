package at.manuelbichler.octalsuntime.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.manuelbichler.octalsuntime.R
import at.manuelbichler.octalsuntime.model.Location

class LocationAdapter : ListAdapter<Location, LocationAdapter.LocationViewHolder>(LocationsComparator()){

    class LocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameView: TextView = view.findViewById(R.id.location_name)
        private val geoView: TextView = view.findViewById(R.id.location_geo)
        lateinit var location : Location

        fun bind(location: Location) {
            this.location = location
            nameView.text = location.name
            geoView.text = "%.2f, %.2f".format(location.latitude, location.longitude)
        }
    }

    var onClickListener : View.OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        // create a new view
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_listitem, parent, false)
        onClickListener?.let { adapterLayout.setOnClickListener(it) }
        return LocationViewHolder(adapterLayout)
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