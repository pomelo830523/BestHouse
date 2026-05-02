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
  estimatedRegistryPrice: number | null;
  status: HouseStatus;
  eliminatedReason: string;
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
  monthlyFee?: number;
  monthlyRent?: number | null;
  note?: string;
  listingUrl?: string;
  hasVisited?: boolean;
  discountPercent?: number | null;
  estimatedRegistryPrice?: number | null;
}
