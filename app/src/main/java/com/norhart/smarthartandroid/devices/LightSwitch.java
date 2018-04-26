package com.norhart.smarthartandroid.devices;

import com.norhart.smarthartandroid.ZWaveDevice;

/**
 * Created by Stone on 3/9/2018.
 */

public class LightSwitch extends ZWaveDevice {
    private State state;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setState(int state){
        if (state <= 0){
            this.state = State.OFF;
        }
        else {
            this.state = State.ON;
        }
    }

    public void setState(boolean state){
        if (state == false){
            this.state = State.OFF;
        }
        else {
            this.state = State.ON;
        }
    }

    public enum State {
        ON(true),
        OFF(false);

        private Boolean value;

        State(Boolean value) {
            this.value = value;
        }

        public Boolean getValue() {
            return value;
        }
    }
}
