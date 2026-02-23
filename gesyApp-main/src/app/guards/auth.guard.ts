import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Fonction helper pour vérifier si l'utilisateur est transitaire
  const checkIsTransitaire = (): boolean => {
    const roles = authService.getRoles();
    return roles.some((role: string) =>
      role.toUpperCase().includes('TRANSITAIRE')
    );
  };

  // La route /login n'a pas de guard, donc si on arrive ici c'est une autre route
  // Vérifier si l'utilisateur est authentifié
  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], {
      queryParams: { returnUrl: state.url }
    });
    return false;
  }

  // Vérifier si le token est valide (non expiré)
  const token = authService.getToken();
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expirationDate = new Date(payload.exp * 1000);

      if (expirationDate <= new Date()) {
        authService.clearAuthData();
        router.navigate(['/login'], {
          queryParams: { returnUrl: state.url }
        });
        return false;
      }
    } catch (e) {
      authService.clearAuthData();
      router.navigate(['/login'], {
        queryParams: { returnUrl: state.url }
      });
      return false;
    }
  }

  const isTransitaire = checkIsTransitaire();
  const currentUrl = state.url;

  // Transitaire : accès uniquement à /for-transitaire
  if (isTransitaire) {
    if (currentUrl === '/for-transitaire' || currentUrl.startsWith('/for-transitaire')) {
      return true;
    }
    router.navigate(['/for-transitaire']);
    return false;
  }

  // Autres rôles (admin, logisticien, contrôleur, etc.) : bloquer l'accès à for-transitaire
  if (currentUrl === '/for-transitaire' || currentUrl.startsWith('/for-transitaire')) {
    router.navigate(['/home']);
    return false;
  }

  return true;
};

