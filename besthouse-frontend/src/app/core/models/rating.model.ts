export interface RatingDimension {
  dimensionId: number;
  dimensionName: string;
  description: string;
  weight: number;
  sortOrder: number;
  isActive: boolean;
}

export interface RatingView {
  ratingId: number;
  memberId: number;
  memberName: string;
  dimensionId: number;
  dimensionName: string;
  score: number;
}

export interface RatingEntry {
  memberId: number;
  dimensionId: number;
  score: number;
}

export interface RatingBatchRequest {
  ratings: RatingEntry[];
}
