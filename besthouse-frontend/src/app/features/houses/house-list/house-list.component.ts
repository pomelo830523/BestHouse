import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HouseService } from '../../../core/services/house.service';
import { RealPriceService } from '../../../core/services/real-price.service';
import { House } from '../../../core/models/house.model';
import { ColResizableDirective } from '../../../core/directives/col-resizable.directive';

type SortableColumn =
  | 'buildAreaPing' | 'indoorPing' | 'parkingPing' | 'commonAreaRatio'
  | 'totalPrice' | 'parkingPrice' | 'pricePerPingWithParking' | 'pricePerPingWithoutParking'
  | 'discountedTotalPrice' | 'discountedPricePerPingWithoutParking'
  | 'monthlyFee' | 'monthlyMortgage' | 'monthlyInterest' | 'interestToRentRatio'
  | 'houseAgeYear' | 'floor'
  | 'originalRegistryDiff' | 'discountedRegistryDiff'
  | 'hasVisited' | 'visitIssue';

type ColumnKey = SortableColumn | 'layout';

const COLUMN_LABELS: Record<ColumnKey, string> = {
  buildAreaPing: '總坪',
  indoorPing: '室內坪',
  parkingPing: '車位坪',
  commonAreaRatio: '公設比',
  totalPrice: '總價',
  parkingPrice: '車位價',
  pricePerPingWithParking: '含車位單價',
  pricePerPingWithoutParking: '不含車位單價',
  discountedTotalPrice: '折扣後總價',
  discountedPricePerPingWithoutParking: '折扣後不含車位每坪',
  monthlyFee: '管理費',
  monthlyMortgage: '月付房貸',
  monthlyInterest: '月付利息',
  interestToRentRatio: '息租比',
  houseAgeYear: '屋齡',
  floor: '樓層',
  layout: '格局',
  originalRegistryDiff: '開價 vs 實登',
  discountedRegistryDiff: '折扣後 vs 實登',
  hasVisited: '看房',
  visitIssue: '看房問題',
};

const TOGGLEABLE_COLUMNS: ColumnKey[] = [
  'buildAreaPing', 'indoorPing', 'parkingPing', 'commonAreaRatio',
  'totalPrice', 'parkingPrice', 'pricePerPingWithParking', 'pricePerPingWithoutParking',
  'discountedTotalPrice', 'discountedPricePerPingWithoutParking',
  'monthlyFee', 'monthlyMortgage', 'monthlyInterest', 'interestToRentRatio',
  'houseAgeYear', 'floor', 'layout',
  'originalRegistryDiff', 'discountedRegistryDiff',
  'hasVisited', 'visitIssue',
];

const DEFAULT_VISIBLE: Record<ColumnKey, boolean> = {
  buildAreaPing: true,
  indoorPing: true,
  parkingPing: false,
  commonAreaRatio: true,
  totalPrice: true,
  parkingPrice: false,
  pricePerPingWithParking: false,
  pricePerPingWithoutParking: true,
  discountedTotalPrice: true,
  discountedPricePerPingWithoutParking: true,
  monthlyFee: true,
  monthlyMortgage: false,
  monthlyInterest: false,
  interestToRentRatio: false,
  houseAgeYear: true,
  floor: true,
  layout: true,
  originalRegistryDiff: true,
  discountedRegistryDiff: true,
  hasVisited: true,
  visitIssue: true,
};

const STORAGE_KEY = 'besthouse-column-visibility';

@Component({
  selector: 'app-house-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ColResizableDirective],
  templateUrl: './house-list.component.html',
  styleUrls: ['./house-list.component.scss'],
})
export class HouseListComponent implements OnInit {
  houses: House[] = [];
  isLoading = false;
  isSyncing = false;
  errorMessage = '';
  successMessage = '';
  sortColumn: SortableColumn | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';
  showColumnPanel = false;

  readonly toggleableColumns = TOGGLEABLE_COLUMNS;
  readonly columnLabels = COLUMN_LABELS;
  columnVisible: Record<ColumnKey, boolean> = { ...DEFAULT_VISIBLE };

  constructor(
    private houseService: HouseService,
    private realPriceService: RealPriceService,
  ) {}

  ngOnInit(): void {
    this.loadColumnVisibility();
    this.loadHouses();
  }

