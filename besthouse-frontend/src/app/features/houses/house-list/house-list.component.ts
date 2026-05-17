import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HouseService } from '../../../core/services/house.service';
import { House } from '../../../core/models/house.model';
import { ColResizableDirective } from '../../../core/directives/col-resizable.directive';

type SortableColumn =
  | 'nickname' | 'address'
  | 'buildAreaPing' | 'indoorPing' | 'parkingPing' | 'commonAreaRatio'
  | 'totalPrice' | 'parkingPrice' | 'pricePerPingWithParking' | 'pricePerPingWithoutParking'
  | 'discountedTotalPrice' | 'discountedPricePerPingWithoutParking'
  | 'monthlyFee' | 'monthlyMortgage' | 'monthlyInterest' | 'interestToRentRatio'
  | 'houseAgeYear' | 'floor' | 'unitsPerElevator'
  | 'walkMetersToHsrZhubei' | 'walkMetersToFengyuan'
  | 'walkMetersToElementary' | 'walkMetersToJuniorHigh'
  | 'registryPricePerPingMin' | 'registryPricePerPingMax' | 'latestRegistryPricePerPing'
  | 'hasVisited' | 'visitIssue';

type ColumnKey = SortableColumn | 'layout' | 'registryPricePerPingRange';

const COLUMN_LABELS: Record<ColumnKey, string> = {
  nickname: '代號',
  address: '地址',
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
  unitsPerElevator: '戶/梯',
  walkMetersToHsrZhubei: '步行→高鐵',
  walkMetersToFengyuan: '步行→火車站',
  walkMetersToElementary: '步行→國小',
  walkMetersToJuniorHigh: '步行→國中',
  layout: '格局',
  registryPricePerPingMin: '實登每坪下限',
  registryPricePerPingMax: '實登每坪上限',
  registryPricePerPingRange: '實登每坪範圍',
  latestRegistryPricePerPing: '最新實登每坪',
  hasVisited: '看房',
  visitIssue: '看房問題',
};

const TOGGLEABLE_COLUMNS: ColumnKey[] = [
  'buildAreaPing', 'indoorPing', 'parkingPing', 'commonAreaRatio',
  'totalPrice', 'parkingPrice', 'pricePerPingWithParking', 'pricePerPingWithoutParking',
  'discountedTotalPrice', 'discountedPricePerPingWithoutParking',
  'monthlyFee', 'monthlyMortgage', 'monthlyInterest', 'interestToRentRatio',
  'houseAgeYear', 'floor', 'layout',
  'walkMetersToHsrZhubei', 'walkMetersToFengyuan',
  'walkMetersToElementary', 'walkMetersToJuniorHigh',
  'registryPricePerPingRange', 'latestRegistryPricePerPing',
  'hasVisited', 'visitIssue', 'unitsPerElevator',
];

