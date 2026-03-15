export interface ScoreResult {
  houseId: number;
  nickname: string;
  address: string;
  totalScore: number;
  memberScores: Record<string, number>;
  rank: number;
}
