package com.besthouse.entity.enums;

public enum FilterRuleType {
    /** 總價上限（萬） */
    MAX_TOTAL_PRICE,
    /** 不含車位每坪單價上限（萬/坪） */
    MAX_PRICE_PER_PING,
    /** 屋齡上限（年） */
    MAX_HOUSE_AGE,
    /** 室內坪數下限（坪） */
    MIN_INDOOR_PING,
    /** 最低樓層 */
    MIN_FLOOR,
    /** 排除的車位類型，逗號分隔，如 MECHANICAL,RAMP_MECHANICAL */
    EXCLUDE_PARKING_TYPE,
    /** 車位坪數下限（坪），有車位但坪數不足視為不符合 */
    MIN_PARKING_PING
}
