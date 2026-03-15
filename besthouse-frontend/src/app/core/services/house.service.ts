import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { House, HouseCreateRequest } from '../models/house.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class HouseService {
  private readonly baseUrl = `${environment.apiUrl}/api/houses`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<House[]> {
    return this.http.get<House[]>(this.baseUrl);
  }

  getById(houseId: number): Observable<House> {
    return this.http.get<House>(`${this.baseUrl}/${houseId}`);
  }

  create(request: HouseCreateRequest): Observable<House> {
    return this.http.post<House>(this.baseUrl, request);
  }

  update(houseId: number, request: HouseCreateRequest): Observable<House> {
    return this.http.put<House>(`${this.baseUrl}/${houseId}`, request);
  }

  delete(houseId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${houseId}`);
  }

  restore(houseId: number): Observable<House> {
    return this.http.post<House>(`${this.baseUrl}/${houseId}/restore`, {});
  }

  applyFilters(): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/apply-filters`, {});
  }
}
