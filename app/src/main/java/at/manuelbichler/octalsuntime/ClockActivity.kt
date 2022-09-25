package at.manuelbichler.octalsuntime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import ca.rmen.sunrisesunset.SunriseSunset
import java.lang.Exception
import java.time.Duration
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.*

class ClockActivity : AppCompatActivity() {

    var currentTime : Double = 0.0 // between 0 and 1
    var sunriseTime : Double = 0.0 // between 0 and 1
    var sunsetTime : Double = 1.0 // between 0 and 1
    var latitude : Double = 0.0 // in degrees
    var longitude : Double = 0.0 // in degrees

    val updateHandler = Handler(Looper.getMainLooper())

    val locationProviderBlocklist : MutableSet<String> = HashSet() // list of already-tried but failed location providers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_clock)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        val updateLocationButton = findViewById<Button>(R.id.location_button)
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { //all good. maybe granted, maybe not. todo maybe show an alert to the user if permission was denied.
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        updateLocationButton.setOnClickListener{
            Log.i("location", "location update button pressed.")
            // do we have location permission already?
            if( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED )
            {
                Log.i("location", "location access not yet granted.")
                if( shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) )
                {
                    // show an educational UI to the user TODO https://developer.android.com/training/permissions/requesting#explain
                }
                locationPermissionRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            // now, update location:
            val criteria = Criteria().apply { this.accuracy = Criteria.ACCURACY_COARSE; this.isAltitudeRequired = false }
            val providers = locationManager.getProviders(criteria, true)
            var remainingProviders = providers.minus(locationProviderBlocklist).toHashSet()
            if(remainingProviders.isEmpty()) {
                // no remaining providers? Let's reset the blocklist to be able to start anew:
                locationProviderBlocklist.clear()
                remainingProviders = providers.toHashSet()
            }
            val bestProvider = locationManager.getBestProvider(criteria, true )
            var locationUpdatePosted = false
            while (remainingProviders.isNotEmpty() && !locationUpdatePosted) {
                val tryProvider : String
                if(remainingProviders.size > 1 && bestProvider != null && remainingProviders.contains(bestProvider)) {
                    tryProvider = bestProvider
                }
                else {
                    tryProvider = remainingProviders.first()
                }
                remainingProviders.remove(tryProvider)
                try {
                    locationManager.getCurrentLocation(
                        tryProvider,
                        null,
                        { run -> run.run() }) { loc: Location? ->
                        if( loc == null ) { // block this location provider for the future:
                            Log.i("location", "location provider %s was unable to return a location. Blocking this provider.".format(tryProvider))
                            locationProviderBlocklist.add(tryProvider)
                            // and retry the location getting if it was not the last option:
                            if(remainingProviders.isNotEmpty()) {
                                updateLocationButton.callOnClick()
                            }
                        } else {
                            updateLocation(loc.latitude, loc.longitude)
                        }
                    }
                    locationUpdatePosted = true
                    Log.i("location", "location request successfully posted to provider %s".format(tryProvider))
                } catch (e:Exception) {
                    Log.w("location", "location provider %s threw exception:\n%s".format(tryProvider, e))
                }
                if(remainingProviders.isEmpty()) {
                    Log.w("location", "location providers exhausted. No location provider could provide a location. Not updating the app's location.")
                }
            }
        }

        // start with last known location:
        val criteria = Criteria().apply { this.accuracy = Criteria.ACCURACY_COARSE; this.isAltitudeRequired = false }
        val providers = locationManager.getProviders(criteria, true)
        val bestProvider = locationManager.getBestProvider(criteria, true )
        var locationFound = false
        while (providers.isNotEmpty()) {
            val tryProvider : String
            if( bestProvider != null && providers.contains(bestProvider) ) {
                tryProvider = bestProvider
            }
            else {
                tryProvider = providers.first()
            }
            providers.remove(tryProvider)
            val location = locationManager.getLastKnownLocation(tryProvider)
            if( location != null ) {
                locationFound = true
                updateLocation(location.latitude, location.longitude)
                break
            }
        }

        if(!locationFound) {
            // start with 0/0 location, until updated (todo use last known location instead):
            updateLocation(0.0, 0.0)
        }
    }

    /**
     * calculates and sets this instance's current, sunrise, and sunset relative times, given these three points in time in regular timestamps, plus a reference timestamp for solar noon.
     */
    fun updateTimes(now : Date, sunrise : Date, sunset : Date, solarnoon : Date) {
        currentTime = getRelativeTime(now, solarnoon)
        sunriseTime = getRelativeTime(sunrise, solarnoon)
        sunsetTime = getRelativeTime(sunset, solarnoon)
    }

    /**
     * given a day's solar noon Date and a point in time, returns the relative sun time of that point in time (in range [0,1[) if it were on the same day.
     * 0 means solar midnight, 0.5 means solar noon.
     */
    fun getRelativeTime(datetime : Date, solarnoon : Date) : Double {
        val differenceToSolarNoon = Duration.between(
            solarnoon.toInstant(),
            datetime.toInstant()
        ) // positive if now is after noon, negative if now is before noon
        val relativeDifferenceToSolarNoon =
            differenceToSolarNoon.toMillis().toDouble() / Duration.ofDays(1).toMillis()
        return (relativeDifferenceToSolarNoon + 0.5).rem(1)
    }

