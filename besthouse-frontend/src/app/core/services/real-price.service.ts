import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RealPriceMatch, RealPriceSyncResult } from '../models/real-price.model';

@Injectable({ providedIn: 'root' })
export class RealPriceService {
  private readonly base = '/api/real-price';

  constructor(private http: HttpClient) {}

  syncAll(): Observable<RealPriceSyncResult> {
    return this.http.post<RealPriceSyncResult>(`${this.base}/sync`, null);
  }

  getMatches(houseId: number): Observable<RealPriceMatch[]> {
    return this.http.get<RealPriceMatch[]>(`${this.base}/matches/${houseId}`);
  }
}
