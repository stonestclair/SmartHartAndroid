package com.norhart.smarthartandroid;

/**
 * Created by Stone on 3/9/2018.
 */

public abstract class ZWaveDevice {
    private String id;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString (){
        return name;
    }
}
