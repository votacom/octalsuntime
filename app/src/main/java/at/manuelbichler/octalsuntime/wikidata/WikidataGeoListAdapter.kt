package at.manuelbichler.octalsuntime.wikidata

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.HandlerCompat
import at.manuelbichler.octalsuntime.AddLocationAutoCompletionDialogFragment
import at.manuelbichler.octalsuntime.R
import at.manuelbichler.octalsuntime.model.Location
import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.*


/**
 * filterable adapter that is backed by Wikidata.
 * At all times, it keeps the current search string, and the current search results (dataset).
 */
class WikidataGeoListAdapter(val context: Context) : BaseAdapter(), Filterable, AddLocationAutoCompletionDialogFragment.ClickRegistrant<Location> {
    private var searchString : String = ""
    private val dataset = Collections.synchronizedList(emptyList<LocatedItem>().toMutableList())
    /**
     * the lookup backlog is a list of items for which we haven't yet obtained a location.
     * Obtaining these locations is done in async HTTPS POST calls to the wikidata SPARQL engine.
     */
    private val lookupBacklog = Collections.synchronizedList(emptyList<Item>().toMutableList())
    private val runningSparqlRequests = Collections.synchronizedSet(emptySet<CancellableRequest>().toMutableSet())

    private val uiHandler : Handler = HandlerCompat.createAsync(context.mainLooper)

    private val onClickListeners : MutableSet<AddLocationAutoCompletionDialogFragment.OnClickListener<Location>> = emptySet<AddLocationAutoCompletionDialogFragment.OnClickListener<Location>>().toMutableSet()

    override fun getCount() = dataset.size

    override fun getItem(p0: Int): LocatedItem = dataset[p0]

    override fun getItemId(p0: Int) = p0.toLong()

    override fun hasStableIds() = false

    override fun addOnClickListener(l:AddLocationAutoCompletionDialogFragment.OnClickListener<Location>) {
        onClickListeners.add(l)
    }
    override fun removeOnClickListener(l:AddLocationAutoCompletionDialogFragment.OnClickListener<Location>) {
        onClickListeners.remove(l)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView?: LayoutInflater.from(context).inflate(R.layout.wikidatalocation_listitem, parent, false)
        val item = getItem(position)
        view.findViewById<TextView>(R.id.label).text = item.item.label
        view.findViewById<TextView>(R.id.description).text = item.item.description
        view.findViewById<TextView>(R.id.source).text = item.location.source
        // nice feature: when long-pressing the item, open the web browser with it:
        view.setOnLongClickListener { v ->
            v.context.startActivity(Intent().apply { action = Intent.ACTION_VIEW; data = Uri.parse("https://www.wikidata.org/wiki/Q${item.item.id}")})
            false
        }
        view.setOnClickListener { v ->
            for(l in onClickListeners) {
                l.onClick(Location(item.item.label, item.location.lat, item.location.lon))
            }
        }
        return view
    }

    private fun update(items: Collection<Item>) {
        synchronized(dataset) {
            dataset.clear()
            for(req in runningSparqlRequests) {
                req.cancel()
            }
            runningSparqlRequests.clear()
            lookupBacklog.clear()
            lookupBacklog.addAll(items)
        }
        spawnLocationRequest()
        notifyDataSetChanged()
    }

    private fun spawnLocationRequest() {
        synchronized(lookupBacklog) {
            for(item in lookupBacklog) {
                val query = context.getString(R.string.sparqlQuery).format(item.id)
                Log.i("wikidata", "sparkling following query:\n$query")
                val cancellableSparqlRequest = "https://query.wikidata.org/sparql"
                    .httpPost(listOf("format" to "json", "query" to query))
                    .responseString { _, _, result ->
                        when(result) {
                            is Result.Failure -> {
                                if(result.getException() is SocketTimeoutException) {
                                    //TODO maybe retry
                                }
                            }
                            is Result.Success -> {
                                val locations = CoordinateLocation.fromJSON(JSONObject(result.get()))
                                synchronized(lookupBacklog) {
                                    lookupBacklog.remove(item)
                                }
                                if(locations.isNotEmpty()) {
                                    synchronized(dataset) {
                                        for (l in locations) {
                                            Log.i("wikidata", "adding to dataset: $item $l")
                                            dataset.add(LocatedItem(item, l))
                                        }
                                    }
                                }
                                this@WikidataGeoListAdapter.uiHandler.post { notifyDataSetChanged() }
                            }
                        }
                    }
                runningSparqlRequests.add(cancellableSparqlRequest)
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            // FilterResults.values object must be of type List<Item>
            override fun performFiltering(p0: CharSequence?): FilterResults {
                Log.i("wikidata", "requestd to perform filtering for search: $p0")
                val query = p0?.toString() ?: ""
                searchString = query
                if(query.isEmpty()) {
                    return FilterResults().apply { values = emptyList<Item>(); count = 0 }
                }
                val json = JSONObject("https://www.wikidata.org/w/api.php".httpGet(listOf("action" to "wbsearchentities", "language" to "en", "format" to "json", "search" to query)).responseString().third.component1())
                val jsonResults = json.getJSONArray("search")
                val items = emptyList<Item>().toMutableList()
                for(i in 0 until jsonResults.length()) {
                    val jsonResult = jsonResults.getJSONObject(i)
                    val idOk = jsonResult.getString("id").matches(Regex("Q[0-9]+"))
                    if(!idOk) throw Exception()
                    val id = jsonResult.getString("id").substring(1).toInt()
                    val label = jsonResult.getString("label")
                    val description = jsonResult.optString("description", null)
                    items.add(Item(id, label, description))
                }
                return FilterResults().apply { values = items; count = items.size }
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                // if the search string is outdated, ignore the result:
                if((p0?.toString() ?: "") == searchString && p1 != null && p1.values != null) {
                    Log.i("wikidata", "publishing results for search: $p0")
                    update(p1?.values as Collection<Item>)
                }
            }
        }
    }
}