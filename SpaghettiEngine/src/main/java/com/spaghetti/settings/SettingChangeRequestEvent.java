package com.spaghetti.settings;

import com.spaghetti.events.GameEvent;
import com.spaghetti.utils.MathUtil;

public class SettingChangeRequestEvent extends GameEvent {

    protected String settingName;
    protected final Object oldValue;
    protected Object newValue;

    public SettingChangeRequestEvent(String settingName, Object oldValue, Object newValue) {
        this.settingName = settingName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getSettingName() {
        return settingName;
    }

    public String getEngineSettingName() {
        return settingName.substring((int) MathUtil.clampMax(GameSettings.PREFIX.length(), settingName.length()));
    }

    public <T> T getOldValue() {
        return (T) oldValue;
    }

    public <T> T getNewValue() {
        return (T) newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

}
