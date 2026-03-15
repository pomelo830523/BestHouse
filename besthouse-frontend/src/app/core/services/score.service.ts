import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ScoreResult } from '../models/score.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ScoreService {
  constructor(private http: HttpClient) {}

  getRanking(): Observable<ScoreResult[]> {
    return this.http.get<ScoreResult[]>(`${environment.apiUrl}/api/scores`);
  }
}
