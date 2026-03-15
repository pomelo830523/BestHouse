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
  floodRisk: FloodRisk | null;
  hasNuisanceFacility: boolean | null;
  nuisanceFacilityNote: string | null;
  isManagementOk: boolean | null;
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
  note?: string;
  hasVisited?: boolean;
  discountPercent?: number | null;
  estimatedRegistryPrice?: number | null;
}
