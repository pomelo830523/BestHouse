import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { RatingService } from '../../../core/services/rating.service';
import { MemberService } from '../../../core/services/member.service';
import { DimensionService } from '../../../core/services/dimension.service';
import { HouseService } from '../../../core/services/house.service';
import { Member } from '../../../core/models/member.model';
import { RatingDimension, RatingEntry } from '../../../core/models/rating.model';
import { House } from '../../../core/models/house.model';

@Component({
  selector: 'app-house-rate',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './house-rate.component.html',
  styleUrls: ['./house-rate.component.scss'],
})
export class HouseRateComponent implements OnInit {
  house: House | null = null;
  members: Member[] = [];
  dimensions: RatingDimension[] = [];
  form!: FormGroup;
  isLoading = true;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private houseService: HouseService,
    private ratingService: RatingService,
    private memberService: MemberService,
    private dimensionService: DimensionService,
  ) {}

  ngOnInit(): void {
    const houseId = +(this.route.snapshot.paramMap.get('houseId') || 0);

    forkJoin({
      house: this.houseService.getById(houseId),
      members: this.memberService.getAll(),
      dimensions: this.dimensionService.getAll(),
      ratings: this.ratingService.getByHouse(houseId),
    }).subscribe({
      next: ({ house, members, dimensions, ratings }) => {
        this.house = house;
        this.members = members;
        this.dimensions = dimensions;
        this.buildForm(ratings);
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = '載入資料失敗';
        this.isLoading = false;
      },
    });
  }

  private buildForm(existingRatings: any[]): void {
    const controls: Record<string, any> = {};
    for (const member of this.members) {
      for (const dimension of this.dimensions) {
        const key = this.controlKey(member.memberId, dimension.dimensionId);
        const existing = existingRatings.find(
          r => r.memberId === member.memberId && r.dimensionId === dimension.dimensionId
        );
        controls[key] = [
          existing?.score ?? null,
          [Validators.required, Validators.min(1), Validators.max(10)],
        ];
      }
    }
    this.form = this.fb.group(controls);
  }

  controlKey(memberId: number, dimensionId: number): string {
    return `${memberId}_${dimensionId}`;
  }

  getMemberWeightedScore(member: Member): number {
    let score = 0;
    for (const dimension of this.dimensions) {
      const val = +(this.form.get(this.controlKey(member.memberId, dimension.dimensionId))?.value || 0);
      score += val * dimension.weight;
    }
    return +score.toFixed(2);
  }

  getHouseTotalScore(): number {
    let total = 0;
    for (const member of this.members) {
      total += member.weight * this.getMemberWeightedScore(member);
    }
    return +total.toFixed(2);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;

    const ratings: RatingEntry[] = [];
    for (const member of this.members) {
      for (const dimension of this.dimensions) {
        const key = this.controlKey(member.memberId, dimension.dimensionId);
        ratings.push({
          memberId: member.memberId,
          dimensionId: dimension.dimensionId,
          score: +this.form.get(key)!.value,
        });
      }
    }

    this.ratingService.saveRatings(this.house!.houseId, { ratings }).subscribe({
      next: () => {
        this.successMessage = '評分已儲存';
        this.isSubmitting = false;
      },
      error: () => {
        this.errorMessage = '儲存評分失敗';
        this.isSubmitting = false;
      },
    });
  }

  back(): void {
    this.router.navigate(['/houses']);
  }
}
