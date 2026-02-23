import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { AchatsService, Achat, AchatPage, AchatMarge, CreateAchatWithFactureDTO, CreateAchatCessionDTO, PayerAchatDTO } from '../../services/achats.service';
import { DepotsService, Depot } from '../../services/depots.service';
import { ProduitsService, Produit } from '../../services/produits.service';
import { FournisseursService, Fournisseur } from '../../services/fournisseurs.service';
import { ComptesBancairesService, CompteBancaire } from '../../services/comptes-bancaires.service';
import { ClientsService, Client } from '../../services/clients.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-gestion-achat',
  templateUrl: './gestion-achat.component.html',
  styleUrls: ['./gestion-achat.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class GestionAchatComponent implements OnInit {
  isLoading: boolean = false;
  achats: Achat[] = [];
  searchTerm: string = '';
  showDetailModal: boolean = false;
  selectedAchat: Achat | null = null;
  achatMarge: AchatMarge | null = null;
  isLoadingMarge: boolean = false;

  // Onglets
  activeTab: 'tous' | 'payes' | 'impayes' | 'cession' = 'tous';

  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  // Filtres de date
  filterDate: string = '';
  filterStartDate: string = '';
  filterEndDate: string = '';
  useDateRange: boolean = false;

  // Modal nouveau achat
  showNewAchatModal: boolean = false;
  depots: Depot[] = [];
  produits: Produit[] = [];
  fournisseurs: Fournisseur[] = [];
  newAchat: Partial<CreateAchatWithFactureDTO> = {
    depotId: undefined,
    produitId: undefined,
    fournisseurId: undefined,
    quantite: 0,
    prixUnitaire: 0,
    description: '',
    notes: '',
    unite: ''
  };

  // Modal paiement
  showPaiementModal: boolean = false;
  achatToPay: Achat | null = null;
  comptesBancaires: CompteBancaire[] = [];
  selectedCompte: CompteBancaire | null = null;
  paiementData: Partial<PayerAchatDTO> = {
    achatId: undefined,
    compteBancaireId: undefined
  };
  isAdmin!: boolean;

  // Modal achat de cession
  showNewCessionModal: boolean = false;
  clients: Client[] = [];
  newCession: Partial<CreateAchatCessionDTO> = {
    clientId: undefined,
    depotId: undefined,
    produitId: undefined,
    quantite: 0,
    description: '',
    notes: '',
    unite: 'L'
  };

  constructor(
    private achatsService: AchatsService,
    private depotsService: DepotsService,
    private produitsService: ProduitsService,
    private fournisseursService: FournisseursService,
    private comptesBancairesService: ComptesBancairesService,
    private clientsService: ClientsService,
    private toastService: ToastService,
    private alertService: AlertService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.loadAchats();
    this.loadDepots();
    this.loadProduits();
    this.loadFournisseurs();
    this.loadComptesBancaires();
    this.loadClients();
    this.isAdmin = this.authService.hasRole("ROLE_ADMIN")
  }

  loadClients() {
    this.clientsService.getAllClients().subscribe({
      next: (data: Client[]) => {
        this.clients = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des clients:', error);
      }
    });
  }

  loadDepots() {
    this.depotsService.getAllDepots().subscribe({
      next: (data) => {
        this.depots = data.filter(d => d.statut === 'ACTIF');
      },
      error: (error) => {
        console.error('Erreur lors du chargement des dépôts:', error);
      }
    });
  }

  loadProduits() {
    this.produitsService.getAllProduits().subscribe({
      next: (data) => {
        this.produits = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des produits:', error);
      }
    });
  }

  loadFournisseurs() {
    this.fournisseursService.getFournisseursByType('ACHAT').subscribe({
      next: (data) => {
        this.fournisseurs = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des fournisseurs:', error);
      }
    });
  }

  loadComptesBancaires() {
    this.comptesBancairesService.getAllComptes().subscribe({
      next: (data: CompteBancaire[]) => {
        this.comptesBancaires = data.filter((c: CompteBancaire) => c.statut === 'ACTIF');
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des comptes bancaires:', error);
      }
    });
  }

  loadAchats() {
    this.isLoading = true;
    let request: Observable<AchatPage>;

    if (this.activeTab === 'cession') {
      if (this.useDateRange && this.filterStartDate && this.filterEndDate) {
        request = this.achatsService.getAchatsCessionByDateRange(
          this.filterStartDate,
          this.filterEndDate,
          this.currentPage,
          this.pageSize
        );
      } else if (this.filterDate) {
        request = this.achatsService.getAchatsCessionByDate(
          this.filterDate,
          this.currentPage,
          this.pageSize
        );
      } else {
        request = this.achatsService.getAchatsCessionPaginated(
          this.currentPage,
          this.pageSize
        );
      }
    } else {
      // Déterminer le statut selon l'onglet actif
      let statut: string | null = null;
      if (this.activeTab === 'payes') {
        statut = 'PAYEE';
      } else if (this.activeTab === 'impayes') {
        statut = 'EMISE';
      }

    if (statut) {
      // Filtrer par statut de facture
      if (this.useDateRange && this.filterStartDate && this.filterEndDate) {
        request = this.achatsService.getAchatsByStatutFactureAndDateRange(
          statut,
          this.filterStartDate,
          this.filterEndDate,
          this.currentPage,
          this.pageSize
        );
      } else if (this.filterDate) {
        request = this.achatsService.getAchatsByStatutFactureAndDate(
          statut,
          this.filterDate,
          this.currentPage,
          this.pageSize
        );
      } else {
        request = this.achatsService.getAchatsByStatutFacture(
          statut,
          this.currentPage,
          this.pageSize
        );
      }
    } else {
      // Onglet "Tous" - pas de filtre de statut
      if (this.useDateRange && this.filterStartDate && this.filterEndDate) {
        request = this.achatsService.getAchatsByDateRange(
          this.filterStartDate,
          this.filterEndDate,
          this.currentPage,
          this.pageSize
        );
      } else if (this.filterDate) {
        request = this.achatsService.getAchatsByDate(
          this.filterDate,
          this.currentPage,
          this.pageSize
        );
      } else {
        request = this.achatsService.getAchatsPaginated(
          this.currentPage,
          this.pageSize
        );
      }
    }
    }

    request.subscribe({
      next: (page: AchatPage) => {
        this.achats = page.achats;

        // Appliquer le filtre de recherche si présent
        if (this.searchTerm.trim()) {
          const term = this.searchTerm.toLowerCase();
          this.achats = this.achats.filter(a =>
            a.depotNom?.toLowerCase().includes(term) ||
            a.produitNom?.toLowerCase().includes(term) ||
            a.typeProduit?.toLowerCase().includes(term) ||
            a.description?.toLowerCase().includes(term) ||
            a.clientNom?.toLowerCase().includes(term)
          );
        }

        this.currentPage = page.currentPage;
        this.totalPages = page.totalPages;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des achats:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors du chargement des achats');
      }
    });
  }

  onSearchChange() {
    this.currentPage = 0;
    this.loadAchats();
  }

  onDateFilterChange() {
    this.currentPage = 0;
    this.loadAchats();
  }

  clearDateFilters() {
    this.filterDate = '';
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.useDateRange = false;
    this.currentPage = 0;
    this.loadAchats();
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadAchats();
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadAchats();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadAchats();
    }
  }

  getTypeProduitIcon(typeProduit: string | undefined): string {
    if (!typeProduit) return '📦';
    switch (typeProduit) {
      case 'ESSENCE':
      case 'GAZOLE':
        return '⛽';
      case 'GPL':
        return '🔥';
      case 'KEROSENE':
        return '🛢️';
      default:
        return '📦';
    }
  }

  getTypeProduitColor(typeProduit: string | undefined): string {
    if (!typeProduit) return 'blue';
    switch (typeProduit) {
      case 'ESSENCE':
        return 'blue';
      case 'GAZOLE':
        return 'orange';
      case 'GPL':
        return 'red';
      case 'KEROSENE':
        return 'green';
      default:
        return 'blue';
    }
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return dateString;
      }
      return date.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateString;
    }
  }

  getDepotInitiales(depotNom: string | undefined): string {
    if (!depotNom) return '??';
    return depotNom.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }

  getDepotColor(depotNom: string | undefined): string {
    if (!depotNom) return 'gray';
    const colors = ['blue', 'purple', 'red', 'green', 'orange', 'teal', 'pink'];
    const hash = depotNom.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return colors[hash % colors.length];
  }

  formatMontant(montant: number | undefined | null): string {
    if (!montant && montant !== 0) return '0 FCFA';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'XOF',
      minimumFractionDigits: 0
    }).format(montant);
  }

  viewAchatDetails(achat: Achat) {
    if (achat.id) {
      this.achatsService.getAchatById(achat.id).subscribe({
        next: (fullAchat) => {
          this.selectedAchat = fullAchat;
          this.showDetailModal = true;
          if (!fullAchat.cession) {
            this.loadMarge(achat.id!);
          } else {
            this.achatMarge = null;
            this.isLoadingMarge = false;
          }
        },
        error: (error) => {
          console.error('Erreur lors du chargement des détails:', error);
          this.selectedAchat = achat;
          this.showDetailModal = true;
          if (achat.id && !achat.cession) {
            this.loadMarge(achat.id);
          } else {
            this.achatMarge = null;
            this.isLoadingMarge = false;
          }
        }
      });
    } else {
      this.selectedAchat = achat;
      this.showDetailModal = true;
    }
  }

  loadMarge(achatId: number) {
    this.isLoadingMarge = true;
    this.achatsService.getMargeAchat(achatId).subscribe({
      next: (marge) => {
        this.achatMarge = marge;
        this.isLoadingMarge = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la marge:', error);
        this.isLoadingMarge = false;
        this.toastService.error('Erreur lors du calcul de la marge');
      }
    });
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedAchat = null;
    this.achatMarge = null;
  }

  setTab(tab: 'tous' | 'payes' | 'impayes' | 'cession') {
    this.activeTab = tab;
    this.currentPage = 0;
    this.loadAchats();
  }

  openNewCessionModal() {
    this.newCession = {
      clientId: undefined,
      depotId: undefined,
      produitId: undefined,
      quantite: 0,
      description: '',
      notes: '',
      unite: 'L'
    };
    this.showNewCessionModal = true;
  }

  closeNewCessionModal() {
    this.showNewCessionModal = false;
  }

  saveNewCession() {
    if (!this.newCession.clientId || !this.newCession.depotId || !this.newCession.produitId || !this.newCession.quantite || this.newCession.quantite <= 0) {
      this.toastService.error('Veuillez remplir le client, le dépôt, le produit et une quantité strictement positive');
      return;
    }
    this.achatsService.createAchatCession({
      clientId: this.newCession.clientId!,
      depotId: this.newCession.depotId!,
      produitId: this.newCession.produitId!,
      quantite: this.newCession.quantite!,
      description: this.newCession.description || undefined,
      notes: this.newCession.notes || undefined,
      unite: this.newCession.unite || 'L'
    }).subscribe({
      next: () => {
        this.toastService.success('Achat de cession enregistré. Le stock en cession a été mis à jour.');
        this.closeNewCessionModal();
        this.loadAchats();
      },
      error: (err) => {
        this.toastService.error(err?.error?.message || 'Erreur lors de l\'enregistrement de l\'achat de cession');
      }
    });
  }

  nouveauAchat() {
    this.newAchat = {
      depotId: undefined,
      produitId: undefined,
      fournisseurId: undefined,
      quantite: 0,
      prixUnitaire: 0,
      description: '',
      notes: '',
      unite: ''
    };
    this.showNewAchatModal = true;
  }

  closeNewAchatModal() {
    this.showNewAchatModal = false;
    this.newAchat = {
      depotId: undefined,
      produitId: undefined,
      fournisseurId: undefined,
      quantite: 0,
      prixUnitaire: 0,
      description: '',
      notes: '',
      unite: ''
    };
  }

  getProduitUnite(produitId: number | undefined): string {
    // Produit n'a pas de propriété unite, on utilise 'L' par défaut
    return 'L';
  }

  onProduitChange() {
    // Produit n'a pas de propriété unite, on utilise 'L' par défaut
    this.newAchat.unite = 'L';
  }

  saveNewAchat() {
    if (!this.validateNewAchat()) {
      return;
    }

    this.isLoading = true;
    const dto: CreateAchatWithFactureDTO = {
      depotId: this.newAchat.depotId!,
      produitId: this.newAchat.produitId!,
      fournisseurId: this.newAchat.fournisseurId!,
      quantite: this.newAchat.quantite!,
      prixUnitaire: this.newAchat.prixUnitaire!,
      description: this.newAchat.description,
      notes: this.newAchat.notes,
      unite: this.newAchat.unite
    };

    this.achatsService.createAchatWithFacture(dto).subscribe({
      next: (achat) => {
        this.isLoading = false;
        this.toastService.success('Achat créé avec succès! Un paiement en attente a été enregistré.');
        this.closeNewAchatModal();
        this.loadAchats();
      },
      error: (error) => {
        console.error('Erreur lors de la création de l\'achat:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors de la création de l\'achat');
      }
    });
  }

  validateNewAchat(): boolean {
    if (!this.newAchat.depotId) {
      this.toastService.warning('Veuillez sélectionner un dépôt');
      return false;
    }
    if (!this.newAchat.produitId) {
      this.toastService.warning('Veuillez sélectionner un produit');
      return false;
    }
    if (!this.newAchat.fournisseurId) {
      this.toastService.warning('Veuillez sélectionner un fournisseur');
      return false;
    }
    if (!this.newAchat.quantite || this.newAchat.quantite <= 0) {
      this.toastService.warning('Veuillez saisir une quantité valide (supérieure à 0)');
      return false;
    }
    if (!this.newAchat.prixUnitaire || this.newAchat.prixUnitaire <= 0) {
      this.toastService.warning('Veuillez saisir un prix unitaire valide (supérieur à 0)');
      return false;
    }
    return true;
  }

  payerAchat(achat: Achat) {
    if (!achat.id) {
      this.toastService.error('ID d\'achat invalide');
      return;
    }

    // Vérifier le statut de paiement (priorité sur statutFacture)
    const statut = achat.statutPaiement || achat.statutFacture;
    if (statut === 'VALIDE' || statut === 'PAYEE') {
      this.toastService.warning('Cet achat est déjà payé');
      return;
    }

    this.achatToPay = achat;
    this.selectedCompte = null;
    this.paiementData = {
      achatId: achat.id,
      compteBancaireId: undefined
    };
    this.showPaiementModal = true;
  }

  closePaiementModal() {
    this.showPaiementModal = false;
    this.achatToPay = null;
    this.selectedCompte = null;
    this.paiementData = {
      achatId: undefined,
      compteBancaireId: undefined
    };
  }

  getMontantAPayer(): number {
    if (!this.achatToPay || !this.achatToPay.quantite || !this.achatToPay.prixUnitaire) {
      return 0;
    }
    return this.achatToPay.quantite * this.achatToPay.prixUnitaire;
  }

  onCompteChange() {
    if (!this.paiementData.compteBancaireId) {
      this.selectedCompte = null;
      return;
    }

    // Convertir en number pour la comparaison (le select peut retourner une string)
    const compteId = typeof this.paiementData.compteBancaireId === 'string'
      ? Number(this.paiementData.compteBancaireId)
      : Number(this.paiementData.compteBancaireId);

    // Chercher le compte en convertissant tous les IDs en nombres pour la comparaison
    this.selectedCompte = this.comptesBancaires.find(c => {
      if (!c.id) return false;
      return Number(c.id) === compteId;
    }) || null;
  }

  isSoldeInsuffisant(): boolean {
    if (!this.selectedCompte) return false;
    const solde = Number(this.selectedCompte.solde);
    const montant = this.getMontantAPayer();
    return solde < montant;
  }

  confirmPaiement() {
    if (!this.paiementData.achatId || !this.paiementData.compteBancaireId) {
      this.toastService.warning('Veuillez sélectionner un compte bancaire');
      return;
    }
    if (this.selectedCompte === null) {
      this.toastService.error('Compte bancaire non trouvé');
      return;
    }

    const montant = this.getMontantAPayer();
    const solde = Number(this.selectedCompte.solde);

    if (solde < montant) {
      this.toastService.error(`Solde insuffisant. Solde disponible: ${this.formatMontant(solde)}, Montant requis: ${this.formatMontant(montant)}`);
      return;
    }

    const confirmMessage = `Vous êtes sur le point de payer ${this.formatMontant(montant)} pour cet achat.\n\nCompte: ${this.selectedCompte.banque || this.selectedCompte.numero}\n\nCette action va:\n- Valider la facture\n- Débiter le compte bancaire\n- Créer une transaction\n- Approvisionner le dépôt\n\nConfirmer le paiement?`;

    this.alertService.confirm(confirmMessage, 'Confirmation de paiement').subscribe(confirmed => {
      if (!confirmed) return;

      this.isLoading = true;
      const dto: PayerAchatDTO = {
        achatId: this.paiementData.achatId!,
        compteBancaireId: this.selectedCompte!.id!
      };

      this.achatsService.payerAchat(dto).subscribe({
        next: (achat) => {
          this.isLoading = false;
          this.toastService.success('Achat payé avec succès! Le dépôt a été approvisionné.');
          this.closePaiementModal();
          this.loadAchats();
        },
        error: (error) => {
          console.error('Erreur lors du paiement:', error);
          this.isLoading = false;
          const errorMessage = error?.error?.message || 'Erreur lors du paiement de l\'achat';
          this.toastService.error(errorMessage);
        }
      });
    });
  }

  getStatutFactureLabel(statut: string | undefined): string {
    if (!statut) return 'N/A';
    const labels: { [key: string]: string } = {
      'PAYEE': 'Payée',
      'EMISE': 'Impayée',
      'PARTIELLEMENT_PAYEE': 'Partiellement payée',
      'BROUILLON': 'Brouillon',
      'ANNULEE': 'Annulée',
      'EN_RETARD': 'En retard',
      // Statuts de paiement (transaction)
      'VALIDE': 'Payée',
      'EN_ATTENTE': 'Impayée',
      'REJETE': 'Rejetée',
      'ANNULE': 'Annulée'
    };
    return labels[statut] || statut;
  }

  getStatutFactureClass(statut: string | undefined): string {
    if (!statut) return 'badge-grey';
    const classes: { [key: string]: string } = {
      'PAYEE': 'badge-green',
      'EMISE': 'badge-orange',
      'PARTIELLEMENT_PAYEE': 'badge-yellow',
      'BROUILLON': 'badge-grey',
      'ANNULEE': 'badge-red',
      'EN_RETARD': 'badge-red',
      // Statuts de paiement (transaction)
      'VALIDE': 'badge-green',
      'EN_ATTENTE': 'badge-orange',
      'REJETE': 'badge-red',
      'ANNULE': 'badge-grey'
    };
    return classes[statut] || 'badge-grey';
  }

  getStatutPaiement(achat: Achat): string {
    // Priorité au statut de paiement (transaction), sinon utiliser statutFacture
    return achat.statutPaiement || achat.statutFacture || '';
  }
}
