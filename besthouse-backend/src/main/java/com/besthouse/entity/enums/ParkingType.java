package com.besthouse.entity.enums;

public enum ParkingType {
    NONE("無"),
    FLAT("平面"),
    RAMP_FLAT("坡道平面"),
    MECHANICAL("機械"),
    RAMP_MECHANICAL("坡道機械");

    private final String displayName;

    ParkingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
