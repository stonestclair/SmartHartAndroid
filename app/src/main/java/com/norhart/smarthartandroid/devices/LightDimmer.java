package com.norhart.smarthartandroid.devices;

import com.norhart.smarthartandroid.ZWaveDevice;

/**
 * Created by Stone on 3/9/2018.
 */

public class LightDimmer extends ZWaveDevice {
    private int dimLevel;

    public int getDimLevel() {
        return dimLevel;
    }

    public void setDimLevel(int dimLevel) {
        if (dimLevel < 0){
            this.dimLevel = 0;
        }
        else if (dimLevel > 100) {
            this.dimLevel = 100;
        }
        else {
            this.dimLevel = dimLevel;
        }
    }

    public void setDimLevel (LightSwitch.State state) {
        switch (state){
            case ON:
                this.dimLevel = 100;
                break;
            case OFF:
                this.dimLevel = 0;
                break;
        }
    }
}
