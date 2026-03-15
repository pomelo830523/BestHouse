export type FilterRuleType =
  | 'MAX_TOTAL_PRICE'
  | 'MAX_PRICE_PER_PING'
  | 'MAX_HOUSE_AGE'
  | 'MIN_INDOOR_PING'
  | 'MIN_FLOOR'
  | 'EXCLUDE_PARKING_TYPE'
  | 'MIN_PARKING_PING';

export const FILTER_RULE_TYPE_LABELS: Record<FilterRuleType, string> = {
  MAX_TOTAL_PRICE: '總價上限（萬）',
  MAX_PRICE_PER_PING: '不含車位每坪單價上限（萬/坪）',
  MAX_HOUSE_AGE: '屋齡上限（年）',
  MIN_INDOOR_PING: '室內坪數下限（坪）',
  MIN_FLOOR: '最低樓層',
  EXCLUDE_PARKING_TYPE: '排除車位類型',
  MIN_PARKING_PING: '車位坪數下限（坪）',
};

export interface FilterRule {
  ruleId: number;
  ruleName: string;
  ruleType: FilterRuleType;
  numValue: number | null;
  strValue: string | null;
  isActive: boolean;
}
