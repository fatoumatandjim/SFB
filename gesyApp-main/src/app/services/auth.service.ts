import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  identifiant: string;
  motDePasse: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  identifiant: string;
  roles: string[];
}

/** Noms de rôles côté backend (ROLE_ + nom en majuscules, espaces possibles) */
export const ROLES = {
  ADMIN: 'ROLE_ADMIN',
  RESPONSABLE_LOGISTIQUE: 'ROLE_RESPONSABLE LOGISTIQUE',
  LOGISTICIEN: 'ROLE_LOGISTICIEN',
  SIMPLE_LOGISTICIEN: 'ROLE_SIMPLE LOGISTICIEN',
  CONTROLEUR: 'ROLE_CONTRÔLEUR',
  COMPTABLE: 'ROLE_COMPTABLE',
  TRANSITAIRE: 'ROLE_TRANSITAIRE',
  CHARGER_DEPOT: 'ROLE_CHARGER DEPOT',
} as const;

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private tokenKey = 'auth_token';
  private identifiantKey = 'auth_identifiant';
  private rolesKey = 'auth_roles';
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    // Charger le token et les données utilisateur depuis le localStorage au démarrage
    const token = this.getToken();
    if (token) {
      this.setCurrentUserFromToken(token);
    }
  }

  login(identifiant: string, motDePasse: string): Observable<LoginResponse> {
    const loginRequest: LoginRequest = { identifiant, motDePasse };

    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, loginRequest).pipe(
      tap(response => {
        // Stocker le token
        localStorage.setItem(this.tokenKey, response.token);
        // Stocker l'identifiant
        localStorage.setItem(this.identifiantKey, response.identifiant);
        // Stocker les rôles
        localStorage.setItem(this.rolesKey, JSON.stringify(response.roles || []));
        // Définir l'utilisateur actuel
        this.setCurrentUserFromStorage();
      })
    );
  }

  logout(): Observable<void> {
    const token = this.getToken();
    if (token) {
      // Appeler l'endpoint de déconnexion
      return this.http.post<void>(`${this.apiUrl}/logout`, {}, {
        headers: this.getAuthHeaders()
      }).pipe(
        tap(() => {
          this.clearAuth();
        })
      );
    }
    this.clearAuth();
    return new Observable(observer => {
      observer.next();
      observer.complete();
    });
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    // Vérifier si le token est expiré (décodage basique)
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expirationDate = new Date(payload.exp * 1000);
      return expirationDate > new Date();
    } catch (e) {
      return false;
    }
  }

  getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    if (token) {
      return new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });
    }
    return new HttpHeaders();
  }

  getCurrentUser(): any {
    return this.currentUserSubject.value;
  }

  /** Vérifie si l'utilisateur a un rôle (exact ou normalisé ROLE_XXX / ROLE_XXX YYY) */
  hasRole(role: string): boolean {
    const roles = this.getRoles();
    if (roles.includes(role)) return true;
    const normalized = role.toUpperCase().replace(/\s+/g, ' ');
    return roles.some(r => r.toUpperCase().replace(/\s+/g, ' ') === normalized);
  }

  isAdmin(): boolean {
    if (this.hasRole(ROLES.ADMIN)) return true;
    // Fallback : accepter tout rôle contenant "ADMIN" (casse insensible) pour compatibilité backend
    const roles = this.getRoles();
    return roles.some(r => (r || '').toUpperCase().includes('ADMIN'));
  }

  /** Responsable logistique unique : crée voyages, attribue, charge camions ; pas d'accès clients */
  isResponsableLogistique(): boolean {
    return this.hasRole(ROLES.RESPONSABLE_LOGISTIQUE);
  }

  /** Autres logisticiens : camions attribués, suivi jusqu'à réception ; pas d'accès clients */
  isLogisticien(): boolean {
    return this.hasRole(ROLES.LOGISTICIEN) || this.hasRole(ROLES.SIMPLE_LOGISTICIEN);
  }

  /** Contrôleur : attribution, manquants ; prix définis par le comptable */
  isControleur(): boolean {
    return this.hasRole(ROLES.CONTROLEUR);
  }

  /** Comptable : gestion des paiements, validation par l'admin */
  isComptable(): boolean {
    return this.hasRole(ROLES.COMPTABLE);
  }

  isTransitaire(): boolean {
    return this.hasRole(ROLES.TRANSITAIRE);
  }

  isChargerDepot(): boolean {
    return this.hasRole(ROLES.CHARGER_DEPOT);
  }

  /** Accès à la section Clients & Fournisseurs (refusé pour Responsable logistique et Logisticien) */
  hasAccessToClients(): boolean {
    if (this.isAdmin()) return true;
    if (this.isResponsableLogistique() || this.isLogisticien()) return false;
    return this.isComptable() || this.isControleur();
  }

  getIdentifiant(): string | null {
    return localStorage.getItem(this.identifiantKey);
  }

  getRoles(): string[] {
    const rolesStr = localStorage.getItem(this.rolesKey);
    if (rolesStr) {
      try {
        return JSON.parse(rolesStr);
      } catch (e) {
        console.error('Erreur lors du parsing des rôles:', e);
        return [];
      }
    }
    return [];
  }

  private setCurrentUserFromToken(token: string) {
    // Cette méthode est appelée au démarrage si un token existe
    // On récupère les données depuis le localStorage
    this.setCurrentUserFromStorage();
  }

  private setCurrentUserFromStorage() {
    const identifiant = this.getIdentifiant();
    const roles = this.getRoles();

    if (identifiant) {
      this.currentUserSubject.next({
        identifiant: identifiant,
        roles: roles
      });
    } else {
      // Si pas d'identifiant dans le localStorage, essayer de le récupérer du token
      const token = this.getToken();
      if (token) {
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          const tokenIdentifiant = payload.sub;
          const tokenRoles = payload.roles || [];

          // Sauvegarder dans le localStorage pour les prochaines fois
          if (tokenIdentifiant) {
            localStorage.setItem(this.identifiantKey, tokenIdentifiant);
          }
          if (tokenRoles && tokenRoles.length > 0) {
            localStorage.setItem(this.rolesKey, JSON.stringify(tokenRoles));
          }

          this.currentUserSubject.next({
            identifiant: tokenIdentifiant,
            roles: tokenRoles
          });
        } catch (e) {
          console.error('Erreur lors du décodage du token:', e);
          this.currentUserSubject.next(null);
        }
      } else {
        this.currentUserSubject.next(null);
      }
    }
  }

  private clearAuth() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.identifiantKey);
    localStorage.removeItem(this.rolesKey);
    this.currentUserSubject.next(null);
  }

  clearAuthData() {
    this.clearAuth();
  }
}

