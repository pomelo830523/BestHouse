export interface RealPriceMatch {
  recordId: number;
  district: string;
  address: string;
  transactionDate: string;
  buildingType: string;
  totalAreaPing: number;
  floorDesc: string;
  totalFloor: number | null;
  bedroomCount: number | null;
  livingRoomCount: number | null;
  bathroomCount: number | null;
  houseAgeYear: number | null;
  hasElevator: boolean | null;
  hasManagement: boolean | null;
  totalPriceWan: number;
  parkingPriceWan: number | null;
  parkingAreaPing: number | null;
  pricePerPingWan: number;
  similarityScore: number;
  similarityNote: string;
}

export interface RealPriceSyncResult {
  success: boolean;
  counts: Record<string, number>;
  total: number;
  message?: string;
}
