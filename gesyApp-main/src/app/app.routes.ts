import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'home',
    loadComponent: () => import('./home/home.page').then((m) => m.HomePage),
    canActivate: [authGuard]
  },
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full',
  },
  {
    path: 'for-transitaire',
    loadComponent: () => import('./for-transitaire/for-transitaire.page').then( m => m.ForTransitairePage),
    canActivate: [authGuard]
  },
  {
    path: 'transitaire-detail/:id',
    loadComponent: () => import('./transitaire-detail/transitaire-detail.page').then(m => m.TransitaireDetailPage),
    canActivate: [authGuard]
  },
  {
    path: 'login',
    loadComponent: () => import('./login/login.page').then( m => m.LoginPage)
  },
  {
    path: 'construc',
    loadComponent: () => import('./construc/construc.page').then( m => m.ConstrucPage)
  },
];
