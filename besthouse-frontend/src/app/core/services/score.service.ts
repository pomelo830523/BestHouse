import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ScoreResult } from '../models/score.model';

@Injectable({ providedIn: 'root' })
export class ScoreService {
  constructor(private http: HttpClient) {}

  getRanking(): Observable<ScoreResult[]> {
    return this.http.get<ScoreResult[]>('/api/scores');
  }
}
