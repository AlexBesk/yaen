package xyz.beskh.yaen;

import java.util.HashMap;
import java.util.Map;

public enum AppState {
    appFileSave(100),
    appFinish(101),
    appFileReset(102),
    appFileOpen(103),
    appFileSaveAndOpen(104)
    ;

    private int intValue;
    private static Map<Integer, AppState> map = new HashMap<Integer, AppState>();

    private AppState(int value) {
        this.intValue = value;
    }

    static {
        for (AppState appState : AppState.values()) {
            map.put(appState.intValue, appState);
        }
    }

    public static AppState fromInt(int intRepresentation) {
        return map.get(intRepresentation);
    }

    public int asInt() {
        return intValue;
    }
}
