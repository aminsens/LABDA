package ai.aminrezaei.dataloggerapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepsCounter(context: Context) : SensorEventListener {
    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var stepCount: Int = 0
    private var stepsSinceBoot: Int = 0
    private var initialStepCount: Int = -1 // Initial step count to adjust the step counting

    init {
        if (stepSensor == null) {
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                stepsSinceBoot = it.values[0].toInt()
                if (initialStepCount < 0) {
                    initialStepCount = stepsSinceBoot
                }
                stepCount = stepsSinceBoot - initialStepCount
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    fun getStepsCount(): Int {
        return stepCount
    }

    fun getStepsSinceBoot(): Int {
        return stepsSinceBoot
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }
}
