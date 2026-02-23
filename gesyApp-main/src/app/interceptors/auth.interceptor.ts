import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Liste des URLs publiques qui ne nécessitent pas de token
  const publicUrls = [
    '/auth/login',
    '/auth/logout',
  ];

  // Vérifier si l'URL est publique
  const isPublicUrl = publicUrls.some(url => req.url.includes(url));

  if (isPublicUrl) {
    return next(req);
  }

  // Ajouter le token JWT aux requêtes protégées
  const token = authService.getToken();
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // Gérer les erreurs d'authentification
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        console.warn('Token invalide ou expiré. Déconnexion...');
        // Nettoyer les données d'authentification sans appeler l'API logout (le token est déjà invalide)
        authService.clearAuthData();
        router.navigate(['/login']);
      } else if (error.status === 403) {
        console.error('Accès refusé - Droits insuffisants');
        // Pour 403, on peut aussi déconnecter selon votre logique métier
        // authService.clearAuthData();
        // router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};
