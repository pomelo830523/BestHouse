export type FilterRuleType =
  | 'MAX_TOTAL_PRICE'
  | 'MAX_PRICE_PER_PING'
  | 'MAX_HOUSE_AGE'
  | 'MIN_INDOOR_PING'
  | 'MIN_FLOOR'
  | 'EXCLUDE_PARKING_TYPE'
  | 'MIN_PARKING_PING'
  | 'MAX_HOUSEHOLD_PER_ELEVATOR_RATIO'
  | 'EXCLUDE_VISIT_ISSUES'
  | 'EXCLUDE_FATAL_VISIT_ISSUES'
  | 'WARN_REPAIRABLE_VISIT_ISSUES'
  | 'MAX_WALK_METERS_TO_HSR_ZHUBEI'
  | 'MAX_WALK_METERS_TO_FENGYUAN'
  | 'MAX_WALK_METERS_TO_ELEMENTARY'
  | 'MAX_WALK_METERS_TO_JUNIOR_HIGH'
  | 'MAX_FLOOR';

export const FILTER_RULE_TYPE_LABELS: Record<FilterRuleType, string> = {
  MAX_TOTAL_PRICE: '總價上限（萬）',
  MAX_PRICE_PER_PING: '不含車位每坪單價上限（萬/坪）',
  MAX_HOUSE_AGE: '屋齡上限（年）',
  MIN_INDOOR_PING: '室內坪數下限（坪）',
  MIN_FLOOR: '最低樓層',
  EXCLUDE_PARKING_TYPE: '排除車位類型',
  MIN_PARKING_PING: '車位坪數下限（坪）',
  MAX_HOUSEHOLD_PER_ELEVATOR_RATIO: '戶/梯比上限（每部電梯戶數）',
  EXCLUDE_VISIT_ISSUES: '（已停用）有看房問題即淘汰',
  EXCLUDE_FATAL_VISIT_ISSUES: '致命缺陷淘汰（凶宅/海砂/輻射/違建/嫌惡/淹水高）',
  WARN_REPAIRABLE_VISIT_ISSUES: '可修缮缺陷警告（漏水/門窗/水壓/車位/管委會）',
  MAX_WALK_METERS_TO_HSR_ZHUBEI: '步行至竹北高鐵公尺數上限',
  MAX_WALK_METERS_TO_FENGYUAN: '步行至新竹火車站公尺數上限',
  MAX_WALK_METERS_TO_ELEMENTARY: '步行至最近國小公尺數上限',
  MAX_WALK_METERS_TO_JUNIOR_HIGH: '步行至最近國中公尺數上限',
  MAX_FLOOR: '最高樓層',
};

export interface FilterRule {
  ruleId: number;
  ruleName: string;
  ruleType: FilterRuleType;
  numValue: number | null;
  strValue: string | null;
  isActive: boolean;
}
