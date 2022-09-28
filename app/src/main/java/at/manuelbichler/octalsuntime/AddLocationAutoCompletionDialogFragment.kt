package at.manuelbichler.octalsuntime

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import at.manuelbichler.octalsuntime.model.Location
import at.manuelbichler.octalsuntime.wikidata.LocatedItem
import at.manuelbichler.octalsuntime.wikidata.WikidataGeoListAdapter

/**
 * a fragment for a dialog where the user enters names of places and auto completion functionality offers locations to pick from.
 * The auto completion functionality is backed by an adaptor extending ListAdapter & Filterable that needs to be provided.
 */
class AddLocationAutoCompletionDialogFragment<T>(private val adapter: T) : DialogFragment() where T : ListAdapter, T: Filterable, T: AddLocationAutoCompletionDialogFragment.ClickRegistrant<Location> {
    internal lateinit var listener: LocationDialogListener

    interface LocationDialogListener {
        fun onLocationChosen(dialog: DialogFragment, chosenObject: Location)
    }
    interface OnClickListener<P> {
        fun onClick( item : P )
    }
    interface ClickRegistrant<P> {
        fun addOnClickListener(l:OnClickListener<P>)
        fun removeOnClickListener(l:OnClickListener<P>)
    }

    // Override the Fragment.onAttach() method to instantiate the listener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as LocationDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement NoticeDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val textViewLayout = inflater.inflate(R.layout.dialog_add_location_auto_completion, null)
            builder.setMessage(R.string.dialog_search_location)
                // Inflate and set the layout for the dialog
                // Pass null as the parent view because its going in the dialog layout
                .setView(textViewLayout)
                .setNegativeButton(R.string.cancel, DialogInterface.OnClickListener {
                        _, _ ->
                } )
            // Create the AlertDialog object:
            val dlg = builder.create()
            // auto completion logic:
            val textView = textViewLayout.findViewById<AutoCompleteTextView>(R.id.auto_complete_location_picker)
            textView.setAdapter(adapter)
            adapter.addOnClickListener(object : AddLocationAutoCompletionDialogFragment.OnClickListener<Location> {
                override fun onClick(itemSelected: Location) {
                    listener.onLocationChosen(
                        this@AddLocationAutoCompletionDialogFragment,
                        itemSelected
                    )
                    dlg.dismiss()
                }
            })
            return dlg
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}