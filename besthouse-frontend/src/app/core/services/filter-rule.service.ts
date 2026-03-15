import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FilterRule } from '../models/filter-rule.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FilterRuleService {
  private readonly baseUrl = `${environment.apiUrl}/api/filter-rules`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<FilterRule[]> {
    return this.http.get<FilterRule[]>(this.baseUrl);
  }

  create(rule: Partial<FilterRule>): Observable<FilterRule> {
    return this.http.post<FilterRule>(this.baseUrl, rule);
  }

  update(ruleId: number, rule: Partial<FilterRule>): Observable<FilterRule> {
    return this.http.put<FilterRule>(`${this.baseUrl}/${ruleId}`, rule);
  }

  delete(ruleId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${ruleId}`);
  }
}
