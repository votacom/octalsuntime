package at.manuelbichler.octalsuntime

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import at.manuelbichler.octalsuntime.model.Location
import ca.rmen.sunrisesunset.SunriseSunset
import java.time.Duration
import java.util.*
import kotlin.math.*

class ClockActivity : AppCompatActivity() {

    var currentTime : Double = 0.0 // between 0 and 1
    var sunriseTime : Double = 0.0 // between 0 and 1
    var sunsetTime : Double = 1.0 // between 0 and 1
    var latitude : Float = 0.0f // in degrees
    var longitude : Float = 0.0f // in degrees
    var locationName : String = ""

    private lateinit var preferences : SharedPreferences

    private val updateHandler = Handler(Looper.getMainLooper())

    private val locationSelectionContract = registerForActivityResult(LocationsActivity.SelectLocationContract()) {it?.let { updateLocation(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_clock)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        preferences = this.getPreferences(Context.MODE_PRIVATE)

        // start with location set in preferences:
        val defaultLocationName = getString(R.string.default_clock_location_name)
        val defaultLocationLat = getString(R.string.default_clock_location_latitude).toFloat()
        val defaultLocationLon = getString(R.string.default_clock_location_longitude).toFloat()

        if( preferences.contains( getString(R.string.clock_location_name))
            && preferences.contains( getString(R.string.clock_location_latitude))
            && preferences.contains( getString(R.string.clock_location_longitude)) ) {
            locationName = preferences.getString(getString(R.string.clock_location_name), defaultLocationName)!!
            latitude = preferences.getString(getString(R.string.clock_location_latitude), null)?.toFloat()?:defaultLocationLat
            longitude = preferences.getString(getString(R.string.clock_location_longitude), null)?.toFloat()?:defaultLocationLat
        } else {
            locationName = defaultLocationName
            latitude = defaultLocationLat
            longitude = defaultLocationLon
            with(preferences.edit()) {
                putString(getString(R.string.clock_location_name), locationName)
                putString(getString(R.string.clock_location_latitude), latitude.toString())
                putString(getString(R.string.clock_location_longitude), longitude.toString())
                apply() // save
            }
        }
        updateUiLocation()
    }

    /**
     * calculates and sets this instance's current, sunrise, and sunset relative times, given these three points in time in regular timestamps, plus a reference timestamp for solar noon.
     */
    private fun updateTimes(now : Date, sunrise : Date, sunset : Date, solarnoon : Date) {
        currentTime = getRelativeTime(now, solarnoon)
        sunriseTime = getRelativeTime(sunrise, solarnoon)
        sunsetTime = getRelativeTime(sunset, solarnoon)
    }

    /**
     * given a day's solar noon Date and a point in time, returns the relative sun time of that point in time (in range [0,1[) if it were on the same day.
     * 0 means solar midnight, 0.5 means solar noon.
     */
    private fun getRelativeTime(datetime : Date, solarNoon : Date) : Double {
        val differenceToSolarNoon = Duration.between(
            solarNoon.toInstant(),
            datetime.toInstant()
        ) // positive if now is after noon, negative if now is before noon
        val relativeDifferenceToSolarNoon =
            differenceToSolarNoon.toMillis().toDouble() / Duration.ofDays(1).toMillis()
        return (relativeDifferenceToSolarNoon + 0.5).rem(1)
    }

    /**
     * updates the clock according to lat and lon members and the location label.
     */
    private fun updateUiLocation() {
        val locationNameTextview = findViewById<TextView>(R.id.clock_location_name)
        runOnUiThread{
            locationNameTextview.text = locationName
        }
        updateClock()
    }

    /**
     * uses the stored latitude and longitude to update the clock to "now". Also updates the UI.
     */
    private fun updateClock() {
        val digitalClock = findViewById<TextView>(R.id.digital_clock)
        val clockFingers = findViewById<ClockFingersView>(R.id.clock_fingers)
        // get sun location
        with(this) {
            val now = Calendar.getInstance()
            val sunriseSunset: Array<Calendar>? = SunriseSunset.getSunriseSunset(
                now,
                latitude.toDouble(),
                longitude.toDouble()
            )
            // if null was returned, it means it's all day sun/night. Pretend sunrise=sunset=now for now and deal with this case later.
            val sunrise = sunriseSunset?.get(0)?.time ?: now.time
            val sunset = sunriseSunset?.get(1)?.time ?: now.time
            val solarNoon =
                SunriseSunset.getSolarNoon(now, latitude.toDouble(), longitude.toDouble()).time
            updateTimes(now.time, sunrise, sunset, solarNoon)
            if (sunriseSunset == null) {
                // is it all day or all night?
                val isDay = SunriseSunset.isDay(now, latitude.toDouble(), longitude.toDouble())
                sunriseTime = 0.0
                sunsetTime = if (isDay) 1.0 else 0.0
            }
        }

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
            R.id.manage_locations -> {
                locationSelectionContract.launch(null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateLocation(location : Location) {
        latitude = location.latitude
        longitude = location.longitude
        locationName = location.name
        // save in preferences:
        with(preferences.edit()) {
            putString(getString(R.string.clock_location_name), locationName)
            putString(getString(R.string.clock_location_latitude), latitude.toString())
            putString(getString(R.string.clock_location_longitude), longitude.toString())
            apply() // save
        }
        updateUiLocation()
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