package at.manuelbichler.octalsuntime

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class LocationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locations)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }
}