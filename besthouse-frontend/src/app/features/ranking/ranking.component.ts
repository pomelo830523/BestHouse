import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ScoreService } from '../../core/services/score.service';
import { HouseService } from '../../core/services/house.service';
import { ScoreResult } from '../../core/models/score.model';
import { House } from '../../core/models/house.model';

@Component({
  selector: 'app-ranking',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './ranking.component.html',
  styleUrls: ['./ranking.component.scss'],
})
export class RankingComponent implements OnInit {
  ranking: ScoreResult[] = [];
  eliminatedHouses: House[] = [];
  memberNames: string[] = [];
  isLoading = false;
  errorMessage = '';

  constructor(
    private scoreService: ScoreService,
    private houseService: HouseService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    forkJoin({
      ranking: this.scoreService.getRanking(),
      houses: this.houseService.getAll(),
    }).subscribe({
      next: ({ ranking, houses }) => {
        this.ranking = ranking;
        this.eliminatedHouses = houses.filter(h => h.status === 'ELIMINATED');
        if (ranking.length > 0) {
          this.memberNames = Object.keys(ranking[0].memberScores);
        }
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = '載入排名失敗';
        this.isLoading = false;
      },
    });
  }

  getMemberScore(result: ScoreResult, memberName: string): string {
    const score = result.memberScores[memberName];
    return score != null ? score.toFixed(2) : '-';
  }

  getScoreClass(score: number): string {
    if (score >= 7) return 'score--high';
    if (score >= 5) return 'score--mid';
    return 'score--low';
  }
}
