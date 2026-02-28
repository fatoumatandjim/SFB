import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CamionsService, Camion } from '../../services/camions.service';
import { TransactionsService, Transaction } from '../../services/transactions.service';
import { VoyagesService, Voyage, VoyageMarge } from '../../services/voyages.service';
import { ClientsService, Client } from '../../services/clients.service';
import { FournisseursService, Fournisseur } from '../../services/fournisseurs.service';
import { TransitairesService, Transitaire } from '../../services/transitaires.service';
import { ProduitsService, Produit } from '../../services/produits.service';
import { ComptesBancairesService, CompteBancaire } from '../../services/comptes-bancaires.service';
import { CaissesService, Caisse } from '../../services/caisses.service';
import { DepotsService, Depot } from '../../services/depots.service';
import { AxesService, Axe } from '../../services/axes.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { AuthService } from '../../services/auth.service';
import { UtilisateursService, Utilisateur } from '../../services/utilisateurs.service';

interface CamionDisplay extends Camion {
  couleur: string;
}

@Component({
  selector: 'app-camion',
  templateUrl: './camion.component.html',
  styleUrls: ['./camion.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class CamionComponent implements OnInit {
  searchTerm: string = '';
  activeFilter: string = 'tous';
  showAddModal: boolean = false;
  showEditModal: boolean = false;
  showDetailModal: boolean = false;
  showAddFraisModal: boolean = false;
  showAddVoyageModal: boolean = false;
  selectedCamion: CamionDisplay | null = null;
  isLoading: boolean = false;
  maxYear: number = new Date().getFullYear() + 1;
  transactions: Transaction[] = [];
  activeTab: 'voyages' | 'frais' = 'voyages';
  clients: Client[] = [];
  fournisseurs: Fournisseur[] = [];
  fournisseursTransport: Fournisseur[] = [];
  utilisateurs: Utilisateur[] = [];
  currentUserId: number | null = null;
  transitaires: Transitaire[] = [];
  produits: Produit[] = [];
  depots: Depot[] = [];
  axes: Axe[] = [];
  comptesBancaires: CompteBancaire[] = [];
  caisses: Caisse[] = [];
  camionsDisponibles: CamionDisplay[] = [];
  /** Clients ayant au moins un achat (pour voyage de type cession) */
  clientsWithAchat: Client[] = [];
  voyages: Voyage[] = [];
  newTransaction: Partial<Transaction> = {
    type: 'FRAIS_LOCATION',
    montant: 0,
    date: new Date().toISOString().split('T')[0],
    statut: 'EN_ATTENTE',
    description: ''
  };
  newVoyage: Partial<Voyage> = {
    camionId: 0,
    clientId: undefined,
    transitaireId: undefined,
    axeId: undefined,
    produitId: undefined,
    depotId: undefined,
    prixUnitaire: undefined,
    chauffeur: undefined,
    numeroChauffeur: undefined
  };

  voyageErrors: { [key: string]: string } = {};
  showAxeModal: boolean = false;
  newAxeNom: string = '';
  isCreatingAxe: boolean = false;

  stats = {
    enCours: 0,
    arrive: 0,
    douane: 0,
    receptionne: 0,
    livre: 0
  };

  allVoyages: Voyage[] = [];
  voyagesMarges: Map<number, VoyageMarge> = new Map();
  isLoadingMarges: Map<number, boolean> = new Map();

  newCamion: Partial<Camion> = {
    immatriculation: '',
    type: 'CITERNE',
    capacite: 0,
    fournisseurId: undefined,
    responsableId: undefined
  };

  editCamionData: Partial<Camion> = {
    immatriculation: '',
    modele: '',
    marque: '',
    annee: new Date().getFullYear(),
    type: '',
    capacite: 0,
    kilometrage: 0,
    statut: 'DISPONIBLE',
    loue: false,
    montantLocation: undefined,
    montantLocationInitial: undefined
  };

  camions: CamionDisplay[] = [];
  private couleurMap: { [key: string]: string } = {
    'blue': 'blue',
    'orange': 'orange',
    'purple': 'purple',
    'red': 'red',
    'teal': 'teal',
    'green': 'green'
  };
  private couleurIndex = 0;
  private couleurs = ['blue', 'orange', 'purple', 'red', 'teal', 'green'];

  constructor(
    private camionsService: CamionsService,
    private transactionsService: TransactionsService,
    private voyagesService: VoyagesService,
    private clientsService: ClientsService,
    private fournisseursService: FournisseursService,
    private transitairesService: TransitairesService,
    private produitsService: ProduitsService,
    private depotsService: DepotsService,
    private axesService: AxesService,
    private comptesBancairesService: ComptesBancairesService,
    private caissesService: CaissesService,
    private alertService: AlertService,
    private toastService: ToastService,
    private authService: AuthService,
    private utilisateursService: UtilisateursService
  ) { }

  ngOnInit() {
    this.loadCamions();
    this.loadClients();
    this.loadFournisseurs();
    this.loadUtilisateurs();
    this.loadCurrentUser();
    this.loadTransitaires();
    this.loadProduits();
    this.loadDepots();
    this.loadAxes();
    this.loadComptesBancaires();
    this.loadCaisses();
  }

  loadUtilisateurs() {
    // On charge uniquement les utilisateurs ayant un rôle de type
    // "Responsable Logistique" / "Logisticien" / "Simple Logisticien"
    this.utilisateursService.getLogisticiensEtResponsables().subscribe({
      next: (data) => {
        this.utilisateurs = data.filter(u => u.actif !== false);
      },
      error: (error) => {
        console.error('Erreur lors du chargement des utilisateurs:', error);
      }
    });
  }

  loadCurrentUser() {
    this.utilisateursService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUserId = user?.id ?? null;
      },
      error: () => {
        this.currentUserId = null;
      }
    });
  }

  loadClients() {
    this.clientsService.getAllClients().subscribe({
      next: (data) => {
        this.clients = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des clients:', error);
      }
    });
  }

  loadFournisseurs() {
    this.fournisseursService.getAllFournisseurs().subscribe({
      next: (data) => {
        this.fournisseurs = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des fournisseurs:', error);
      }
    });
  }

  loadFournisseursTransport() {
    this.fournisseursService.getFournisseursByType('TRANSPORT').subscribe({
      next: (data) => {
        this.fournisseursTransport = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des fournisseurs transport:', error);
      }
    });
  }

  loadTransitaires() {
    this.transitairesService.getAllTransitaires().subscribe({
      next: (data) => {
        this.transitaires = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des transitaires:', error);
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

  loadAxes() {
    this.axesService.getAllAxes().subscribe({
      next: (data) => {
        this.axes = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des axes:', error);
      }
    });
  }

  openAxeModal() {
    this.showAxeModal = true;
    this.newAxeNom = '';
  }

  closeAxeModal() {
    this.showAxeModal = false;
    this.newAxeNom = '';
  }

  createAxe() {
    if (!this.newAxeNom || this.newAxeNom.trim() === '') {
      this.toastService.error('Veuillez saisir un nom pour l\'axe');
      return;
    }

    this.isCreatingAxe = true;
    this.axesService.createAxe({ nom: this.newAxeNom.trim() }).subscribe({
      next: (newAxe) => {
        this.axes.push(newAxe);
        this.toastService.success('Axe créé avec succès');
        this.closeAxeModal();
        this.isCreatingAxe = false;
        // Sélectionner automatiquement le nouvel axe
        this.newVoyage.axeId = newAxe.id;
      },
      error: (error) => {
        console.error('Erreur lors de la création de l\'axe:', error);
        const errorMsg = error?.error?.message || 'Erreur lors de la création de l\'axe';
        this.toastService.error(errorMsg);
        this.isCreatingAxe = false;
      }
    });
  }

  loadComptesBancaires() {
    this.comptesBancairesService.getAllComptes().subscribe({
      next: (data) => {
        this.comptesBancaires = data.filter(c => c.statut === 'ACTIF');
      },
      error: (error) => {
        console.error('Erreur lors du chargement des comptes bancaires:', error);
      }
    });
  }

  loadCaisses() {
    this.caissesService.getAllCaisses().subscribe({
      next: (data) => {
        this.caisses = data.filter(c => c.statut === 'ACTIF');
      },
      error: (error) => {
        console.error('Erreur lors du chargement des caisses:', error);
      }
    });
  }

  loadCamions() {
    this.isLoading = true;
    this.camionsService.getAllCamions().subscribe({
      next: (data) => {

        this.camions = data.map(c => ({
          ...c,
          couleur: this.getNextCouleur(),
          dernierControle: c.dernierControle ? this.formatDate(c.dernierControle) : ''
        }));
        this.loadAllVoyages();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des camions:', error);
        this.isLoading = false;
      }
    });
  }

  getNextCouleur(): string {
    const couleur = this.couleurs[this.couleurIndex % this.couleurs.length];
    this.couleurIndex++;
    return couleur;
  }

  formatDate(date: string): string {
    if (!date) return '';
    const d = new Date(date);
    const months = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
                    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
    return `${d.getDate()} ${months[d.getMonth()]} ${d.getFullYear()}`;
  }

  updateStats() {
    // Compter les voyages par statut
    this.stats.enCours = this.allVoyages.filter(v =>
      v.statut === 'CHARGEMENT' ||
      v.statut === 'CHARGE' ||
      v.statut === 'DEPART' ||
      v.statut === 'ARRIVER'
    ).length;

    this.stats.arrive = this.allVoyages.filter(v => v.statut === 'ARRIVER').length;
    this.stats.douane = this.allVoyages.filter(v => v.statut === 'DOUANE').length;
    this.stats.receptionne = this.allVoyages.filter(v => v.statut === 'RECEPTIONNER').length;
    this.stats.livre = this.allVoyages.filter(v => v.statut === 'LIVRE').length;
  }

  loadAllVoyages() {
    this.voyagesService.getAllVoyages().subscribe({
      next: (data: Voyage[]) => {
        this.allVoyages = data;
        this.updateStats();
      },
      error: (error) => {
        console.error('Erreur lors du chargement des voyages:', error);
        this.allVoyages = [];
      }
    });
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
  }

  get filteredCamions(): CamionDisplay[] {
    let filtered = this.camions;

    if (this.activeFilter === 'disponibles') {
      filtered = filtered.filter(c => c.statut === 'DISPONIBLE' && !c.loue);
    } else if (this.activeFilter === 'en-route') {
      filtered = filtered.filter(c => c.statut === 'EN_ROUTE');
    } else if (this.activeFilter === 'en-maintenance') {
      filtered = filtered.filter(c => c.statut === 'EN_MAINTENANCE');
    } else if (this.activeFilter === 'hors-service') {
      filtered = filtered.filter(c => c.statut === 'HORS_SERVICE');
    } else if (this.activeFilter === 'loues') {
      filtered = filtered.filter(c => c.loue === true);
    }

    if (this.searchTerm) {
      filtered = filtered.filter(c =>
        c.immatriculation.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }

    return filtered;
  }

  nouveauVoyage() {
    // Charger les camions disponibles pour le voyage
    this.camionsDisponibles = this.camions.filter(c =>
      c.statut === 'DISPONIBLE'
    );

    // Charger les fournisseurs de transport
    this.loadFournisseursTransport();

    this.newVoyage = {
      camionId: 0,
      clientId: undefined,
      transitaireId: undefined,
      axeId: undefined,
      produitId: undefined,
      depotId: undefined,
      responsableId: undefined,
      prixUnitaire: undefined,
      chauffeur: undefined,
      numeroChauffeur: undefined,
      cession: false
    };

    this.loadClientsWithAchat();
    this.showAddVoyageModal = true;
  }

  loadClientsWithAchat() {
    this.clientsService.getClientsWithAtLeastOneAchat().subscribe({
      next: (list) => (this.clientsWithAchat = list || []),
      error: () => (this.clientsWithAchat = [])
    });
  }

  closeAddVoyageModal() {
    this.showAddVoyageModal = false;
    this.newVoyage = {
      camionId: 0,
      clientId: undefined,
      transitaireId: undefined,
      axeId: undefined,
      produitId: undefined,
      depotId: undefined,
      responsableId: undefined,
      prixUnitaire: undefined,
      chauffeur: undefined,
      numeroChauffeur: undefined,
      cession: false
    };
    this.voyageErrors = {};
  }

  addVoyage() {
    // Réinitialiser les erreurs
    this.voyageErrors = {};

    if (!this.validateVoyage()) {
      return;
    }

    this.isLoading = true;
    const isCession = !!this.newVoyage.cession;
    const voyage: Voyage = {
      camionId: this.newVoyage.camionId!,
      clientId: (this.newVoyage.clientId && this.newVoyage.clientId > 0) ? this.newVoyage.clientId : undefined,
      transitaireId: this.newVoyage.transitaireId!,
      axeId: this.newVoyage.axeId!,
      produitId: this.newVoyage.produitId!,
      depotId: this.newVoyage.depotId!,
      responsableId: this.newVoyage.responsableId!,
      prixUnitaire: isCession ? undefined : this.newVoyage.prixUnitaire,
      notes: this.newVoyage.notes,
      chauffeur: this.newVoyage.chauffeur,
      numeroChauffeur: this.newVoyage.numeroChauffeur,
      cession: isCession
    };

    this.voyagesService.createVoyage(voyage).subscribe({
      next: (newVoyage) => {
        this.isLoading = false;
        this.voyageErrors = {};
        this.toastService.success('Voyage créé avec succès');
        this.closeAddVoyageModal();
        // Recharger les camions pour mettre à jour les statuts
        this.loadCamions();
        // Recharger tous les voyages pour mettre à jour les stats
        this.loadAllVoyages();
        // Si la modal de détails est ouverte, recharger les voyages
        if (this.selectedCamion && this.showDetailModal && this.selectedCamion.id) {
          this.loadVoyages(this.selectedCamion.id);
        }
      },
      error: (error: any) => {
        console.error('Erreur lors de la création du voyage:', error);
        this.isLoading = false;

        // Gérer les erreurs de validation du backend
        if (error?.error) {
          const errorData = error.error;

          // Si c'est une erreur de validation avec des champs spécifiques
          if (errorData.errors && Array.isArray(errorData.errors)) {
            errorData.errors.forEach((err: any) => {
              const field = err.field || err.property;
              if (field) {
                this.voyageErrors[field] = err.message || err.defaultMessage;
              }
            });
          }

          // Gérer les messages d'erreur spécifiques
          const errorMessage = errorData.message || errorData.error || error.message;

          // Messages d'erreur spécifiques pour les champs
          if (errorMessage) {
            const lowerMessage = errorMessage.toLowerCase();

            // Dépôt vide
            if (lowerMessage.includes('dépôt') && (lowerMessage.includes('vide') || lowerMessage.includes('insuffisant') || lowerMessage.includes('stock'))) {
              this.voyageErrors['depotId'] = 'Le dépôt sélectionné est vide ou n\'a pas assez de stock';
            }
            // Camion non disponible
            else if (lowerMessage.includes('camion') && (lowerMessage.includes('disponible') || lowerMessage.includes('occupé'))) {
              this.voyageErrors['camionId'] = 'Le camion sélectionné n\'est pas disponible';
            }
            // Produit invalide
            else if (lowerMessage.includes('produit')) {
              this.voyageErrors['produitId'] = errorMessage;
            }
            // Transitaire invalide
            else if (lowerMessage.includes('transitaire')) {
              this.voyageErrors['transitaireId'] = errorMessage;
            }
            // Prix invalide
            else if (lowerMessage.includes('prix') || lowerMessage.includes('prixunitaire')) {
              this.voyageErrors['prixUnitaire'] = errorMessage;
            }
            // Erreur générale
            else {
              this.voyageErrors['general'] = errorMessage;
            }
          }

          // Si aucune erreur spécifique n'a été trouvée, afficher un message général
          if (Object.keys(this.voyageErrors).length === 0) {
            this.voyageErrors['general'] = errorMessage || 'Erreur lors de la création du voyage. Veuillez réessayer.';
          }
        } else {
          this.voyageErrors['general'] = 'Erreur lors de la création du voyage. Veuillez réessayer.';
        }
      }
    });
  }

  validateVoyage(): boolean {
    this.voyageErrors = {};
    let isValid = true;

    if (!this.newVoyage.camionId || this.newVoyage.camionId <= 0) {
      this.voyageErrors['camionId'] = 'Veuillez sélectionner un camion';
      isValid = false;
    }
    if (!this.newVoyage.produitId) {
      this.voyageErrors['produitId'] = 'Veuillez sélectionner un produit';
      isValid = false;
    }
    if (!this.newVoyage.depotId || this.newVoyage.depotId <= 0) {
      this.voyageErrors['depotId'] = 'Veuillez sélectionner un dépôt';
      isValid = false;
    }
    if (!this.newVoyage.transitaireId || this.newVoyage.transitaireId <= 0) {
      this.voyageErrors['transitaireId'] = 'Veuillez sélectionner un transitaire';
      isValid = false;
    }
    if (!this.newVoyage.axeId || this.newVoyage.axeId <= 0) {
      this.voyageErrors['axeId'] = 'Veuillez sélectionner un axe';
      isValid = false;
    }
    if (!this.newVoyage.responsableId || this.newVoyage.responsableId <= 0) {
      this.voyageErrors['responsableId'] = 'Veuillez sélectionner un responsable';
      isValid = false;
    }
    if (this.newVoyage.cession) {
      if (!this.newVoyage.clientId || this.newVoyage.clientId <= 0) {
        this.voyageErrors['clientId'] = 'Veuillez sélectionner un client (ayant déjà un achat)';
        isValid = false;
      }
    } else {
      if (!this.newVoyage.prixUnitaire || this.newVoyage.prixUnitaire <= 0) {
        this.voyageErrors['prixUnitaire'] = 'Veuillez saisir le prix unitaire de transport';
        isValid = false;
      }
    }

    return isValid;
  }

  onDepotChange() {
    // La validation du stock se fera côté backend lors de la création du voyage
    // On peut afficher un message informatif si nécessaire
  }


  openAddModal() {
    this.loadFournisseursTransport();

    this.showAddModal = true;
    this.newCamion = {
      immatriculation: '',
      type: 'CITERNE',
      capacite: 0,
      fournisseurId: undefined,
      responsableId: undefined
    };
  }


  closeAddModal() {
    this.showAddModal = false;
  }

  addCamion() {
    if (!this.validateCamion()) {
      return;
    }

    this.isLoading = true;
    this.camionsService.createCamion(this.newCamion as Camion).subscribe({
      next: (camion) => {
        this.camions.push({
          ...camion,
          couleur: this.getNextCouleur(),
          dernierControle: camion.dernierControle ? this.formatDate(camion.dernierControle) : ''
        });
        this.updateStats();
        this.closeAddModal();
        this.isLoading = false;
        this.toastService.success('Camion ajouté avec succès!');
      },
      error: (error) => {
        console.error('Erreur lors de l\'ajout du camion:', error);
        this.toastService.error('Erreur lors de l\'ajout du camion. Veuillez réessayer.');
        this.isLoading = false;
      }
    });
  }

  validateCamion(): boolean {
    if (!this.newCamion.immatriculation) {
      this.toastService.warning('Veuillez remplir tous les champs obligatoires');
      return false;
    }
    if (!this.newCamion.capacite || this.newCamion.capacite <= 0) {
      this.toastService.warning('La capacité doit être supérieure à 0');
      return false;
    }
    if (!this.newCamion.responsableId || this.newCamion.responsableId <= 0) {
      this.toastService.warning('Veuillez sélectionner un responsable');
      return false;
    }
    return true;
  }

  canEditCamion(camion: CamionDisplay): boolean {
    if (this.authService.hasRole('ROLE_ADMIN')) return true;
    if (this.currentUserId == null || !camion.responsableId) return false;
    return camion.responsableId === this.currentUserId;
  }

  viewCamion(camion: CamionDisplay) {
    this.selectedCamion = camion;
    this.showDetailModal = true;
    this.activeTab = 'voyages'; // Reset to voyages tab by default

    // Charger les voyages du camion
    if (camion.id) {
      this.loadVoyages(camion.id);
    }

    // Charger les transactions si le camion est loué
    if (camion.loue && camion.id) {
      this.loadTransactions(camion.id);
    }
  }

  setActiveTab(tab: 'voyages' | 'frais') {
    this.activeTab = tab;
  }

  loadTransactions(camionId: number) {
    this.transactionsService.getTransactionsByCamion(camionId).subscribe({
      next: (data) => {
        this.transactions = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des transactions:', error);
        this.transactions = [];
      }
    });
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedCamion = null;
    this.transactions = [];
    this.voyages = [];
    this.showAddFraisModal = false;
  }

  loadVoyages(camionId: number) {
    this.voyagesService.getVoyagesByCamion(camionId).subscribe({
      next: (data) => {
        this.voyages = data;
        // Charger les marges pour chaque voyage
        this.voyages.forEach(voyage => {
          if (voyage.id) {
            this.loadVoyageMarge(voyage.id);
          }
        });
      },
      error: (error) => {
        console.error('Erreur lors du chargement des voyages:', error);
        this.voyages = [];
      }
    });
  }

  loadVoyageMarge(voyageId: number) {
    if (this.isLoadingMarges.get(voyageId)) {
      return; // Déjà en cours de chargement
    }
    this.isLoadingMarges.set(voyageId, true);
    this.voyagesService.getVoyageMarge(voyageId).subscribe({
      next: (marge) => {
        this.voyagesMarges.set(voyageId, marge);
        this.isLoadingMarges.set(voyageId, false);
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la marge:', error);
        this.isLoadingMarges.set(voyageId, false);
      }
    });
  }

  getVoyageMarge(voyageId: number | undefined): VoyageMarge | null {
    if (!voyageId) return null;
    return this.voyagesMarges.get(voyageId) || null;
  }

  isLoadingMarge(voyageId: number | undefined): boolean {
    if (!voyageId) return false;
    return this.isLoadingMarges.get(voyageId) || false;
  }

  formatDateTime(dateTime: string | undefined): string {
    if (!dateTime) return '';
    const d = new Date(dateTime);
    const months = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];
    const hours = d.getHours().toString().padStart(2, '0');
    const minutes = d.getMinutes().toString().padStart(2, '0');
    return `${d.getDate()} ${months[d.getMonth()]} ${d.getFullYear()} ${hours}:${minutes}`;
  }

  getStatutLabel(statut: string): string {
    const statuts: { [key: string]: string } = {
      'CHARGEMENT': 'Chargement',
      'CHARGE': 'Chargé',
      'DEPART': 'Départ',
      'ARRIVER': 'Arrivé',
      'DOUANE': 'Douane',
      'RECEPTIONNER': 'Réceptionné',
      'LIVRE': 'Livré'
    };
    return statuts[statut] || statut;
  }

  getStatutClass(statut: string): string {
    const classes: { [key: string]: string } = {
      'CHARGEMENT': 'status-chargement',
      'CHARGE': 'status-charge',
      'DEPART': 'status-depart',
      'ARRIVER': 'status-arriver',
      'DOUANE': 'status-douane',
      'RECEPTIONNER': 'status-receptionner',
      'LIVRE': 'status-livre'
    };
    return classes[statut] || '';
  }

  openAddFraisModal() {
    if (!this.selectedCamion || !this.selectedCamion.id) return;
    this.newTransaction = {
      type: 'FRAIS_LOCATION',
      montant: this.selectedCamion.montantLocation || 0,
      date: new Date().toISOString().split('T')[0],
      statut: 'EN_ATTENTE',
      description: `Paiement de location pour le camion ${this.selectedCamion.immatriculation}`,
      camionId: this.selectedCamion.id
    };
    this.showAddFraisModal = true;
  }

  closeAddFraisModal() {
    this.showAddFraisModal = false;
    this.newTransaction = {
      type: 'FRAIS_LOCATION',
      montant: 0,
      date: new Date().toISOString().split('T')[0],
      statut: 'EN_ATTENTE',
      description: ''
    };
  }

  addFrais() {
    if (!this.newTransaction.montant || this.newTransaction.montant <= 0) {
      this.toastService.warning('Veuillez saisir un montant valide');
      return;
    }
    if (!this.selectedCamion || !this.selectedCamion.id) {
      return;
    }

    this.isLoading = true;
    const transaction: Transaction = {
      type: 'FRAIS_LOCATION',
      montant: this.newTransaction.montant!,
      date: new Date(this.newTransaction.date!).toISOString(),
      statut: 'EN_ATTENTE',
      description: this.newTransaction.description || `Paiement de location pour le camion ${this.selectedCamion.immatriculation}`,
      camionId: this.selectedCamion.id
    };

    this.transactionsService.createTransaction(transaction).subscribe({
      next: (newTransaction) => {
        this.transactions.push(newTransaction);
        this.closeAddFraisModal();
        this.isLoading = false;
        this.toastService.success('Frais de location ajouté avec succès');
      },
      error: (error) => {
        console.error('Erreur lors de l\'ajout du frais:', error);
        this.toastService.error('Erreur lors de l\'ajout du frais. Veuillez réessayer.');
        this.isLoading = false;
      }
    });
  }

  getTotalPaye(): number {
    // Le montant total payé = montantLocationInitial (déjà payé initialement) + somme des transactions validées
    const montantInitialPaye = this.selectedCamion?.montantLocationInitial || 0;
    const transactionsPayees = this.transactions
      .filter(t => t.statut === 'VALIDE')
      .reduce((sum, t) => sum + (t.montant || 0), 0);
    return montantInitialPaye + transactionsPayees;
  }

  getResteAPayer(): number {
    if (!this.selectedCamion || !this.selectedCamion.montantLocation) {
      return 0;
    }
    // montantLocation = montant total à payer
    // getTotalPaye() = montant total déjà payé (initial + transactions)
    // reste = total - déjà payé
    return this.selectedCamion.montantLocation - this.getTotalPaye();
  }

  mettreEnMaintenance() {
    if (!this.selectedCamion || !this.selectedCamion.id) {
      return;
    }

    if (!this.selectedCamion || !this.selectedCamion.id) return;

    this.alertService.confirm(
      `Êtes-vous sûr de vouloir mettre le camion ${this.selectedCamion.immatriculation} en maintenance ?`,
      'Confirmation'
    ).subscribe(confirmed => {
      if (!confirmed) return;
      this.isLoading = true;
      const camionUpdate = { ...this.selectedCamion!, statut: 'EN_MAINTENANCE' as const };

      this.camionsService.updateCamion(this.selectedCamion!.id!, camionUpdate).subscribe({
        next: (camion) => {
          const index = this.camions.findIndex(c => c.id === camion.id);
          if (index !== -1) {
            this.camions[index] = {
              ...camion,
              couleur: this.camions[index].couleur,
              dernierControle: camion.dernierControle ? this.formatDate(camion.dernierControle) : ''
            };
            this.selectedCamion = this.camions[index];
          }
          this.updateStats();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erreur lors de la mise en maintenance:', error);
          if (error?.status === 403) {
            this.toastService.error('Seul le responsable du camion ou un administrateur peut effectuer cette action.');
          } else {
            this.toastService.error('Erreur lors de la mise en maintenance du camion');
          }
          this.isLoading = false;
        }
      });
    });
  }

  editCamion(camion: CamionDisplay) {
    this.loadFournisseursTransport(); // pour le select Fournisseur Transport
    this.editCamionData = {
      id: camion.id,
      immatriculation: camion.immatriculation,
      modele: camion.modele,
      marque: camion.marque,
      annee: camion.annee,
      type: camion.type,
      capacite: camion.capacite,
      kilometrage: camion.kilometrage,
      statut: camion.statut,
      loue: camion.loue || false,
      montantLocation: camion.montantLocation,
      montantLocationInitial: camion.montantLocationInitial,
      dernierControle: camion.dernierControle ? this.parseDateForInput(camion.dernierControle) : undefined,
      responsableId: camion.responsableId ?? undefined,
      fournisseurId: camion.fournisseurId ?? undefined
    };
    this.showEditModal = true;
  }

  parseDateForInput(dateString: string): string {
    if (!dateString) return '';
    // Si la date est au format "DD Mois YYYY", la convertir en format ISO
    const months: { [key: string]: string } = {
      'Janvier': '01', 'Février': '02', 'Mars': '03', 'Avril': '04',
      'Mai': '05', 'Juin': '06', 'Juillet': '07', 'Août': '08',
      'Septembre': '09', 'Octobre': '10', 'Novembre': '11', 'Décembre': '12'
    };

    const parts = dateString.split(' ');
    if (parts.length === 3) {
      const day = parts[0].padStart(2, '0');
      const month = months[parts[1]] || '01';
      const year = parts[2];
      return `${year}-${month}-${day}`;
    }

    // Si c'est déjà au format ISO ou autre, essayer de le parser
    try {
      const d = new Date(dateString);
      if (!isNaN(d.getTime())) {
        return d.toISOString().split('T')[0];
      }
    } catch (e) {
      console.error('Erreur lors du parsing de la date:', e);
    }

    return '';
  }

  closeEditModal() {
    this.showEditModal = false;
    this.editCamionData = {
      immatriculation: '',
      modele: '',
      marque: '',
      annee: new Date().getFullYear(),
      type: '',
      capacite: 0,
      kilometrage: 0,
      statut: 'DISPONIBLE',
      loue: false,
      montantLocation: undefined,
      montantLocationInitial: undefined
    };
  }

  onEditLoueChange() {
    if (!this.editCamionData.loue) {
      this.editCamionData.montantLocation = undefined;
      this.editCamionData.montantLocationInitial = undefined;
    }
  }

  updateCamion() {
    if (!this.validateEditCamion()) {
      return;
    }

    if (!this.editCamionData.id) {
      this.toastService.error('Erreur: ID du camion manquant');
      return;
    }

    this.isLoading = true;
    this.camionsService.updateCamion(this.editCamionData.id, this.editCamionData as Camion).subscribe({
      next: (camion) => {
        const index = this.camions.findIndex(c => c.id === camion.id);
        if (index !== -1) {
          this.camions[index] = {
            ...camion,
            couleur: this.camions[index].couleur,
            dernierControle: camion.dernierControle ? this.formatDate(camion.dernierControle) : ''
          };
        }
        this.updateStats();
        this.closeEditModal();
        this.isLoading = false;
        this.toastService.success('Camion modifié avec succès');
      },
      error: (error) => {
        console.error('Erreur lors de la modification du camion:', error);
        if (error?.status === 403) {
          this.toastService.error('Seul le responsable du camion ou un administrateur peut le modifier.');
        } else {
          this.toastService.error('Erreur lors de la modification du camion. Veuillez réessayer.');
        }
        this.isLoading = false;
      }
    });
  }

  validateEditCamion(): boolean {
    if (!this.editCamionData.immatriculation || !this.editCamionData.modele ||
        !this.editCamionData.marque || !this.editCamionData.type) {
      this.toastService.warning('Veuillez remplir tous les champs obligatoires');
      return false;
    }
    if (!this.editCamionData.capacite || this.editCamionData.capacite <= 0) {
      this.toastService.warning('La capacité doit être supérieure à 0');
      return false;
    }
    return true;
  }

  deleteCamion(camion: CamionDisplay) {
    this.alertService.confirm(
      `Êtes-vous sûr de vouloir supprimer le camion ${camion.immatriculation}?`,
      'Confirmation de suppression'
    ).subscribe(confirmed => {
      if (!confirmed || !camion.id) return;
      this.isLoading = true;
      this.camionsService.deleteCamion(camion.id).subscribe({
        next: () => {
          this.camions = this.camions.filter(c => c.id !== camion.id);
          this.updateStats();
          this.isLoading = false;
          this.toastService.success('Camion supprimé avec succès');
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          if (error?.status === 403) {
            this.toastService.error('Seul le responsable du camion ou un administrateur peut le supprimer.');
          } else {
            this.toastService.error('Erreur lors de la suppression du camion');
          }
          this.isLoading = false;
        }
      });
    });
  }

  // Vérifier si l'utilisateur connecté a le rôle ADMIN
  isAdmin(): boolean {
    return this.authService.hasRole('ROLE_ADMIN');
  }

  // Vérifier si l'utilisateur connecté est comptable (peut modifier les prix de transport)
  isComptable(): boolean {
    return this.authService.isComptable();
  }

  // Modal modification prix unitaire (comptable)
  showEditPrixModal = false;
  editingVoyage: Voyage | null = null;
  editingPrixUnitaire: number = 0;

  openEditPrixModal(voyage: Voyage) {
    this.editingVoyage = voyage;
    this.editingPrixUnitaire = voyage.prixUnitaire ?? 0;
    this.showEditPrixModal = true;
  }

  closeEditPrixModal() {
    this.showEditPrixModal = false;
    this.editingVoyage = null;
    this.editingPrixUnitaire = 0;
  }

  savePrixUnitaire() {
    if (!this.editingVoyage?.id || this.editingPrixUnitaire <= 0) {
      this.toastService.warning('Veuillez saisir un prix valide');
      return;
    }
    this.isLoading = true;
    const payload = { ...this.editingVoyage, prixUnitaire: this.editingPrixUnitaire };
    this.voyagesService.updateVoyage(this.editingVoyage.id, payload).subscribe({
      next: (updated) => {
        const idx = this.voyages.findIndex(v => v.id === updated.id);
        if (idx !== -1) this.voyages[idx] = updated;
        this.toastService.success('Prix unitaire mis à jour');
        this.closeEditPrixModal();
        this.isLoading = false;
      },
      error: (err) => {
        this.toastService.error(err?.error?.message || 'Erreur lors de la mise à jour du prix');
        this.isLoading = false;
      }
    });
  }
}
