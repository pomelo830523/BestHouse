import { Component, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HouseService } from '../../../core/services/house.service';
import { House } from '../../../core/models/house.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-house-map',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './house-map.component.html',
  styleUrls: ['./house-map.component.scss'],
})
export class HouseMapComponent implements OnInit, OnDestroy {
  @ViewChild('mapContainer', { static: false }) mapContainer!: ElementRef<HTMLDivElement>;

  isLoading = true;
  errorMessage = '';
  geocodedCount = 0;
  totalWithAddress = 0;
  noAddressHouses: House[] = [];
  showEliminated = true;

  private map!: google.maps.Map;
  private geocoder!: google.maps.Geocoder;
  private infoWindow!: google.maps.InfoWindow;
  private markers: google.maps.Marker[] = [];
  private allHouses: House[] = [];

  constructor(private houseService: HouseService) {}

  ngOnInit(): void {
    if (!environment.googleMapsApiKey) {
      this.errorMessage = '尚未設定 Google Maps API Key，請在 environment.ts 填入 googleMapsApiKey。';
      this.isLoading = false;
      return;
    }

    this.houseService.getAll().subscribe({
      next: (data) => {
        this.allHouses = data;
        this.loadMapsApi()
          .then(() => this.initMap())
          .catch((err: Error) => {
            if (err?.message === 'gm_authFailure') {
              this.errorMessage = 'API Key 驗證失敗：請確認已啟用「Maps JavaScript API」與「Geocoding API」，且帳單帳號已設定。';
            } else if (err?.message === 'script_load_failed') {
              this.errorMessage = 'Google Maps 腳本載入失敗，請確認網路連線正常。';
            } else {
              this.errorMessage = `地圖初始化失敗：${err?.message ?? '未知錯誤'}`;
            }
            this.isLoading = false;
          });
      },
      error: () => {
        this.errorMessage = '載入房屋資料失敗';
        this.isLoading = false;
      },
    });
  }

  private loadMapsApi(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (typeof google !== 'undefined' && google.maps) {
        resolve();
        return;
      }

      (window as any)['gm_authFailure'] = () => {
        reject(new Error('gm_authFailure'));
      };

      const callbackName = '__gmapsReady__';
      (window as any)[callbackName] = () => {
        delete (window as any)[callbackName];
        resolve();
      };

      const script = document.createElement('script');
      script.id = 'google-maps-script';
      script.src = `https://maps.googleapis.com/maps/api/js?key=${environment.googleMapsApiKey}&language=zh-TW&callback=${callbackName}`;
      script.async = true;
      script.defer = true;
      script.onerror = () => reject(new Error('script_load_failed'));
      document.head.appendChild(script);
    });
  }

  private initMap(): void {
    this.map = new google.maps.Map(this.mapContainer.nativeElement, {
      center: { lat: 24.8039, lng: 120.9647 },
      zoom: 13,
    });
    this.geocoder = new google.maps.Geocoder();
    this.infoWindow = new google.maps.InfoWindow();
    this.isLoading = false;
    this.geocodeHouses();
  }

  private geocodeHouses(): void {
    const toGeocode = this.getVisibleHouses().filter(h => h.address?.trim());
    this.noAddressHouses = this.allHouses.filter(h => !h.address?.trim());
    this.totalWithAddress = toGeocode.length;
    this.geocodedCount = 0;
    this.clearMarkers();
    this.geocodeNext(toGeocode, 0);
  }

  private geocodeNext(houses: House[], index: number): void {
    if (index >= houses.length) {
      this.fitBounds();
      return;
    }
    const house = houses[index];
    this.geocoder.geocode({ address: house.address, region: 'TW' }, (results, status) => {
      if (status === 'OK' && results && results[0]) {
        this.geocodedCount++;
        this.addMarker(house, results[0].geometry.location);
      }
      setTimeout(() => this.geocodeNext(houses, index + 1), 100);
    });
  }

  private addMarker(house: House, position: google.maps.LatLng): void {
    const isActive = house.status === 'ACTIVE';
    const marker = new google.maps.Marker({
      position,
      map: this.map,
      title: house.nickname,
      icon: {
        path: google.maps.SymbolPath.CIRCLE,
        scale: 9,
        fillColor: isActive ? '#1a237e' : '#9e9e9e',
        fillOpacity: 0.9,
        strokeColor: '#ffffff',
        strokeWeight: 2,
      },
    });

    marker.addListener('click', () => {
      const priceStr = `${house.totalPrice} 萬`;
      const discountedPrice = house.discountPercent
        ? (house.totalPrice * (1 - house.discountPercent / 100)).toFixed(1)
        : null;
      const discountRow = discountedPrice
        ? `<div style="font-size:0.85rem;color:#e65100;">折扣後：${discountedPrice} 萬</div>`
        : '';
      const statusBadge = isActive
        ? `<span style="color:#2e7d32;font-weight:600;">● 有效</span>`
        : `<span style="color:#9e9e9e;font-weight:600;">● 已淘汰</span>`;

      this.infoWindow.setContent(`
        <div style="font-family:-apple-system,sans-serif;min-width:180px;line-height:1.6;">
          <div style="font-weight:700;font-size:1rem;margin-bottom:2px;">${house.nickname}</div>
          <div style="color:#616161;font-size:0.82rem;margin-bottom:6px;">${house.address}</div>
          <div style="font-size:0.9rem;">開價：${priceStr}</div>
          ${discountRow}
          <div style="margin-top:6px;font-size:0.8rem;">${statusBadge}</div>
        </div>
      `);
      this.infoWindow.open(this.map, marker);
    });

    this.markers.push(marker);
  }

  private fitBounds(): void {
    if (this.markers.length === 0) return;
    if (this.markers.length === 1) {
      this.map.setCenter(this.markers[0].getPosition()!);
      this.map.setZoom(15);
      return;
    }
    const bounds = new google.maps.LatLngBounds();
    this.markers.forEach(m => bounds.extend(m.getPosition()!));
    this.map.fitBounds(bounds);
  }

  private clearMarkers(): void {
    this.markers.forEach(m => m.setMap(null));
    this.markers = [];
  }

  toggleEliminated(): void {
    this.showEliminated = !this.showEliminated;
    this.geocodeHouses();
  }

  private getVisibleHouses(): House[] {
    return this.showEliminated
      ? this.allHouses
      : this.allHouses.filter(h => h.status === 'ACTIVE');
  }

  ngOnDestroy(): void {
    this.clearMarkers();
  }
}
