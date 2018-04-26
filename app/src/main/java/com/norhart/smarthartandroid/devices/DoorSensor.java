package com.norhart.smarthartandroid.devices;

import com.norhart.smarthartandroid.ZWaveDevice;

/**
 * Created by Stone on 3/9/2018.
 */

public class DoorSensor extends ZWaveDevice {
    private State state;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setState(boolean state){
        if (state){
            this.state = State.OPEN;
        }
        else {
            this.state = State.CLOSED;
        }
    }

    public enum State {
        OPEN,
        CLOSED;
    }
}
