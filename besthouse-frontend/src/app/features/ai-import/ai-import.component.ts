import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AiService, AiImportResult } from '../../core/services/ai.service';
import { HouseService } from '../../core/services/house.service';
import { PARKING_TYPE_LABELS } from '../../core/models/house.model';
import type { ParkingType } from '../../core/models/house.model';

@Component({
  selector: 'app-ai-import',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ai-import.component.html',
  styleUrls: ['./ai-import.component.scss'],
})
export class AiImportComponent {
  private fb           = inject(FormBuilder);
  private aiService    = inject(AiService);
  private houseService = inject(HouseService);
  private router       = inject(Router);

  state: 'idle' | 'loading' | 'preview' | 'saving' = 'idle';
  errorMessage    = '';
  previewImageUrl: string | null = null;
  isDragOver      = false;

  readonly parkingTypes: ParkingType[] = ['NONE', 'FLAT', 'RAMP_FLAT', 'MECHANICAL', 'RAMP_MECHANICAL'];
  readonly parkingLabels = PARKING_TYPE_LABELS;

  form = this.fb.group({
    nickname:        ['', Validators.required],
    communityName:   [''],
    address:         [''],
    builder:         [''],
    houseAgeYear:    [null as number | null],
    floor:           [null as number | null],
    totalFloor:      [null as number | null],
    buildAreaPing:   [null as number | null],
    indoorPing:      [null as number | null],
    bedroomCount:    [null as number | null],
    livingRoomCount: [null as number | null],
    bathroomCount:   [null as number | null],
    totalPrice:      [null as number | null, [Validators.required, Validators.min(0.1)]],
    parkingType:     ['NONE' as ParkingType],
    parkingPrice:    [0],
    monthlyFee:      [null as number | null],
    listingUrl:      [''],
  });

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.processFile(file);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files?.[0];
    if (file) this.processFile(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(): void {
    this.isDragOver = false;
  }

  private processFile(file: File): void {
    if (!file.type.startsWith('image/')) {
      this.errorMessage = '請上傳圖片檔案（JPG / PNG）';
      return;
    }
    this.errorMessage    = '';
    this.previewImageUrl = URL.createObjectURL(file);
    this.state           = 'loading';

    this.aiService.extractFromImage(file).subscribe({
      next: (result) => {
        this.fillForm(result);
        this.state = 'preview';
      },
      error: () => {
        this.errorMessage = 'AI 解析失敗，請稍後再試';
        this.state        = 'idle';
      },
    });
  }

  private fillForm(result: AiImportResult): void {
    this.form.patchValue({
      nickname:        result.nickname        ?? '',
      communityName:   result.communityName   ?? '',
      address:         result.address         ?? '',
      builder:         result.builder         ?? '',
      houseAgeYear:    result.houseAgeYear,
      floor:           result.floor,
      totalFloor:      result.totalFloor,
      buildAreaPing:   result.buildAreaPing,
      indoorPing:      result.indoorPing,
      bedroomCount:    result.bedroomCount,
      livingRoomCount: result.livingRoomCount,
      bathroomCount:   result.bathroomCount,
      totalPrice:      result.totalPrice,
      parkingType:     result.parkingType     ?? 'NONE',
      parkingPrice:    result.parkingPrice    ?? 0,
      monthlyFee:      result.monthlyFee,
      listingUrl:      result.listingUrl      ?? '',
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.state = 'saving';
    const v = this.form.value;

    this.houseService.create({
      nickname:        v.nickname!,
      communityName:   v.communityName  || undefined,
      address:         v.address        || undefined,
      builder:         v.builder        || undefined,
      houseAgeYear:    v.houseAgeYear   ?? undefined,
      floor:           v.floor          ?? undefined,
      totalFloor:      v.totalFloor     ?? undefined,
      buildAreaPing:   v.buildAreaPing  ?? undefined,
      indoorPing:      v.indoorPing     ?? undefined,
      bedroomCount:    v.bedroomCount   ?? undefined,
      livingRoomCount: v.livingRoomCount ?? undefined,
      bathroomCount:   v.bathroomCount  ?? undefined,
      totalPrice:      v.totalPrice!,
      parkingType:     v.parkingType    ?? 'NONE',
      parkingPrice:    v.parkingPrice   ?? 0,
      monthlyFee:      v.monthlyFee     ?? undefined,
      listingUrl:      v.listingUrl     || undefined,
      hasVisited:      false,
    }).subscribe({
      next:  (house) => this.router.navigate(['/houses', house.houseId, 'edit']),
      error: () => {
        this.errorMessage = '儲存失敗，請檢查欄位';
        this.state        = 'preview';
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
}
