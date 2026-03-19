import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HouseService } from '../../../core/services/house.service';
import { RealPriceService } from '../../../core/services/real-price.service';
import { House } from '../../../core/models/house.model';

type SortableColumn =
  | 'buildAreaPing' | 'indoorPing' | 'totalPrice'
  | 'pricePerPingWithoutParking'
  | 'houseAgeYear' | 'floor';

@Component({
  selector: 'app-house-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
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

  constructor(
    private houseService: HouseService,
    private realPriceService: RealPriceService,
  ) {}

  ngOnInit(): void {
    this.loadHouses();
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

  private sortedHouses(list: House[]): House[] {
    if (!this.sortColumn) return list;
    const col = this.sortColumn;
    const dir = this.sortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const aVal = (a as any)[col] ?? -Infinity;
      const bVal = (b as any)[col] ?? -Infinity;
      return (aVal - bVal) * dir;
    });
  }

  get activeHouses(): House[] {
    return this.sortedHouses(this.houses.filter(h => h.status === 'ACTIVE'));
  }

  get eliminatedHouses(): House[] {
    return this.sortedHouses(this.houses.filter(h => h.status === 'ELIMINATED'));
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

  /** @deprecated 相容舊版，改用 originalRegistryDiffPct / discountedRegistryDiffPct */
  registryDiffPct(house: House): number | null {
    return this.originalRegistryDiffPct(house);
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