    /**
     * given the location, updates the location info in the app and updates the clock.
     */
    fun updateLocation(latitude : Double, longitude : Double ) {
        this.latitude = latitude
        this.longitude = longitude
        val locationTextview = findViewById<TextView>(R.id.location_view)
        runOnUiThread{
            locationTextview.text = "Latitude: %f\nLongitude: %f".format(latitude, longitude)
        }
        updateClock()
    }

    /**
     * uses the stored latitude and longitude to update the clock. Also updates the UI.
     */
    private fun updateClock() {
        val digitalClock = findViewById<TextView>(R.id.digital_clock)
        val clockFingers = findViewById<ClockFingersView>(R.id.clock_fingers)
        // get sun location
        val now = Calendar.getInstance()
        val sunriseSunset: Array<Calendar> = SunriseSunset.getSunriseSunset(
            now,
            latitude,
            longitude
        )
        val sunrise = sunriseSunset[0].time
        val sunset = sunriseSunset[1].time
        val solarnoon = SunriseSunset.getSolarNoon(now, latitude, longitude).time

        updateTimes(now.time, sunrise, sunset, solarnoon)

        val ocataltimeMinutes = currentTime*OCTAL_MINUTES_PER_SOLAR_DAY

        // update UI:
        runOnUiThread {
            digitalClock.text = "%03o".format((ocataltimeMinutes-0.5).roundToInt())
            clockFingers.updateTime( currentTime, sunriseTime, sunsetTime )
        }

        // schedule a timer for the next clock update. make it so that it exactly uses the remainder of the current octal second (plus one millisecond, to be safe).
        val octalsecondsUntilNextOctalSecond = 1.0 - ( currentTime * OCTAL_SECONDS_PER_SOLAR_DAY ).rem(1) // between 0 and 1
        val millisecondsUntilNextOctalSecond = octalsecondsUntilNextOctalSecond * MILLISECONDS_PER_OCTAL_SECOND
        updateHandler.postDelayed({this.updateClock()}, millisecondsUntilNextOctalSecond.roundToLong() + 1)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.add_location -> {
                startActivity(Intent(this, LocationsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    companion object {
        private const val OCTAL_HOURS_PER_SOLAR_DAY = 8
        private const val OCTAL_MINUTES_PER_SOLAR_DAY = 8*8* OCTAL_HOURS_PER_SOLAR_DAY
        private const val OCTAL_SECONDS_PER_OCTAL_MINUTE = 8*8
        private const val OCTAL_SECONDS_PER_SOLAR_DAY = OCTAL_SECONDS_PER_OCTAL_MINUTE * OCTAL_MINUTES_PER_SOLAR_DAY
        private const val MILLISECONDS_PER_OCTAL_SECOND = 24*60*60*1000.0 / OCTAL_SECONDS_PER_SOLAR_DAY
    }

    /**
     * the part of the clock that is static, i.e. never changes with time.
     */
    class ClockFaceView(context: Context, attrs: AttributeSet) : View(context, attrs) {

        override fun onDraw(canvas: Canvas) {
            val diameter = 0.98*min(canvas.width, canvas.height)
            val radius = (diameter / 2.0).toFloat()
            val centerX = canvas.width / 2.0f
            val centerY = canvas.height / 2.0f
            // white background circle:
            canvas.drawCircle(centerX, centerY, radius, Paint().apply { color = resources.getColor(R.color.white, context.theme) } )
            // circle (stroke):
            canvas.drawCircle(centerX, centerY, radius, Paint().apply { color = resources.getColor(R.color.black, context.theme); style=Paint.Style.STROKE; strokeWidth = 0.04f*radius } )
            // ticks:
            val lengthHourTick = 0.2f * radius // % of radius for hour tick length
            val lengthOuterMinuteTick = 0.1f * radius // % of radius for the outer minute tick length
            val lengthOuterMinute4Tick = 0.15f * radius // % of radius for the middle outer minute tick length (outer minute 4)
            val textSizeHour = 0.1f * radius
            for( i in 0..7) { // hour ticks. i represents the angle.
                val tickRelX = -sin(2*PI*i/8.0)
                val tickRelY = cos(2*PI*i/8.0)
                val outerX = (centerX+tickRelX*radius).toFloat()
                val outerY = (centerY+tickRelY*radius).toFloat()
                val innerX = (centerX+tickRelX*(radius-lengthHourTick)).toFloat()
                val innerY = (centerY+tickRelY*(radius-lengthHourTick)).toFloat()
                canvas.drawLine(outerX,
                    outerY,
                    innerX,
                    innerY,
                    Paint().apply { color = resources.getColor(R.color.black, context.theme); style=Paint.Style.STROKE; strokeWidth=0.02f*radius }
                )
                canvas.drawText("%o".format(i), innerX, innerY,
                    Paint().apply { color = resources.getColor(R.color.black, context.theme); textAlign=Paint.Align.CENTER; textSize=textSizeHour })

                for( j in 0..7) { // coarse minute ticks within the current hour.
                    val tickRelX = -sin(2*PI*(i+j/8.0)/8.0)
                    val tickRelY = cos(2*PI*(i+j/8.0)/8.0)
                    val outerX = (centerX+tickRelX*radius).toFloat()
                    val outerY = (centerY+tickRelY*radius).toFloat()
                    val innerX : Float
                    val innerY : Float
                    if(j != 4) {
                        innerX = (centerX+tickRelX*(radius-lengthOuterMinuteTick)).toFloat()
                        innerY= (centerY+tickRelY*(radius-lengthOuterMinuteTick)).toFloat()
                    } else { // slightly longer tick for 4
                        innerX = (centerX+tickRelX*(radius-lengthOuterMinute4Tick)).toFloat()
                        innerY= (centerY+tickRelY*(radius-lengthOuterMinute4Tick)).toFloat()
                    }

                    canvas.drawLine(outerX,
                        outerY,
                        innerX,
                        innerY,
                        Paint().apply { color = resources.getColor(R.color.black, context.theme); style=Paint.Style.STROKE; strokeWidth=0.01f*radius }
                    )
                    /*
                    canvas.drawText("%o".format(j), innerX, innerY,
                        Paint().apply { setColor(resources.getColor(R.color.grey, context.theme)); textAlign=Paint.Align.CENTER; textSize=textSizeOuterMinute })
                     */
                }
            }
        }
    }

    /**
     * the fingers of the clock, plus the sun activity arc
     */
    class ClockFingersView(context: Context, attrs: AttributeSet) : View(context, attrs) {
        var currentTime : Double = 0.0 // between 0 and 1
        var sunriseTime : Double = 0.0 // between 0 and 1
        var sunsetTime : Double = 1.0 // between 0 and 1

        override fun onDraw(canvas: Canvas) {
            val diameter = 0.98*min(canvas.width, canvas.height)
            val radius = (diameter / 2.0).toFloat()
            val startx = (canvas.width - diameter) / 2.0f
            val starty = (canvas.height - diameter) / 2.0f
            val centerX = canvas.width / 2.0f
            val centerY = canvas.height / 2.0f

            // sun activity arc:
            do {
                val sunactivityarcSize = 0.5 // so much (relative) of the radius is used for the sun arc radius
                val inboxing = (1-sunactivityarcSize)*radius // inner border on each of the 4 sides of the arc vs. the full diameter
                val startangle = sunriseTime*360.0 + 90.0
                val sweepangle = (sunsetTime-sunriseTime)*360.0
                canvas.drawArc((startx+inboxing).toFloat(),
                    (starty+inboxing).toFloat(), (startx+diameter-inboxing).toFloat(),
                    (starty+diameter-inboxing).toFloat(),
                    startangle.toFloat(), sweepangle.toFloat(), false, Paint().apply {
                    color = resources.getColor(
                            R.color.gold,
                            context.theme
                        ); style = Paint.Style.STROKE; strokeWidth = 0.5f*radius
                })
            }while(false)

            // hour finger:
            do {
                val tickRelX = -sin(2 * PI * currentTime)
                val tickRelY = cos(2 * PI * currentTime)
                val innerX = (centerX + tickRelX * 0.2 * radius).toFloat()
                val innerY = (centerY + tickRelY * 0.2 * radius).toFloat()
                val outerX = (centerX + tickRelX * 0.98 * radius).toFloat()
                val outerY = (centerY + tickRelY * 0.98 * radius).toFloat()
                canvas.drawLine(innerX,
                    innerY,
                    outerX,
                    outerY,
                    Paint().apply {
                        color =
                            resources.getColor(
                                R.color.purple_200,
                                context.theme
                            )
                        ; style = Paint.Style.STROKE; strokeWidth = 5.0f; strokeCap = Paint.Cap.ROUND
                    }
                )
            } while (false)

            // second finger: (just a wandering point, rounded to full seconds)
            do {
                val secondsTime = (( currentTime * OCTAL_MINUTES_PER_SOLAR_DAY ).rem(1) * OCTAL_SECONDS_PER_OCTAL_MINUTE ).roundToInt().toDouble() / OCTAL_SECONDS_PER_OCTAL_MINUTE
                val tickRelX = -sin(2 * PI * secondsTime)
                val tickRelY = cos(2 * PI * secondsTime)
                val lineX = (centerX + tickRelX * radius).toFloat()
                val lineY = (centerY + tickRelY * radius).toFloat()
                canvas.drawCircle(lineX,
                    lineY,
                    (0.02f*diameter).toFloat(), // radius
                    Paint().apply {
                        color =
                            resources.getColor(
                                R.color.purple_200,
                                context.theme
                            )
                        ; style = Paint.Style.STROKE; strokeWidth = 0.02f*radius
                    }
                )
            }while(false)
        }

        fun updateTime( time : Double, sunrise : Double, sunset : Double ) {
            currentTime = time
            sunriseTime = sunrise
            sunsetTime = sunset
            invalidate() // force to redraw
        }
    }
}