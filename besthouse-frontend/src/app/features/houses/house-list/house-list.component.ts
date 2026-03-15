import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HouseService } from '../../../core/services/house.service';
import { House, PARKING_TYPE_LABELS } from '../../../core/models/house.model';

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
  errorMessage = '';
  successMessage = '';
  parkingLabels = PARKING_TYPE_LABELS;

  sortColumn: SortableColumn | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(private houseService: HouseService) {}

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

  registryDiffPct(house: House): number | null {
    if (!house.estimatedRegistryPrice || house.estimatedRegistryPrice <= 0) return null;
    const discounted = house.discountPercent && house.discountPercent > 0
      ? house.totalPrice * (1 - house.discountPercent / 100)
      : house.totalPrice;
    if (discounted <= 0) return null;
    return +((discounted - house.estimatedRegistryPrice) / house.estimatedRegistryPrice * 100).toFixed(1);
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
