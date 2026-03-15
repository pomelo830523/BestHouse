import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RatingDimension } from '../models/rating.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DimensionService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<RatingDimension[]> {
    return this.http.get<RatingDimension[]>(`${environment.apiUrl}/api/dimensions`);
  }
}
