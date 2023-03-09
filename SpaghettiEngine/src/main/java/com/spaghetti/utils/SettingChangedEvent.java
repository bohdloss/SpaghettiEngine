package com.spaghetti.utils;

import com.spaghetti.events.GameEvent;

public class SettingChangedEvent extends GameEvent {

    protected String settingName;
    protected final Object oldValue;
    protected Object newValue;

    public SettingChangedEvent(String settingName, Object oldValue, Object newValue) {
        this.settingName = settingName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getSettingName() {
        return settingName;
    }

    public void getSettingName(String settingName) {
        this.settingName = settingName;
    }
    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

}
