import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FilterRuleService } from '../../core/services/filter-rule.service';
import { HouseService } from '../../core/services/house.service';
import { FilterRule, FilterRuleType, FILTER_RULE_TYPE_LABELS } from '../../core/models/filter-rule.model';
import { PARKING_TYPE_LABELS, ParkingType } from '../../core/models/house.model';

@Component({
  selector: 'app-filter-rules',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './filter-rules.component.html',
  styleUrls: ['./filter-rules.component.scss'],
})
export class FilterRulesComponent implements OnInit {
  rules: FilterRule[] = [];
  form!: FormGroup;
  editingRuleId: number | null = null;
  isSubmitting = false;
  showForm = false;
  errorMessage = '';
  successMessage = '';

  readonly ruleTypes: FilterRuleType[] = [
    'MAX_TOTAL_PRICE', 'MAX_PRICE_PER_PING', 'MAX_HOUSE_AGE',
    'MIN_INDOOR_PING', 'MIN_FLOOR', 'EXCLUDE_PARKING_TYPE', 'MIN_PARKING_PING',
  ];
  readonly ruleTypeLabels = FILTER_RULE_TYPE_LABELS;
  readonly parkingTypes: ParkingType[] = ['NONE', 'FLAT', 'RAMP_FLAT', 'MECHANICAL', 'RAMP_MECHANICAL'];
  readonly parkingLabels = PARKING_TYPE_LABELS;

  // 哪些規則類型用 numValue，哪些用 strValue
  readonly numValueTypes: FilterRuleType[] = [
    'MAX_TOTAL_PRICE', 'MAX_PRICE_PER_PING', 'MAX_HOUSE_AGE', 'MIN_INDOOR_PING', 'MIN_FLOOR', 'MIN_PARKING_PING',
  ];

  constructor(
    private fb: FormBuilder,
    private filterRuleService: FilterRuleService,
    private houseService: HouseService,
  ) {}

  ngOnInit(): void {
    this.loadRules();
    this.buildForm();
  }

  private buildForm(): void {
    this.form = this.fb.group({
      ruleName: ['', Validators.required],
      ruleType: ['MAX_TOTAL_PRICE', Validators.required],
      numValue: [null],
      strValue: [''],
      isActive: [true],
    });
  }

  loadRules(): void {
    this.filterRuleService.getAll().subscribe({
      next: (data) => (this.rules = data),
      error: () => (this.errorMessage = '載入規則失敗'),
    });
  }

  openCreate(): void {
    this.editingRuleId = null;
    this.form.reset({ ruleType: 'MAX_TOTAL_PRICE', isActive: true });
    this.showForm = true;
  }

  openEdit(rule: FilterRule): void {
    this.editingRuleId = rule.ruleId;
    this.form.patchValue({
      ruleName: rule.ruleName,
      ruleType: rule.ruleType,
      numValue: rule.numValue,
      strValue: rule.strValue,
      isActive: rule.isActive,
    });
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingRuleId = null;
  }

  submitForm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    const payload = this.form.value;

    const request$ = this.editingRuleId
      ? this.filterRuleService.update(this.editingRuleId, payload)
      : this.filterRuleService.create(payload);

    request$.subscribe({
      next: () => {
        this.successMessage = this.editingRuleId ? '規則已更新' : '規則已新增';
        this.showForm = false;
        this.isSubmitting = false;
        this.loadRules();
      },
      error: () => {
        this.errorMessage = '儲存失敗';
        this.isSubmitting = false;
      },
    });
  }

  deleteRule(rule: FilterRule): void {
    if (!confirm(`確定要刪除「${rule.ruleName}」？`)) return;
    this.filterRuleService.delete(rule.ruleId).subscribe({
      next: () => {
        this.successMessage = '規則已刪除';
        this.loadRules();
      },
      error: () => (this.errorMessage = '刪除失敗'),
    });
  }

  applyFilters(): void {
    if (!confirm('套用所有啟用的規則後，不符合的房屋將被標記淘汰（可手動恢復）。確定執行？')) return;
    this.houseService.applyFilters().subscribe({
      next: (result) => {
        this.successMessage = `已完成：${result.totalHouses} 間中淘汰 ${result.eliminatedCount} 間`;
      },
      error: () => (this.errorMessage = '套用篩選失敗'),
    });
  }

  isNumType(): boolean {
    return this.numValueTypes.includes(this.form.get('ruleType')?.value);
  }
}
