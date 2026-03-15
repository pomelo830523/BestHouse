import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RatingBatchRequest, RatingView } from '../models/rating.model';

@Injectable({ providedIn: 'root' })
export class RatingService {
  private readonly baseUrl = '/api/ratings';

  constructor(private http: HttpClient) {}

  getByHouse(houseId: number): Observable<RatingView[]> {
    return this.http.get<RatingView[]>(`${this.baseUrl}/house/${houseId}`);
  }

  saveRatings(houseId: number, request: RatingBatchRequest): Observable<RatingView[]> {
    return this.http.post<RatingView[]>(`${this.baseUrl}/house/${houseId}`, request);
  }
}
