// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.os.Handler;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.GyroSensorMode;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.ErrorMessages;

/**
 * ![EV3 component icon](images/legoMindstormsEv3.png)
 * <p>
 * A component that provides a high-level interface to a gyro sensor on a LEGO
 * MINDSTORMS EV3 robot.
 *
 * @author jerry73204@gmail.com (jerry73204)
 * @author spaded06543@gmail.com (Alvin Chang)
 */
public class Ev3GyroSensor extends LegoMindstormsEv3Sensor implements Deleteable {
    private static final int DELAY_MILLISECONDS = 50;
    private static final int SENSOR_TYPE = 32;
    private final Runnable sensorValueChecker;
    private Handler eventHandler;
    private GyroSensorMode mode = GyroSensorMode.Angle;
    private double previousValue = -1.0;
    private boolean sensorValueChangedEventEnabled = false;

    /**
     * Creates a new Ev3GyroSensor component.
     */
    public Ev3GyroSensor(ComponentContainer container) {
        super(container, "Ev3GyroSensor");

        eventHandler = new Handler();
        sensorValueChecker = new Runnable() {
            public void run() {
                String functionName = "";

                if (bluetooth != null && bluetooth.IsConnected()) {
                    double currentValue = getSensorValue(functionName);

                    if (previousValue < 0.0) {
                        previousValue = currentValue;
                        eventHandler.postDelayed(this, DELAY_MILLISECONDS);
                        return;
                    }

                    // trigger events according to the conditions
                    if ((mode == GyroSensorMode.Rate && Math.abs(currentValue) >= 1.0)
                            || (mode == GyroSensorMode.Angle && Math.abs(currentValue - previousValue) >= 1.0)) {
                        SensorValueChanged(currentValue);
                    }

                    previousValue = currentValue;
                }

                eventHandler.postDelayed(this, DELAY_MILLISECONDS);
            }
        };
        eventHandler.post(sensorValueChecker);

        ModeAbstract(GyroSensorMode.Angle);
        SensorValueChangedEventEnabled(false);
    }

    /**
     * Returns the current angle or rotation speed based on current mode,
     * or -1 if the value cannot be read from sensor.
     */
    @SimpleFunction(description = "Returns the current angle or rotation speed based on current mode, " +
            "or -1 if the value cannot be read from sensor.")
    public double GetSensorValue() {
        String functionName = "";
        return getSensorValue(functionName);
    }

    /**
     * Specifies the mode of the sensor.
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_LEGO_EV3_GYRO_SENSOR_MODE,
            defaultValue = "angle")
    @SimpleProperty
    public void Mode(@Options(GyroSensorMode.class) String modeName) {
        // Make sure modeName is a valid GyroSensorMode.
        GyroSensorMode gyroMode = GyroSensorMode.fromUnderlyingValue(modeName);
        if (gyroMode == null) {
            form.dispatchErrorOccurredEvent(
                    this, "Mode", ErrorMessages.ERROR_EV3_ILLEGAL_ARGUMENT, modeName);
            return;
        }
        setMode(gyroMode);
    }

    /**
     * Sets the sensing mode of this gyro sensor.
     */
    @SuppressWarnings("RegularMethodName")
    public void ModeAbstract(GyroSensorMode mode) {
        setMode(mode);
    }

    /**
     * Returns the current sensing mode of this color sensor.
     */
    @SuppressWarnings({"RegularMethodName", "unused"})
    public GyroSensorMode ModeAbstract() {
        return mode;
    }

    /**
     * Returns the mode of the sensor.
     */
    @SimpleProperty(description = "The sensor mode can be a text constant of either \"rate\" or \"angle\", " +
            "which correspond to SetAngleMode or SetRateMode respectively.")
    public @Options(GyroSensorMode.class) String Mode() {
        return mode.toUnderlyingValue();
    }

    /**
     * Make the sensor read the angle.
     */
    @SimpleFunction(description = "Measures the orientation of the sensor.")
    @Deprecated
    public void SetAngleMode() {
        setMode(GyroSensorMode.Angle);
    }

    /**
     * Make the sensor read the rotation rate.
     */
    @SimpleFunction(description = "Measures the angular velocity of the sensor.")
    @Deprecated
    public void SetRateMode() {
        setMode(GyroSensorMode.Rate);
    }

    /**
     * Returns whether the SensorValueChanged event should fire when the sensor value changed.
     */
    @SimpleProperty(description = "Whether the SensorValueChanged event should fire when the sensor value changed.")
    public boolean SensorValueChangedEventEnabled() {
        return sensorValueChangedEventEnabled;
    }

    /**
     * Returns whether the SensorValueChanged event should fire when the sensor value changed.
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
            defaultValue = "False")
    @SimpleProperty
    public void SensorValueChangedEventEnabled(boolean enabled) {
        sensorValueChangedEventEnabled = enabled;
    }

    /**
     * Called then the sensor value changed.
     */
    @SimpleEvent(description = "Called then the sensor value changed.")
    public void SensorValueChanged(double sensorValue) {
        EventDispatcher.dispatchEvent(this, "SensorValueChanged", sensorValue);
    }

    private double getSensorValue(String functionName) {
        return readInputSI(functionName,
                0,
                sensorPortNumber,
                SENSOR_TYPE,
                mode.toInt());
    }

    private void setMode(GyroSensorMode newMode) {
        if (newMode != mode) {
            previousValue = -1;
        }

        mode = newMode;
    }

    // Deleteable implementation
    @Override
    public void onDelete() {
        super.onDelete();
    }
}
