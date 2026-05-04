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
    MIN_PARKING_PING,
    /** 戶/梯比上限（每部電梯服務戶數，= 每層戶數 / 電梯數） */
    MAX_HOUSEHOLD_PER_ELEVATOR_RATIO,
    /** 已看房且發現任何看房問題即淘汰（不需數值） */
    EXCLUDE_VISIT_ISSUES,
    /** 步行至竹北高鐵最近站點公尺數上限 */
    MAX_WALK_METERS_TO_HSR_ZHUBEI,
    /** 步行至新竹火車站最近站點公尺數上限 */
    MAX_WALK_METERS_TO_FENGYUAN,
    /** 步行至最近國小公尺數上限 */
    MAX_WALK_METERS_TO_ELEMENTARY,
    /** 步行至最近國中公尺數上限 */
    MAX_WALK_METERS_TO_JUNIOR_HIGH,
    /** 最高樓層上限（超過即淘汰） */
    MAX_FLOOR
}
