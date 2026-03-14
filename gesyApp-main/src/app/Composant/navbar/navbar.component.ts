import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationService, MenuSection } from '../../services/navigation.service';
import { AuthService } from '../../services/auth.service';

interface MenuItem {
  id: MenuSection;
  label: string;
  icon: string;
  badge?: number;
}

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class NavbarComponent implements OnInit {
  currentSection: MenuSection | null = null;
  isOpen: boolean = true;
  isMobile: boolean = false;
  currentUser: any = null;

  allMenuPrincipal: MenuItem[] = [
    { id: 'dashbord' as MenuSection, label: 'Tableau de Bord', icon: '📊' },
    { id: 'camion' as MenuSection, label: 'Gestion Camions', icon: '🚛'},
    { id: 'achats' as MenuSection, label: 'Achats de produits', icon: '🛒'},
    { id: 'facturation' as MenuSection, label: 'Facturation', icon: '📄' },
    { id: 'paiement' as MenuSection, label: 'Paiements', icon: '💳' },
    { id: 'depenses' as MenuSection, label: 'Dépenses', icon: '💸' },
    { id: 'banque-caisse' as MenuSection, label: 'Banque & Caisse', icon: '🏦' },
    { id: 'stock' as MenuSection, label: 'Gestion Stocks', icon: '📦' },
    { id: 'client-fournisseur' as MenuSection, label: 'Clients & Fournisseurs', icon: '👥' }
  ];

  allLogistique: MenuItem[] = [
    { id: 'suivi-transport' as MenuSection, label: 'Suivis Transport', icon: '🚚' },
    { id: 'transitaire' as MenuSection, label: 'Transitaire', icon: '📋' },
    { id: 'axes' as MenuSection, label: 'Axes', icon: '🛣️' },
    { id: 'depot' as MenuSection, label: 'Depot', icon: '🏭' },
    { id: 'cout' as MenuSection, label: 'Coût de transport', icon: '💰' },
    { id: 'email' as MenuSection, label: 'Messagerie', icon: '📧' }
  ];

  allRapport: MenuItem[] = [
    { id: 'rapport' as MenuSection, label: 'Rapport financier', icon: '📈' },
    { id: 'analyse' as MenuSection, label: 'Analyse & stat', icon: '📊' },
    { id: 'capital' as MenuSection, label: 'Capital', icon: '💎' },
    { id: 'settings' as MenuSection, label: 'Paramètres', icon: '⚙️' }
  ];

  menuPrincipal: MenuItem[] = [];
  logistique: MenuItem[] = [];
  rapport: MenuItem[] = [];

  constructor(
    private navigationService: NavigationService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.checkScreenSize();
    this.loadUserInfo();
    this.filterMenusByRole();
    this.setDefaultSection();

    this.navigationService.currentSection$.subscribe(section => {
      this.currentSection = section;
      // Fermer le menu sur mobile après sélection
      if (this.isMobile) {
        this.isOpen = false;
      }
    });

    // S'abonner aux changements de l'utilisateur
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.filterMenusByRole();
      this.setDefaultSection();
    });
  }

  loadUserInfo() {
    this.currentUser = this.authService.getCurrentUser();
  }

  filterMenusByRole() {
    const isAdmin = this.authService.isAdmin();
    const isComptable = this.authService.isComptable();
    const isResponsableLogistique = this.authService.isResponsableLogistique();
    const isLogisticien = this.authService.isLogisticien();
    const isControleur = this.authService.isControleur();
    const hasAccessToClients = this.authService.hasAccessToClients();

    if (isAdmin) {
      // Admin : tous les menus
      this.menuPrincipal = [...this.allMenuPrincipal];
      this.logistique = [...this.allLogistique];
      this.rapport = [...this.allRapport];
    } else if (isComptable) {
      // Comptable : paiements, facturation, banque-caisse, dépenses, achats, stock, clients, coûts transport, suivi (voyages attribués sans prix uniquement)
      this.menuPrincipal = this.allMenuPrincipal.filter(item =>
        ['paiement', 'facturation', 'banque-caisse', 'client-fournisseur', 'achats', 'stock', 'depenses'].includes(item.id)
      );
      this.logistique = this.allLogistique.filter(item =>
        ['suivi-transport', 'depot', 'cout'].includes(item.id)
      );
      this.rapport = [];
    } else if (isResponsableLogistique) {
      // Responsable logistique : tableau de bord, camions, stock, suivi-transport, transitaire, axes, depot, email
      this.menuPrincipal = this.allMenuPrincipal.filter(item =>
        ['dashbord', 'camion', 'stock'].includes(item.id)
      );
      this.logistique = this.allLogistique.filter(item =>
        ['suivi-transport', 'transitaire', 'axes', 'depot', 'email'].includes(item.id)
      );
      this.rapport = [];
    } else if (isLogisticien) {
      // Logisticien (simple) : tableau de bord, stock, suivi-transport, depot, email
      this.menuPrincipal = this.allMenuPrincipal.filter(item =>
        ['dashbord', 'stock'].includes(item.id)
      );
      this.logistique = this.allLogistique.filter(item =>
        ['suivi-transport', 'depot', 'email'].includes(item.id)
      );
      this.rapport = [];
    } else if (isControleur) {
      // Contrôleur : attribution, gestion manquants (suivi-transport, depot, cout, axes) ; voit clients & fournisseurs (lecture) ; pas de modification des prix
      this.menuPrincipal = this.allMenuPrincipal.filter(item =>
        ['client-fournisseur', 'stock'].includes(item.id)
      );
      this.logistique = this.allLogistique.filter(item =>
        ['suivi-transport', 'depot', 'cout', 'axes'].includes(item.id)
      );
      this.rapport = [];
    } else {
      // Transitaire, Charger Depot ou autre : menus minimaux ou vides
      this.menuPrincipal = [];
      this.logistique = [];
      this.rapport = [];
    }

    // Retirer Clients & Fournisseurs si pas d'accès
    if (!hasAccessToClients) {
      this.menuPrincipal = this.menuPrincipal.filter(item => item.id !== 'client-fournisseur');
    }

    // Retirer Axes pour tout le monde sauf l'admin
    if (!isAdmin) {
      this.logistique = this.logistique.filter(item => item.id !== 'axes');
    }
  }

  setDefaultSection() {
    // Trouver la première section disponible dans l'ordre : menuPrincipal, logistique, rapport
    let firstAvailableSection: MenuSection | null = null;

    if (this.menuPrincipal.length > 0) {
      firstAvailableSection = this.menuPrincipal[0].id;
    } else if (this.logistique.length > 0) {
      firstAvailableSection = this.logistique[0].id;
    } else if (this.rapport.length > 0) {
      firstAvailableSection = this.rapport[0].id;
    }

    // Si une section est trouvée et que la section actuelle n'est pas valide, définir la nouvelle section
    if (firstAvailableSection) {
      const allAvailableSections = [
        ...this.menuPrincipal.map(m => m.id),
        ...this.logistique.map(m => m.id),
        ...this.rapport.map(m => m.id)
      ];

      // Si la section actuelle n'est pas dans les sections disponibles, changer
      if (!this.currentSection || !allAvailableSections.includes(this.currentSection)) {
        this.currentSection = firstAvailableSection;
        this.navigationService.setCurrentSection(firstAvailableSection);
      }
    }
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

  @HostListener('window:resize')
  onResize() {
    this.checkScreenSize();
  }

  checkScreenSize() {
    this.isMobile = window.innerWidth < 768;
    if (!this.isMobile) {
      this.isOpen = true;
    } else {
      this.isOpen = false;
    }
  }

  toggleMenu() {
    this.isOpen = !this.isOpen;
  }

  closeMenu() {
    if (this.isMobile) {
      this.isOpen = false;
    }
  }

  selectSection(section: MenuSection) {
    this.navigationService.setCurrentSection(section);
  }
}
