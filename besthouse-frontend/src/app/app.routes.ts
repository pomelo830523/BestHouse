import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'houses', pathMatch: 'full' },
  {
    path: 'houses',
    loadComponent: () =>
      import('./features/houses/house-list/house-list.component').then(m => m.HouseListComponent),
  },
  {
    path: 'houses/new',
    loadComponent: () =>
      import('./features/houses/house-form/house-form.component').then(m => m.HouseFormComponent),
  },
  {
    path: 'houses/:houseId/edit',
    loadComponent: () =>
      import('./features/houses/house-form/house-form.component').then(m => m.HouseFormComponent),
  },
  {
    path: 'houses/map',
    loadComponent: () =>
      import('./features/houses/house-map/house-map.component').then(m => m.HouseMapComponent),
  },
  {
    path: 'houses/:houseId/rate',
    loadComponent: () =>
      import('./features/houses/house-rate/house-rate.component').then(m => m.HouseRateComponent),
  },
  {
    path: 'ranking',
    loadComponent: () =>
      import('./features/ranking/ranking.component').then(m => m.RankingComponent),
  },
  {
    path: 'filter-rules',
    loadComponent: () =>
      import('./features/filter-rules/filter-rules.component').then(m => m.FilterRulesComponent),
  },
  {
    path: 'house-tips',
    loadComponent: () =>
      import('./features/house-tips/house-tips.component').then(m => m.HouseTipsComponent),
  },
  {
    path: 'ai-import',
    loadComponent: () =>
      import('./features/ai-import/ai-import.component').then(m => m.AiImportComponent),
  },
];
