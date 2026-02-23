import { Component, OnInit, Output, EventEmitter, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AlerteService, Alerte } from '../../services/alerte.service';
import { addIcons } from 'ionicons';
import { logOutOutline } from 'ionicons/icons';
import { IonIcon } from '@ionic/angular/standalone';
import { AlertService } from '../../nativeComp/alert/alert.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  standalone: true,
  imports: [CommonModule, IonIcon]
})
export class HeaderComponent implements OnInit {
  @Output() menuToggle = new EventEmitter<void>();
  currentDate: string = '';
  notificationsCount: number = 0;
  currentUser: any = null;
  alertesPanelOpen = false;
  alertes: Alerte[] = [];
  /** Pagination backend : page courante, taille de page, total. */
  alertesPageSize = 10;
  alertesCurrentPage = 0;
  alertesTotalElements = 0;
  alertesTotalPages = 0;
  alertesLoading = false;
  alertesLoadingMore = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private alertService: AlertService,
    private alerteService: AlerteService
  ) {
    addIcons({logOutOutline});
  }

  ngOnInit() {
    this.updateDate();
    this.currentUser = this.authService.getCurrentUser();
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
    this.loadAlertesNonLuesCount();
  }

  loadAlertesNonLuesCount() {
    this.alerteService.getNonLues().subscribe({
      next: (list) => {
        this.notificationsCount = list?.length ?? 0;
      },
      error: () => {}
    });
  }

  toggleAlertesPanel() {
    this.alertesPanelOpen = !this.alertesPanelOpen;
    if (this.alertesPanelOpen) {
      this.loadAlertes();
    }
  }

  closeAlertesPanel() {
    this.alertesPanelOpen = false;
  }

  loadAlertes() {
    this.alertesLoading = true;
    this.alertesCurrentPage = 0;
    this.alerteService.getPage(0, this.alertesPageSize).subscribe({
      next: (page) => {
        this.alertes = page.content ?? [];
        this.alertesCurrentPage = page.page;
        this.alertesTotalElements = page.totalElements;
        this.alertesTotalPages = page.totalPages;
        this.notificationsCount = this.alertes.filter(a => !a.lu).length;
        this.alertesLoading = false;
      },
      error: () => {
        this.alertesLoading = false;
      }
    });
  }

  /** Il reste des alertes à charger (pagination backend). */
  get hasMoreAlertes(): boolean {
    return this.alertesCurrentPage + 1 < this.alertesTotalPages || this.alertes.length < this.alertesTotalElements;
  }

  /** Charger la page suivante d'alertes depuis le backend. */
  loadMoreAlertes() {
    if (this.alertesLoadingMore || !this.hasMoreAlertes) return;
    this.alertesLoadingMore = true;
    const nextPage = this.alertesCurrentPage + 1;
    this.alerteService.getPage(nextPage, this.alertesPageSize).subscribe({
      next: (page) => {
        this.alertes = [...this.alertes, ...(page.content ?? [])];
        this.alertesCurrentPage = page.page;
        this.alertesTotalElements = page.totalElements;
        this.alertesTotalPages = page.totalPages;
        this.alertesLoadingMore = false;
      },
      error: () => {
        this.alertesLoadingMore = false;
      }
    });
  }

  marquerCommeLue(alerte: Alerte) {
    if (alerte.lu) return;
    this.alerteService.marquerCommeLue(alerte.id, alerte).subscribe({
      next: (updated) => {
        alerte.lu = true;
        this.notificationsCount = Math.max(0, this.notificationsCount - 1);
      }
    });
  }

  formatAlerteDate(dateStr: string): string {
    const d = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'À l\'instant';
    if (diffMins < 60) return `Il y a ${diffMins} min`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `Il y a ${diffHours}h`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays === 1) return 'Hier';
    if (diffDays < 7) return `Il y a ${diffDays} jours`;
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  goToLien(alerte: Alerte) {
    this.marquerCommeLue(alerte);
    if (alerte.lien) {
      this.router.navigateByUrl(alerte.lien);
      this.closeAlertesPanel();
    }
  }

  updateDate() {
    const today = new Date();
    const options: Intl.DateTimeFormatOptions = {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    };
    this.currentDate = today.toLocaleDateString('fr-FR', options);
  }

  toggleMenu() {
    this.menuToggle.emit();
  }

  deconnecter() {
    this.alertService.confirm(
      'Êtes-vous sûr de vouloir vous déconnecter ?',
      'Déconnexion'
    ).subscribe(confirmed => {
      if (!confirmed) return;
      this.authService.logout().subscribe({
        next: () => {
          this.router.navigate(['/login']);
        },
        error: (error) => {
          console.error('Erreur lors de la déconnexion:', error);
          // Déconnexion locale même en cas d'erreur
          this.authService.clearAuthData();
          this.router.navigate(['/login']);
        }
      });
    });
  }

  getUserInitiales(): string {
    if (this.currentUser?.identifiant) {
      const parts = this.currentUser.identifiant.split('.');
      if (parts.length > 1) {
        return parts.slice(1).map((p: string) => p[0]?.toUpperCase() || '').join('').substring(0, 2);
      }
      return this.currentUser.identifiant.substring(0, 2).toUpperCase();
    }
    return 'U';
  }

  getUserName(): string {
    // Si on a le nom complet, l'utiliser, sinon utiliser l'identifiant
    return this.currentUser?.nom || this.currentUser?.identifiant || 'Utilisateur';
  }

  getUserRole(): string {
    if (this.currentUser?.roles && this.currentUser.roles.length > 0) {
      const role = this.currentUser.roles[0];
      // Retirer le préfixe "ROLE_" si présent
      return role.replace('ROLE_', '').replace('_', ' ');
    }
    return 'Utilisateur';
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    this.closeAlertesPanel();
  }
}
