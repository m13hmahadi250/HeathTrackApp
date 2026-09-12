package com.example.salat

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class CompassAccuracy {
    UNKNOWN,
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH
}

data class CompassState(
    val isSensorAvailable: Boolean = true,
    val deviceHeading: Float = 0f, // 0..360 True North Azimuth in degrees
    val magneticHeading: Float = 0f, // Raw magnetic heading
    val declination: Float = 0f, // Geomagnetic declination offset applied
    val accuracy: CompassAccuracy = CompassAccuracy.UNKNOWN,
    val accuracyCode: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val needsCalibration: Boolean = false,
    val errorMessage: String? = null
)

class QiblaCompassManager(
    context: Context,
    private var userLatitude: Double = 23.8103,
    private var userLongitude: Double = 90.4125
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var rotationVectorSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var smoothedHeading = 0f
    private var magneticDeclination = 0f
    private val alpha = 0.20f // Responsive smoothing filter

    private val _compassState = MutableStateFlow(CompassState())
    val compassState: StateFlow<CompassState> = _compassState.asStateFlow()

    private var isListening = false

    init {
        updateDeclination()
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val available = rotationVectorSensor != null || (accelerometer != null && magnetometer != null)
        if (!available) {
            _compassState.value = _compassState.value.copy(
                isSensorAvailable = false,
                errorMessage = "Compass hardware sensor not detected on this device. You can align with the exact static bearing angle shown below."
            )
        }
    }

    fun setLocation(lat: Double, lon: Double) {
        userLatitude = lat
        userLongitude = lon
        updateDeclination()
    }

    private fun updateDeclination() {
        try {
            val geoField = GeomagneticField(
                userLatitude.toFloat(),
                userLongitude.toFloat(),
                15f,
                System.currentTimeMillis()
            )
            magneticDeclination = geoField.declination
        } catch (_: Exception) {
            magneticDeclination = 0f
        }
    }

    fun startListening() {
        if (isListening || sensorManager == null) return

        val hasRotationVector = rotationVectorSensor != null
        if (hasRotationVector) {
            sensorManager.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            isListening = true
        } else if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
            isListening = true
        } else {
            _compassState.value = _compassState.value.copy(
                isSensorAvailable = false,
                errorMessage = "No compass sensor found on this hardware."
            )
        }
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                azimuthDeg = (azimuthDeg + 360f) % 360f
                updateHeading(azimuthDeg, event.accuracy)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true
                if (hasGravity && hasGeomagnetic) {
                    processOrientation(event.accuracy)
                }
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                hasGeomagnetic = true
                if (hasGravity && hasGeomagnetic) {
                    processOrientation(event.accuracy)
                }
            }
        }
    }

    private fun processOrientation(accuracy: Int) {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            gravityValues,
            geomagneticValues
        )
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthRad = orientationAngles[0]
            var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            azimuthDeg = (azimuthDeg + 360f) % 360f
            updateHeading(azimuthDeg, accuracy)
        }
    }

    private fun updateHeading(rawMagneticHeading: Float, accuracyCode: Int) {
        // Correct for True North using magnetic declination
        val rawTrueHeading = (rawMagneticHeading + magneticDeclination + 360f) % 360f

        // Smooth heading across the 360/0 wrap boundary
        val diff = ((rawTrueHeading - smoothedHeading + 180f) % 360f + 360f) % 360f - 180f
        smoothedHeading = (smoothedHeading + diff * alpha + 360f) % 360f

        val accuracyEnum = when (accuracyCode) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
            SensorManager.SENSOR_STATUS_UNRELIABLE -> CompassAccuracy.UNRELIABLE
            else -> CompassAccuracy.HIGH // Default to high if not reported
        }

        val needsCalibration = accuracyEnum == CompassAccuracy.UNRELIABLE || accuracyEnum == CompassAccuracy.LOW

        _compassState.value = _compassState.value.copy(
            isSensorAvailable = true,
            deviceHeading = smoothedHeading,
            magneticHeading = rawMagneticHeading,
            declination = magneticDeclination,
            accuracy = accuracyEnum,
            accuracyCode = accuracyCode,
            needsCalibration = needsCalibration,
            errorMessage = if (needsCalibration) "Compass accuracy is low. Move away from metals and wave phone in a figure-8." else null
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val accuracyEnum = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
            SensorManager.SENSOR_STATUS_UNRELIABLE -> CompassAccuracy.UNRELIABLE
            else -> CompassAccuracy.UNKNOWN
        }
        val needsCal = accuracyEnum == CompassAccuracy.UNRELIABLE || accuracyEnum == CompassAccuracy.LOW
        _compassState.value = _compassState.value.copy(
            accuracy = accuracyEnum,
            accuracyCode = accuracy,
            needsCalibration = needsCal
        )
    }
}
