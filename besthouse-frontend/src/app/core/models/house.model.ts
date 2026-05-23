export type ParkingType = 'NONE' | 'FLAT' | 'RAMP_FLAT' | 'MECHANICAL' | 'RAMP_MECHANICAL';
export type HouseStatus = 'ACTIVE' | 'ELIMINATED';
export type FloodRisk = 'LOW' | 'MEDIUM' | 'HIGH';

export const FLOOD_RISK_LABELS: Record<FloodRisk, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
};

export const PARKING_TYPE_LABELS: Record<ParkingType, string> = {
  NONE: '無',
  FLAT: '平面',
  RAMP_FLAT: '坡道平面',
  MECHANICAL: '機械',
  RAMP_MECHANICAL: '坡道機械',
};

export interface House {
  houseId: number;
  nickname: string;
  address: string;
  communityName: string;
  builder: string;
  houseAgeYear: number;
  floor: number;
  totalFloor: number;
  unitsPerFloor: number | null;
  elevatorCount: number | null;
  walkMetersToHsrZhubei: number | null;
  nearestStationToHsrZhubei: string | null;
  walkMetersToFengyuan: number | null;
  nearestStationToFengyuan: string | null;
  walkMetersToElementary: number | null;
  nearestElementarySchool: string | null;
  walkMetersToJuniorHigh: number | null;
  nearestJuniorHighSchool: string | null;
  buildAreaPing: number;
  indoorPing: number;
  bedroomCount: number;
  livingRoomCount: number;
  bathroomCount: number;
  totalPrice: number;
  parkingType: ParkingType;
  parkingPrice: number;
  parkingPing: number;
  /** 有無機車位 */
  hasMotorcycleParking: boolean | null;
  monthlyFee: number;
  monthlyRent: number | null;
  /** 每月貸款利息（元，後端計算） */
  monthlyMortgage: number | null;
  /** 每月純利息（月付 - 本金/360，元，後端計算） */
  monthlyInterest: number | null;
  /** 買房月息是租金的百分比（後端計算） */
  interestToRentRatio: number | null;
  listingUrl: string | null;
  note: string;
  hasVisited: boolean;
  discountPercent: number | null;
  /** 過去一年實登不含車位每坪下限（萬/坪，使用者手填） */
  registryPricePerPingMin: number | null;
  /** 過去一年實登不含車位每坪上限（萬/坪，使用者手填） */
  registryPricePerPingMax: number | null;
  /** 最新一筆實登不含車位每坪（萬/坪，使用者手填） */
  latestRegistryPricePerPing: number | null;
  status: HouseStatus;
  eliminatedReason: string;
  /** 可修缮缺陷觸發的警告訊息（status 仍 ACTIVE） */
  warningReason: string | null;
  pricePerPingWithParking: number;
  pricePerPingWithoutParking: number;
  createdAt: string;
  updatedAt: string;

  // 看房評估欄位
  hasMoldOrLeak: boolean | null;
  isFloorLevelOk: boolean | null;
  isDoorWindowOk: boolean | null;
  isWaterPressureOk: boolean | null;
  electricCapacity: number | null;
  isHaunted: boolean | null;
  isSeaSand: boolean | null;
  isRadiation: boolean | null;
  hasIllegalConstruction: boolean | null;
  isParkingLowestFloor: boolean | null;
  floodRisk: FloodRisk | null;
  hasNuisanceFacility: boolean | null;
  nuisanceFacilityNote: string | null;
  isManagementOk: boolean | null;
  managementNote: string | null;
  visitDate: string | null;
  visitImpression: string | null;
}

export interface HouseCreateRequest {
  nickname: string;
  address?: string;
  communityName?: string;
  builder?: string;
  houseAgeYear?: number;
  floor?: number;
  totalFloor?: number;
  unitsPerFloor?: number;
  elevatorCount?: number;
  walkMetersToHsrZhubei?: number;
  nearestStationToHsrZhubei?: string;
  walkMetersToFengyuan?: number;
  nearestStationToFengyuan?: string;
  walkMetersToElementary?: number;
  nearestElementarySchool?: string;
  walkMetersToJuniorHigh?: number;
  nearestJuniorHighSchool?: string;
  buildAreaPing?: number;
  indoorPing?: number;
  bedroomCount?: number;
  livingRoomCount?: number;
  bathroomCount?: number;
  totalPrice: number;
  parkingType?: ParkingType;
  parkingPrice?: number;
  parkingPing?: number;
  hasMotorcycleParking?: boolean | null;
  monthlyFee?: number;
  monthlyRent?: number | null;
  note?: string;
  listingUrl?: string;
  hasVisited?: boolean;
  discountPercent?: number | null;
  registryPricePerPingMin?: number | null;
  registryPricePerPingMax?: number | null;
  latestRegistryPricePerPing?: number | null;
}
