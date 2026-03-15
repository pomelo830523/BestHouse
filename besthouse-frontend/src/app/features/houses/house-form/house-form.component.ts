import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HouseService } from '../../../core/services/house.service';
import { FloodRisk, FLOOD_RISK_LABELS, ParkingType, PARKING_TYPE_LABELS } from '../../../core/models/house.model';

@Component({
  selector: 'app-house-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './house-form.component.html',
  styleUrls: ['./house-form.component.scss'],
})
export class HouseFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = false;
  houseId: number | null = null;
  isSubmitting = false;
  errorMessage = '';

  // 原始每坪單價
  pricePerPingWithParking: number | null = null;
  pricePerPingWithoutParking: number | null = null;

  // 折扣後計算
  discountedTotalPrice: number | null = null;
  discountedPricePerPingWithParking: number | null = null;
  discountedPricePerPingWithoutParking: number | null = null;

  // 實價登錄比較
  registryVsOriginalDiffPct: number | null = null;
  registryVsDiscountedDiffPct: number | null = null;

  readonly parkingTypes: ParkingType[] = ['NONE', 'FLAT', 'RAMP_FLAT', 'MECHANICAL', 'RAMP_MECHANICAL'];
  readonly parkingLabels = PARKING_TYPE_LABELS;
  readonly floodRisks: FloodRisk[] = ['LOW', 'MEDIUM', 'HIGH'];
  readonly floodRiskLabels = FLOOD_RISK_LABELS;

  constructor(
    private fb: FormBuilder,
    private houseService: HouseService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    const id = this.route.snapshot.paramMap.get('houseId');
    if (id) {
      this.isEditMode = true;
      this.houseId = +id;
      this.loadHouse(this.houseId);
    }

    this.form.valueChanges.subscribe(() => this.recalcPrices());
  }

  private buildForm(): void {
    this.form = this.fb.group({
      nickname: ['', [Validators.required, Validators.maxLength(100)]],
      address: [''],
      communityName: [''],
      builder: [''],
      houseAgeYear: [null],
      floor: [null],
      totalFloor: [null],
      buildAreaPing: [null, [Validators.min(0.01)]],
      indoorPing: [null, [Validators.min(0.01)]],
      bedroomCount: [null],
      livingRoomCount: [null],
      bathroomCount: [null],
      totalPrice: [null, [Validators.required, Validators.min(1)]],
      hasVisited: [false],
      discountPercent: [null],
      estimatedRegistryPrice: [null],
      parkingType: ['NONE'],
      parkingPrice: [0],
      parkingPing: [null],
      monthlyFee: [null],
      note: [''],
      // 看房評估欄位（tri-state: '' = 未查, 'true' = 是, 'false' = 否）
      hasMoldOrLeak: [''],
      isFloorLevelOk: [''],
      isDoorWindowOk: [''],
      isWaterPressureOk: [''],
      electricCapacity: [null],
      isHaunted: [''],
      isSeaSand: [''],
      isRadiation: [''],
      hasIllegalConstruction: [''],
      floodRisk: [''],
      hasNuisanceFacility: [''],
      nuisanceFacilityNote: [''],
      isManagementOk: [''],
      visitDate: [null],
      visitImpression: [''],
    });
  }

  private loadHouse(id: number): void {
    this.houseService.getById(id).subscribe({
      next: (house) => {
        this.form.patchValue({
          nickname: house.nickname,
          address: house.address,
          communityName: house.communityName,
          builder: house.builder,
          houseAgeYear: house.houseAgeYear,
          floor: house.floor,
          totalFloor: house.totalFloor,
          buildAreaPing: house.buildAreaPing,
          indoorPing: house.indoorPing,
          bedroomCount: house.bedroomCount,
          livingRoomCount: house.livingRoomCount,
          bathroomCount: house.bathroomCount,
          totalPrice: house.totalPrice,
          hasVisited: house.hasVisited,
          discountPercent: house.discountPercent,
          estimatedRegistryPrice: house.estimatedRegistryPrice,
          parkingType: house.parkingType,
          parkingPrice: house.parkingPrice,
          parkingPing: house.parkingPing,
          monthlyFee: house.monthlyFee,
          note: house.note,
          hasMoldOrLeak: this.toBoolStr(house.hasMoldOrLeak),
          isFloorLevelOk: this.toBoolStr(house.isFloorLevelOk),
          isDoorWindowOk: this.toBoolStr(house.isDoorWindowOk),
          isWaterPressureOk: this.toBoolStr(house.isWaterPressureOk),
          electricCapacity: house.electricCapacity,
          isHaunted: this.toBoolStr(house.isHaunted),
          isSeaSand: this.toBoolStr(house.isSeaSand),
          isRadiation: this.toBoolStr(house.isRadiation),
          hasIllegalConstruction: this.toBoolStr(house.hasIllegalConstruction),
          floodRisk: house.floodRisk ?? '',
          hasNuisanceFacility: this.toBoolStr(house.hasNuisanceFacility),
          nuisanceFacilityNote: house.nuisanceFacilityNote ?? '',
          isManagementOk: this.toBoolStr(house.isManagementOk),
          visitDate: house.visitDate ?? null,
          visitImpression: house.visitImpression ?? '',
        });
        this.recalcPrices();
      },
      error: () => {
        this.errorMessage = '載入房屋資料失敗';
      },
    });
  }

  private recalcPrices(): void {
    const totalPrice: number = +this.form.get('totalPrice')?.value || 0;
    const buildAreaPing: number = +this.form.get('buildAreaPing')?.value || 0;
    const parkingType: ParkingType = this.form.get('parkingType')?.value;
    const hasParking = parkingType && parkingType !== 'NONE';
    const parkingPrice: number = hasParking ? (+this.form.get('parkingPrice')?.value || 0) : 0;
    const parkingPing: number  = hasParking ? (+this.form.get('parkingPing')?.value  || 0) : 0;
    const discountPct: number  = +this.form.get('discountPercent')?.value || 0;
    const estimatedPrice: number = +this.form.get('estimatedRegistryPrice')?.value || 0;

    if (totalPrice > 0 && buildAreaPing > 0) {
      const netArea = buildAreaPing - parkingPing;

      // 原始每坪
      this.pricePerPingWithParking = +(totalPrice / buildAreaPing).toFixed(2);
      this.pricePerPingWithoutParking = netArea > 0
        ? +((totalPrice - parkingPrice) / netArea).toFixed(2)
        : null;

      // 折扣後
      const discounted = discountPct > 0
        ? +(totalPrice * (1 - discountPct / 100)).toFixed(2)
        : null;
      this.discountedTotalPrice = discounted;
      if (discounted !== null) {
        this.discountedPricePerPingWithParking = +(discounted / buildAreaPing).toFixed(2);
        this.discountedPricePerPingWithoutParking = netArea > 0
          ? +((discounted - parkingPrice) / netArea).toFixed(2)
          : null;
      } else {
        this.discountedPricePerPingWithParking = null;
        this.discountedPricePerPingWithoutParking = null;
      }

      // 實價登錄比較
      if (estimatedPrice > 0) {
        this.registryVsOriginalDiffPct = +((totalPrice - estimatedPrice) / estimatedPrice * 100).toFixed(2);
        if (discounted !== null && discounted > 0) {
          this.registryVsDiscountedDiffPct = +((discounted - estimatedPrice) / estimatedPrice * 100).toFixed(2);
        } else {
          this.registryVsDiscountedDiffPct = null;
        }
      } else {
        this.registryVsOriginalDiffPct = null;
        this.registryVsDiscountedDiffPct = null;
      }
    } else {
      this.pricePerPingWithParking = null;
      this.pricePerPingWithoutParking = null;
      this.discountedTotalPrice = null;
      this.discountedPricePerPingWithParking = null;
      this.discountedPricePerPingWithoutParking = null;
      this.registryVsOriginalDiffPct = null;
      this.registryVsDiscountedDiffPct = null;
    }
  }

  diffClass(pct: number): string {
    if (pct > 0) return 'diff--higher';
    if (pct < 0) return 'diff--lower';
    return '';
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.errorMessage = '';

    const raw = this.form.value;
    const payload = {
      nickname: raw.nickname,
      address: raw.address || undefined,
      communityName: raw.communityName || undefined,
      builder: raw.builder || undefined,
      houseAgeYear: raw.houseAgeYear || undefined,
      floor: raw.floor || undefined,
      totalFloor: raw.totalFloor || undefined,
      buildAreaPing: raw.buildAreaPing || undefined,
      indoorPing: raw.indoorPing || undefined,
      bedroomCount: raw.bedroomCount || undefined,
      livingRoomCount: raw.livingRoomCount || undefined,
      bathroomCount: raw.bathroomCount || undefined,
      totalPrice: raw.totalPrice,
      parkingType: raw.parkingType || undefined,
      parkingPrice: raw.parkingPrice || undefined,
      parkingPing: raw.parkingPing || undefined,
      monthlyFee: raw.monthlyFee || undefined,
      note: raw.note || undefined,
      hasVisited: raw.hasVisited ?? false,
      discountPercent: raw.discountPercent ?? undefined,
      estimatedRegistryPrice: raw.estimatedRegistryPrice ?? undefined,
      hasMoldOrLeak: this.fromBoolStr(raw.hasMoldOrLeak),
      isFloorLevelOk: this.fromBoolStr(raw.isFloorLevelOk),
      isDoorWindowOk: this.fromBoolStr(raw.isDoorWindowOk),
      isWaterPressureOk: this.fromBoolStr(raw.isWaterPressureOk),
      electricCapacity: raw.electricCapacity || undefined,
      isHaunted: this.fromBoolStr(raw.isHaunted),
      isSeaSand: this.fromBoolStr(raw.isSeaSand),
      isRadiation: this.fromBoolStr(raw.isRadiation),
      hasIllegalConstruction: this.fromBoolStr(raw.hasIllegalConstruction),
      floodRisk: raw.floodRisk || undefined,
      hasNuisanceFacility: this.fromBoolStr(raw.hasNuisanceFacility),
      nuisanceFacilityNote: raw.nuisanceFacilityNote || undefined,
      isManagementOk: this.fromBoolStr(raw.isManagementOk),
      visitDate: raw.visitDate || undefined,
      visitImpression: raw.visitImpression || undefined,
    };

    const request$ = this.isEditMode && this.houseId
      ? this.houseService.update(this.houseId, payload)
      : this.houseService.create(payload);

    request$.subscribe({
      next: () => {
        this.router.navigate(['/houses']);
      },
      error: () => {
        this.errorMessage = '儲存失敗，請確認輸入資料';
        this.isSubmitting = false;
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/houses']);
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  private toBoolStr(val: boolean | null | undefined): string {
    if (val === true) return 'true';
    if (val === false) return 'false';
    return '';
  }

  private fromBoolStr(val: string): boolean | null | undefined {
    if (val === 'true') return true;
    if (val === 'false') return false;
    return undefined;
  }
}
