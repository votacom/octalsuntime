package at.manuelbichler.octalsuntime.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import at.manuelbichler.octalsuntime.R
import at.manuelbichler.octalsuntime.model.Location

class LocationAdapter(private val context: Context, private val dataset: List<Location>) : RecyclerView.Adapter<LocationAdapter.LocationViewHolder>(){
    class LocationViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.location_name)
        val latView: TextView = view.findViewById(R.id.location_lat)
        val lonView: TextView = view.findViewById(R.id.location_lon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        // create a new view
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_location, parent, false)
        return LocationViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = dataset[position]
        holder.nameView.text = location.name
        holder.latView.text = "%.2f".format(location.latitude)
        holder.lonView.text = "%.2f".format(location.longitude)
    }

    override fun getItemCount() = dataset.size

}