const DEFAULT_VISIBLE: Record<ColumnKey, boolean> = {
  nickname: true,
  address: true,
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
  unitsPerElevator: true,
  walkMetersToHsrZhubei: true,
  walkMetersToFengyuan: true,
  walkMetersToElementary: true,
  walkMetersToJuniorHigh: true,
  layout: true,
  registryPricePerPingMin: false,
  registryPricePerPingMax: false,
  registryPricePerPingRange: true,
  latestRegistryPricePerPing: true,
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
  errorMessage = '';
  successMessage = '';
  sortColumn: SortableColumn | null = 'pricePerPingWithoutParking';
  sortDirection: 'asc' | 'desc' = 'asc';
  showColumnPanel = false;

  readonly toggleableColumns = TOGGLEABLE_COLUMNS;
  readonly columnLabels = COLUMN_LABELS;
  columnVisible: Record<ColumnKey, boolean> = { ...DEFAULT_VISIBLE };

  constructor(
    private houseService: HouseService,
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

  private sortValue(house: House, col: SortableColumn): number | string {
    switch (col) {
      case 'nickname':
        return house.nickname ?? '';
      case 'address':
        return house.address ?? '';
      case 'commonAreaRatio':
        return this.commonAreaRatio(house) ?? -Infinity;
      case 'discountedTotalPrice':
        return this.discountedTotalPrice(house) ?? -Infinity;
      case 'discountedPricePerPingWithoutParking':
        return this.discountedPricePerPingWithoutParking(house) ?? -Infinity;
      case 'registryPricePerPingMin':
        return house.registryPricePerPingMin ?? -Infinity;
      case 'registryPricePerPingMax':
        return house.registryPricePerPingMax ?? -Infinity;
      case 'latestRegistryPricePerPing':
        return house.latestRegistryPricePerPing ?? -Infinity;
      case 'hasVisited':
        return house.hasVisited ? 1 : 0;
      case 'visitIssue': {
        const issue = this.hasAnyVisitIssue(house);
        return issue === null ? -1 : issue ? 1 : 0;
      }
      case 'unitsPerElevator':
        return this.unitsPerElevator(house) ?? -Infinity;
      default:
        return (house as any)[col] ?? -Infinity;
    }
  }

  /** 戶/梯比 = 每層戶數 / 電梯數 */
  unitsPerElevator(house: House): number | null {
    if (!house.unitsPerFloor || !house.elevatorCount || house.elevatorCount <= 0) return null;
    return +(house.unitsPerFloor / house.elevatorCount).toFixed(2);
  }

  private sortedHouses(list: House[]): House[] {
    if (!this.sortColumn) return list;
    const col = this.sortColumn;
    const dir = this.sortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const va = this.sortValue(a, col);
      const vb = this.sortValue(b, col);
      if (typeof va === 'string' && typeof vb === 'string') {
        return va.localeCompare(vb, 'zh-TW') * dir;
      }
      const na = typeof va === 'number' ? va : -Infinity;
      const nb = typeof vb === 'number' ? vb : -Infinity;
      return (na - nb) * dir;
    });
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

  /** 顯示用：實登每坪範圍字串，例 "60~70"；兩端皆無時回 null */
  registryPricePerPingRange(house: House): string | null {
    const min = house.registryPricePerPingMin;
    const max = house.registryPricePerPingMax;
    if (min == null && max == null) return null;
    if (min != null && max != null) return `${min}~${max}`;
    if (min != null) return `≥ ${min}`;
    return `≤ ${max}`;
  }

  /** 折扣後總價（無折扣時回 null） */
  discountedTotalPrice(house: House): number | null {
    if (!house.discountPercent || house.discountPercent <= 0) return null;
    return +(house.totalPrice * (1 - house.discountPercent / 100)).toFixed(1);
  }

  /**
   * 折扣後不含車位每坪（無折扣或無建坪時回 null）。
   * 邏輯需與 backend HouseService.calculatePricePerPingWithoutParking 一致：
   * (折扣後總價 - 有效車位價) / (總坪 - 車位坪)；
   * 車位價未填時用「車位坪 × 30 萬」估算。
   */
  discountedPricePerPingWithoutParking(house: House): number | null {
    const discounted = this.discountedTotalPrice(house);
    if (discounted === null) return null;
    if (!house.buildAreaPing || house.buildAreaPing <= 0) return null;

    const hasParking = !!house.parkingType && house.parkingType !== 'NONE';
    const parkingPing = (hasParking && house.parkingPing) ? house.parkingPing : 0;
    const parkingPriceFilled = (hasParking && house.parkingPrice) ? house.parkingPrice : 0;
    const effectiveParkingPrice = parkingPriceFilled > 0
      ? parkingPriceFilled
      : parkingPing * 30;

    const netArea = house.buildAreaPing - parkingPing;
    if (netArea <= 0) return null;
    return +((discounted - effectiveParkingPrice) / netArea).toFixed(2);
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
}
