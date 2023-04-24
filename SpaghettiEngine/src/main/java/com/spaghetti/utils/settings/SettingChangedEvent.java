package com.spaghetti.utils.settings;

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
    public <T> T getOldValue() {
        return (T) oldValue;
    }

    public <T> T getNewValue() {
        return (T) newValue;
    }

}