  private loadColumnVisibility(): void {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as Partial<Record<ColumnKey, boolean>>;
        this.columnVisible = { ...DEFAULT_VISIBLE, ...parsed };
      }
    } catch {
      // ignore parse errors
    }
  }

  private saveColumnVisibility(): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.columnVisible));
    } catch {
      // ignore storage errors
    }
  }

  isVisible(col: ColumnKey): boolean {
    return this.columnVisible[col];
  }

  toggleColumn(col: ColumnKey): void {
    this.columnVisible = { ...this.columnVisible, [col]: !this.columnVisible[col] };
    this.saveColumnVisibility();
  }

  loadHouses(): void {
    this.isLoading = true;
    this.houseService.getAll().subscribe({
      next: (data) => {
        this.houses = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = '載入房屋清單失敗';
        this.isLoading = false;
      },
    });
  }

  deleteHouse(house: House): void {
    if (!confirm(`確定要刪除「${house.nickname}」嗎？`)) return;
    this.houseService.delete(house.houseId).subscribe({
      next: () => {
        this.successMessage = `已刪除「${house.nickname}」`;
        this.loadHouses();
      },
      error: () => { this.errorMessage = '刪除失敗'; },
    });
  }

  restoreHouse(house: House): void {
    this.houseService.restore(house.houseId).subscribe({
      next: () => {
        this.successMessage = `已恢復「${house.nickname}」為有效狀態`;
        this.loadHouses();
      },
      error: () => { this.errorMessage = '恢復失敗'; },
    });
  }

  syncRealPrice(): void {
    if (!confirm('將下載新竹縣 + 新竹市的實價登錄資料（約需 10~30 秒），確定開始？')) return;
    this.isSyncing = true;
    this.errorMessage = '';
    this.realPriceService.syncAll().subscribe({
      next: (result) => {
        this.successMessage = `實價登錄同步完成，共 ${result.total} 筆（新竹縣 ${result.counts['J'] ?? 0} 筆、新竹市 ${result.counts['O'] ?? 0} 筆）`;
        this.isSyncing = false;
      },
      error: () => {
        this.errorMessage = '同步失敗，請確認網路連線';
        this.isSyncing = false;
      },
    });
  }

  applyFilters(): void {
    this.isLoading = true;
    this.houseService.applyFilters().subscribe({
      next: (result) => {
        this.successMessage = `篩選完成：共 ${result.totalHouses} 間，淘汰 ${result.eliminatedCount} 間，有效 ${result.activeCount} 間`;
        this.isLoading = false;
        this.loadHouses();
      },
      error: () => {
        this.errorMessage = '套用篩選條件失敗';
        this.isLoading = false;
      },
    });
  }

  sortBy(column: SortableColumn): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
  }

  sortIcon(column: SortableColumn): string {
    if (this.sortColumn !== column) return '↕';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  private sortValue(house: House, col: SortableColumn): number {
    switch (col) {
      case 'commonAreaRatio':
        return this.commonAreaRatio(house) ?? -Infinity;
      case 'discountedTotalPrice':
        return this.discountedTotalPrice(house) ?? -Infinity;
      case 'discountedPricePerPingWithoutParking':
        return this.discountedPricePerPingWithoutParking(house) ?? -Infinity;
      case 'originalRegistryDiff':
        return this.originalRegistryDiffPct(house) ?? -Infinity;
      case 'discountedRegistryDiff':
        return this.discountedRegistryDiffPct(house) ?? -Infinity;
      case 'hasVisited':
        return house.hasVisited ? 1 : 0;
      case 'visitIssue': {
        const issue = this.hasAnyVisitIssue(house);
        return issue === null ? -1 : issue ? 1 : 0;
      }
      default:
        return (house as any)[col] ?? -Infinity;
    }
  }

  private sortedHouses(list: House[]): House[] {
    if (!this.sortColumn) return list;
    const col = this.sortColumn;
    const dir = this.sortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => (this.sortValue(a, col) - this.sortValue(b, col)) * dir);
  }

  get activeHouses(): House[] {
    return this.sortedHouses(this.houses.filter(h => h.status === 'ACTIVE'));
  }

  get eliminatedHouses(): House[] {
    return this.sortedHouses(this.houses.filter(h => h.status === 'ELIMINATED'));
  }

  /** 公設比 = (總坪 - 室內坪 - 車位坪) / 總坪 × 100（車位視為獨立產權，不算公設） */
  commonAreaRatio(house: House): number | null {
    if (!house.buildAreaPing || house.buildAreaPing <= 0 || house.indoorPing == null) return null;
    const parkingPing = house.parkingPing ?? 0;
    return +((house.buildAreaPing - house.indoorPing - parkingPing) / house.buildAreaPing * 100).toFixed(1);
  }

  /** 開價 vs 實價登錄 */
  originalRegistryDiffPct(house: House): number | null {
    if (!house.estimatedRegistryPrice || house.estimatedRegistryPrice <= 0) return null;
    return +((house.totalPrice - house.estimatedRegistryPrice) / house.estimatedRegistryPrice * 100).toFixed(1);
  }

  /** 折扣後 vs 實價登錄（無折扣時回 null） */
  discountedRegistryDiffPct(house: House): number | null {
    if (!house.estimatedRegistryPrice || house.estimatedRegistryPrice <= 0) return null;
    if (!house.discountPercent || house.discountPercent <= 0) return null;
    const discounted = house.totalPrice * (1 - house.discountPercent / 100);
    return +((discounted - house.estimatedRegistryPrice) / house.estimatedRegistryPrice * 100).toFixed(1);
  }

  /** 折扣後總價（無折扣時回 null） */
  discountedTotalPrice(house: House): number | null {
    if (!house.discountPercent || house.discountPercent <= 0) return null;
    return +(house.totalPrice * (1 - house.discountPercent / 100)).toFixed(1);
  }

  /** 折扣後不含車位每坪（無折扣或無建坪時回 null） */
  discountedPricePerPingWithoutParking(house: House): number | null {
    const discounted = this.discountedTotalPrice(house);
    if (discounted === null) return null;
    if (!house.buildAreaPing || house.buildAreaPing <= 0) return null;
    return +((discounted - (house.parkingPrice ?? 0)) / house.buildAreaPing).toFixed(2);
  }

  /**
   * 看房評估是否有任何問題。
   * 未看房回 null；已看房則檢查各評估欄位是否有異常值。
   */
  hasAnyVisitIssue(house: House): boolean | null {
    if (!house.hasVisited) return null;
    const issues = [
      house.hasMoldOrLeak === true,
      house.isFloorLevelOk === false,
      house.isDoorWindowOk === false,
      house.isWaterPressureOk === false,
      house.isHaunted === true,
      house.isSeaSand === true,
      house.isRadiation === true,
      house.hasIllegalConstruction === true,
      house.isParkingLowestFloor === true,
      house.hasNuisanceFacility === true,
      house.isManagementOk === false,
      house.floodRisk === 'HIGH',
    ];
    return issues.some(Boolean);
  }

  mapsUrl(address: string): string {
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address)}`;
  }

  diffClass(pct: number): string {
    if (pct > 0) return 'diff--higher';
    if (pct < 0) return 'diff--lower';
    return '';
  }
}
