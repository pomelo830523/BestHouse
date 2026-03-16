import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ParkingType } from '../models/house.model';

export interface AiImportResult {
  nickname: string | null;
  address: string | null;
  communityName: string | null;
  builder: string | null;
  houseAgeYear: number | null;
  floor: number | null;
  totalFloor: number | null;
  buildAreaPing: number | null;
  indoorPing: number | null;
  bedroomCount: number | null;
  livingRoomCount: number | null;
  bathroomCount: number | null;
  totalPrice: number | null;
  parkingType: ParkingType | null;
  parkingPrice: number | null;
  monthlyFee: number | null;
  listingUrl: string | null;
}

@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly baseUrl = `${environment.apiUrl}/api/ai`;

  constructor(private http: HttpClient) {}

  extractFromImage(file: File): Observable<AiImportResult> {
    const formData = new FormData();
    formData.append('image', file);
    return this.http.post<AiImportResult>(`${this.baseUrl}/extract-house`, formData);
  }
}
