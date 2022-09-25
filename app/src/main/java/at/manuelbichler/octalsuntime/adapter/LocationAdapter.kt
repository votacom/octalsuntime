package at.manuelbichler.octalsuntime.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.manuelbichler.octalsuntime.R
import at.manuelbichler.octalsuntime.model.Location

class LocationAdapter() : ListAdapter<Location, LocationAdapter.LocationViewHolder>(LocationsComparator()){

    class LocationViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.location_name)
        val latView: TextView = view.findViewById(R.id.location_lat)
        val lonView: TextView = view.findViewById(R.id.location_lon)

        fun bind(location: Location) {
            nameView.text = location.name
            latView.text = "%.2f".format(location.latitude)
            lonView.text = "%.2f".format(location.longitude)
        }
        companion object {
            fun create(parent: ViewGroup) : LocationViewHolder {
                // create a new view
                val adapterLayout = LayoutInflater.from(parent.context)
                    .inflate(R.layout.location_listitem, parent, false)
                return LocationViewHolder(adapterLayout)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        // create a new view
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_listitem, parent, false)
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