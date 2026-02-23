import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClientsService, Client } from '../../services/clients.service';
import { FournisseursService, Fournisseur } from '../../services/fournisseurs.service';
import { FacturesService, Facture } from '../../services/factures.service';
import { CamionsService, CamionWithVoyagesCount } from '../../services/camions.service';
import { VoyagesService, Voyage } from '../../services/voyages.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';

interface Contact {
  id?: number;
  nom: string;
  email: string;
  telephone: string;
  adresse: string;
  type: 'client' | 'fournisseur';
  statut?: 'actif' | 'inactif' | 'premium' | 'vip';
  solde?: number;
  factures?: number;
  derniereActivite?: string;
  initiales: string;
  couleur: string;
  codeClient?: string;
  codeFournisseur?: string;
  typeClient?: string;
  ville?: string;
  pays?: string;
  contactPersonne?: string;
}

@Component({
  selector: 'app-client-fournisseur',
  templateUrl: './client-fournisseur.component.html',
  styleUrls: ['./client-fournisseur.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class ClientFournisseurComponent implements OnInit {
  activeTab: 'clients' | 'fournisseurs' = 'clients';
  searchTerm: string = '';
  activeFilter: string = 'tous';

  stats = {
    clients: {
      total: 0,
      actifs: 0,
      premium: 0,
      vip: 0
    },
    fournisseurs: {
      total: 0,
      actifs: 0,
      principaux: 0
    }
  };

  clients: Contact[] = [];
  fournisseurs: Contact[] = [];

  constructor(
    private clientsService: ClientsService,
    private fournisseursService: FournisseursService,
    private facturesService: FacturesService,
    private camionsService: CamionsService,
    private voyagesService: VoyagesService,
    private alertService: AlertService,
    private toastService: ToastService
  ) { }

  ngOnInit() {
    this.loadClients();
    this.loadFournisseurs();
  }

  loadClients() {
    this.isLoading = true;
    this.clientsService.getAllClients().subscribe({
      next: (data) => {
        this.clients = data.map(client => this.mapClientToContact(client));
        this.updateStats();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des clients:', error);
        this.isLoading = false;
      }
    });
  }

  loadFournisseurs() {
    this.isLoading = true;
    this.fournisseursService.getAllFournisseurs().subscribe({
      next: (data) => {
        this.fournisseurs = data.map(fournisseur => this.mapFournisseurToContact(fournisseur));
        this.updateStats();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des fournisseurs:', error);
        this.isLoading = false;
      }
    });
  }

  mapClientToContact(client: Client): Contact {
    const words = client.nom.split(' ');
    const initiales = words.length >= 2
      ? (words[0][0] + words[1][0]).toUpperCase()
      : client.nom.substring(0, 2).toUpperCase();

    const colors = ['blue', 'green', 'purple', 'orange', 'red', 'teal', 'pink', 'indigo'];
    const colorIndex = client.nom.charCodeAt(0) % colors.length;

    return {
      id: client.id,
      nom: client.nom,
      email: client.email,
      telephone: client.telephone,
      adresse: client.adresse,
      type: 'client',
      initiales: initiales,
      couleur: colors[colorIndex],
      codeClient: client.codeClient,
      typeClient: client.type,
      ville: client.ville,
      pays: client.pays
    };
  }

  mapFournisseurToContact(fournisseur: Fournisseur): Contact {
    const words = fournisseur.nom.split(' ');
    const initiales = words.length >= 2
      ? (words[0][0] + words[1][0]).toUpperCase()
      : fournisseur.nom.substring(0, 2).toUpperCase();

    const colors = ['blue', 'green', 'purple', 'orange', 'red', 'teal', 'pink', 'indigo'];
    const colorIndex = fournisseur.nom.charCodeAt(0) % colors.length;

    return {
      id: fournisseur.id,
      nom: fournisseur.nom,
      email: fournisseur.email,
      telephone: fournisseur.telephone,
      adresse: fournisseur.adresse,
      type: 'fournisseur',
      initiales: initiales,
      couleur: colors[colorIndex],
      codeFournisseur: fournisseur.codeFournisseur,
      ville: fournisseur.ville,
      pays: fournisseur.pays,
      contactPersonne: fournisseur.contactPersonne
    };
  }

  updateStats() {
    this.stats.clients.total = this.clients.length;
    this.stats.clients.actifs = this.clients.length; // Tous actifs par défaut
    this.stats.fournisseurs.total = this.fournisseurs.length;
    this.stats.fournisseurs.actifs = this.fournisseurs.length; // Tous actifs par défaut
  }

  setTab(tab: 'clients' | 'fournisseurs') {
    this.activeTab = tab;
    this.activeFilter = 'tous';
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
  }

  get filteredContacts(): Contact[] {
    const contacts = this.activeTab === 'clients' ? this.clients : this.fournisseurs;
    let filtered = contacts;

    // Les filtres de statut ne sont plus applicables car le backend ne gère pas ces statuts
    // On garde la structure pour une éventuelle extension future

    if (this.searchTerm) {
      filtered = filtered.filter(c =>
        c.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        c.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        c.telephone.includes(this.searchTerm) ||
        (c.codeClient && c.codeClient.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
        (c.codeFournisseur && c.codeFournisseur.toLowerCase().includes(this.searchTerm.toLowerCase()))
      );
    }

    return filtered;
  }

  showAddModal: boolean = false;
  showDetailModal: boolean = false;
  showEditModal: boolean = false;
  selectedContact: Contact | null = null;
  selectedClient: Client | null = null;
  selectedFournisseur: Fournisseur | null = null;
  isLoading: boolean = false;
  clientFactures: Facture[] = [];
  isLoadingFactures: boolean = false;
  isExportingFactures: boolean = false;
  clientVoyages: Voyage[] = [];
  isLoadingVoyagesClient: boolean = false;
  fournisseurCamions: CamionWithVoyagesCount[] = [];
  isLoadingCamions: boolean = false;
  selectedCamion: CamionWithVoyagesCount | null = null;
  camionVoyages: Voyage[] = [];
  isLoadingVoyages: boolean = false;
  showCamionVoyagesModal: boolean = false;

  newClient: {
    nom: string;
    email: string;
    telephone: string;
    adresse: string;
    type: 'PARTICULIER' | 'ENTREPRISE' | 'GOUVERNEMENT';
    ville?: string;
    pays?: string;
  } = {
    nom: '',
    email: '',
    telephone: '',
    adresse: '',
    type: 'ENTREPRISE',
    ville: '',
    pays: ''
  };

  newFournisseur: {
    nom: string;
    email: string;
    telephone: string;
    adresse: string;
    ville?: string;
    pays?: string;
    contactPersonne?: string;
    typeFournisseur?: 'ACHAT' | 'TRANSPORT';
  } = {
    nom: '',
    email: '',
    telephone: '',
    adresse: '',
    ville: '',
    pays: '',
    contactPersonne: '',
    typeFournisseur: 'ACHAT'
  };

  nouveauContact() {
    if (this.activeTab === 'clients') {
      this.newClient = {
        nom: '',
        email: '',
        telephone: '',
        adresse: '',
        type: 'ENTREPRISE',
        ville: '',
        pays: ''
      };
    } else {
      this.newFournisseur = {
        nom: '',
        email: '',
        telephone: '',
        adresse: '',
        ville: '',
        pays: '',
        contactPersonne: ''
      };
    }
    this.showAddModal = true;
  }

  closeAddModal() {
    this.showAddModal = false;
    if (this.activeTab === 'clients') {
      this.newClient = {
        nom: '',
        email: '',
        telephone: '',
        adresse: '',
        type: 'ENTREPRISE',
        ville: '',
        pays: ''
      };
    } else {
      this.newFournisseur = {
        nom: '',
        email: '',
        telephone: '',
        adresse: '',
        ville: '',
        pays: '',
        contactPersonne: '',
        typeFournisseur: 'ACHAT'
      };
    }
  }

  validateClient(): boolean {
    if (!this.newClient.nom || this.newClient.nom.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom du client');
      return false;
    }
    if (!this.newClient.email || this.newClient.email.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'email du client');
      return false;
    }
    if (!this.newClient.telephone || this.newClient.telephone.trim() === '') {
      this.toastService.warning('Veuillez saisir le téléphone du client');
      return false;
    }
    if (!this.newClient.adresse || this.newClient.adresse.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'adresse du client');
      return false;
    }
    return true;
  }

  validateFournisseur(): boolean {
    if (!this.newFournisseur.nom || this.newFournisseur.nom.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom du fournisseur');
      return false;
    }
    if (!this.newFournisseur.email || this.newFournisseur.email.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'email du fournisseur');
      return false;
    }
    if (!this.newFournisseur.telephone || this.newFournisseur.telephone.trim() === '') {
      this.toastService.warning('Veuillez saisir le téléphone du fournisseur');
      return false;
    }
    if (!this.newFournisseur.adresse || this.newFournisseur.adresse.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'adresse du fournisseur');
      return false;
    }
    return true;
  }

  saveClient() {
    if (!this.validateClient()) {
      return;
    }

    this.isLoading = true;

    const clientToSave: Client = {
      nom: this.newClient.nom.trim(),
      email: this.newClient.email.trim(),
      telephone: this.newClient.telephone.trim(),
      adresse: this.newClient.adresse.trim(),
      type: this.newClient.type,
      ville: this.newClient.ville?.trim() || undefined,
      pays: this.newClient.pays?.trim() || undefined
      // codeClient sera généré automatiquement par le backend
    };

    this.clientsService.createClient(clientToSave).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Client créé avec succès!');
        this.closeAddModal();
        this.loadClients();
      },
      error: (error) => {
        console.error('Erreur lors de la création du client:', error);
        this.isLoading = false;
        const errorMessage = error.error?.message || error.message || 'Erreur lors de la création du client';
        this.toastService.error(errorMessage);
      }
    });
  }

  saveFournisseur() {
    if (!this.validateFournisseur()) {
      return;
    }

    this.isLoading = true;

    const fournisseurToSave: Fournisseur = {
      nom: this.newFournisseur.nom.trim(),
      email: this.newFournisseur.email.trim(),
      telephone: this.newFournisseur.telephone.trim(),
      adresse: this.newFournisseur.adresse.trim(),
      ville: this.newFournisseur.ville?.trim() || undefined,
      pays: this.newFournisseur.pays?.trim() || undefined,
      contactPersonne: this.newFournisseur.contactPersonne?.trim() || undefined,
      typeFournisseur: this.newFournisseur.typeFournisseur || 'ACHAT'
      // codeFournisseur sera généré automatiquement par le backend
    };

    this.fournisseursService.createFournisseur(fournisseurToSave).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Fournisseur créé avec succès!');
        this.closeAddModal();
        this.loadFournisseurs();
      },
      error: (error) => {
        console.error('Erreur lors de la création du fournisseur:', error);
        this.isLoading = false;
        const errorMessage = error.error?.message || error.message || 'Erreur lors de la création du fournisseur';
        this.toastService.error(errorMessage);
      }
    });
  }

  editClient: Partial<Client> = {};
  editFournisseur: Partial<Fournisseur> = {};

  viewContact(contact: Contact) {
    this.selectedContact = contact;
    // Récupérer les données complètes depuis l'API
    if (contact.type === 'client' && contact.id) {
      const clientId = contact.id;
      this.clientsService.getClientById(clientId).subscribe({
        next: (client: Client) => {
          this.selectedClient = client;
          this.showDetailModal = true;
          // Charger les factures du client
          this.loadClientFactures(clientId);
          // Charger les voyages du client
          this.loadClientVoyages(clientId);
        },
        error: (error) => {
          console.error('Erreur lors du chargement du client:', error);
          // Utiliser les données du contact si l'API échoue
          this.selectedContact = contact;
          this.showDetailModal = true;
        }
      });
    } else if (contact.type === 'fournisseur' && contact.id) {
      this.fournisseursService.getFournisseurById(contact.id).subscribe({
        next: (fournisseur: Fournisseur) => {
          this.selectedFournisseur = fournisseur;
          if (contact.id) {
            this.loadFournisseurCamions(contact.id);
          }
          this.showDetailModal = true;
        },
        error: (error) => {
          console.error('Erreur lors du chargement du fournisseur:', error);
          // Utiliser les données du contact si l'API échoue
          this.selectedContact = contact;
          this.showDetailModal = true;
        }
      });
    }
  }

  loadClientFactures(clientId: number) {
    this.isLoadingFactures = true;
    this.facturesService.getFacturesByClientId(clientId).subscribe({
      next: (factures: Facture[]) => {
        this.clientFactures = factures;
        this.isLoadingFactures = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des factures:', error);
        this.clientFactures = [];
        this.isLoadingFactures = false;
      }
    });
  }

  loadClientVoyages(clientId: number) {
    this.isLoadingVoyagesClient = true;
    this.voyagesService.getVoyagesByClient(clientId).subscribe({
      next: (voyages: Voyage[]) => {
        this.clientVoyages = voyages;
        this.isLoadingVoyagesClient = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des voyages:', error);
        this.clientVoyages = [];
        this.isLoadingVoyagesClient = false;
      }
    });
  }

  getFactureStatutPaiement(voyage: Voyage): string {
    if (!voyage.factureStatut) {
      return 'Sans facture';
    }
    if (voyage.factureStatut === 'PAYEE') {
      return 'Payée';
    }
    if (voyage.factureStatut === 'PARTIELLEMENT_PAYEE') {
      return 'Partiellement payée';
    }
    if (voyage.factureStatut === 'EMISE') {
      return 'Impayée';
    }
    return voyage.factureStatut;
  }

  getFactureStatutClass(voyage: Voyage): string {
    if (!voyage.factureStatut) {
      return 'badge-grey';
    }
    if (voyage.factureStatut === 'PAYEE') {
      return 'badge-green';
    }
    if (voyage.factureStatut === 'PARTIELLEMENT_PAYEE') {
      return 'badge-orange';
    }
    if (voyage.factureStatut === 'EMISE') {
      return 'badge-red';
    }
    return 'badge-grey';
  }

  getTotalQuantiteVoyages(): number {
    return this.clientVoyages.reduce((total, voyage) => {
      return total + (voyage.quantite || 0);
    }, 0);
  }

  loadFournisseurCamions(fournisseurId: number) {
    this.isLoadingCamions = true;
    this.camionsService.getCamionsByFournisseur(fournisseurId).subscribe({
      next: (camions: CamionWithVoyagesCount[]) => {
        this.fournisseurCamions = camions;
        this.isLoadingCamions = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des camions:', error);
        this.isLoadingCamions = false;
      }
    });
  }

  viewCamionVoyages(camion: CamionWithVoyagesCount) {
    if (!camion.id) return;
    this.selectedCamion = camion;
    this.isLoadingVoyages = true;
    this.voyagesService.getVoyagesByCamion(camion.id).subscribe({
      next: (voyages: Voyage[]) => {
        this.camionVoyages = voyages;
        this.isLoadingVoyages = false;
        this.showCamionVoyagesModal = true;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des voyages:', error);
        this.isLoadingVoyages = false;
        this.toastService.error('Erreur lors du chargement des voyages');
      }
    });
  }

  closeCamionVoyagesModal() {
    this.showCamionVoyagesModal = false;
    this.selectedCamion = null;
    this.camionVoyages = [];
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedContact = null;
    this.selectedClient = null;
    this.selectedFournisseur = null;
    this.clientFactures = [];
    this.clientVoyages = [];
    this.fournisseurCamions = [];
  }

  getStatutLabel(statut: string): string {
    const labels: { [key: string]: string } = {
      'BROUILLON': 'Brouillon',
      'EMISE': 'Émise',
      'PAYEE': 'Payée',
      'PARTIELLEMENT_PAYEE': 'Partiellement payée',
      'ANNULEE': 'Annulée',
      'EN_RETARD': 'En retard'
    };
    return labels[statut] || statut;
  }

  getStatutClass(statut: string): string {
    const classes: { [key: string]: string } = {
      'BROUILLON': 'badge-gray',
      'EMISE': 'badge-blue',
      'PAYEE': 'badge-green',
      'PARTIELLEMENT_PAYEE': 'badge-orange',
      'ANNULEE': 'badge-red',
      'EN_RETARD': 'badge-red'
    };
    return classes[statut] || 'badge-gray';
  }

  exportFacturesPdf() {
    if (!this.selectedClient || !this.selectedClient.id) {
      this.toastService.warning('Aucun client sélectionné');
      return;
    }

    this.isExportingFactures = true;
    const clientId = this.selectedClient.id;

    this.facturesService.exportFacturesPdf(clientId).subscribe({
      next: (blob: Blob) => {
        // Créer un lien de téléchargement
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `factures_client_${this.selectedClient?.nom || clientId}_${new Date().toISOString().split('T')[0]}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        this.isExportingFactures = false;
        this.toastService.success('PDF exporté avec succès');
      },
      error: (error) => {
        console.error('Erreur lors de l\'export PDF:', error);
        this.isExportingFactures = false;
        this.toastService.error('Erreur lors de l\'export du PDF');
      }
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return dateString;
      }
      return date.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
      });
    } catch (e) {
      return dateString;
    }
  }

  editContact(contact: Contact) {
    this.selectedContact = contact;
    // Récupérer les données complètes depuis l'API pour l'édition
    if (contact.type === 'client' && contact.id) {
      this.clientsService.getClientById(contact.id).subscribe({
        next: (client: Client) => {
          this.selectedClient = client;
          this.editClient = {
            nom: client.nom,
            email: client.email,
            telephone: client.telephone,
            adresse: client.adresse,
            type: client.type,
            ville: client.ville,
            pays: client.pays
          };
          this.showEditModal = true;
        },
        error: (error) => {
          console.error('Erreur lors du chargement du client:', error);
          this.toastService.error('Erreur lors du chargement des données du client');
        }
      });
    } else if (contact.type === 'fournisseur' && contact.id) {
      this.fournisseursService.getFournisseurById(contact.id).subscribe({
        next: (fournisseur: Fournisseur) => {
          this.selectedFournisseur = fournisseur;
          this.editFournisseur = {
            nom: fournisseur.nom,
            email: fournisseur.email,
            telephone: fournisseur.telephone,
            adresse: fournisseur.adresse,
            ville: fournisseur.ville,
            pays: fournisseur.pays,
            contactPersonne: fournisseur.contactPersonne
          };
          this.showEditModal = true;
        },
        error: (error) => {
          console.error('Erreur lors du chargement du fournisseur:', error);
          this.toastService.error('Erreur lors du chargement des données du fournisseur');
        }
      });
    }
  }

  closeEditModal() {
    this.showEditModal = false;
    this.selectedContact = null;
    this.selectedClient = null;
    this.selectedFournisseur = null;
    this.editClient = {};
    this.editFournisseur = {};
  }

  updateClient() {
    if (!this.validateEditClient()) {
      return;
    }

    if (!this.selectedClient || !this.selectedClient.id) {
      this.toastService.error('Erreur: ID du client manquant');
      return;
    }

    this.isLoading = true;

    const clientToUpdate: Client = {
      id: this.selectedClient.id,
      nom: this.editClient.nom!.trim(),
      email: this.editClient.email!.trim(),
      telephone: this.editClient.telephone!.trim(),
      adresse: this.editClient.adresse!.trim(),
      type: this.editClient.type!,
      ville: this.editClient.ville?.trim() || undefined,
      pays: this.editClient.pays?.trim() || undefined,
      codeClient: this.selectedClient.codeClient
    };

    this.clientsService.updateClient(this.selectedClient.id, clientToUpdate).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Client mis à jour avec succès!');
        this.closeEditModal();
        this.loadClients();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du client:', error);
        this.isLoading = false;
        const errorMessage = error.error?.message || error.message || 'Erreur lors de la mise à jour du client';
        this.toastService.error(errorMessage);
      }
    });
  }

  updateFournisseur() {
    if (!this.validateEditFournisseur()) {
      return;
    }

    if (!this.selectedFournisseur || !this.selectedFournisseur.id) {
      this.toastService.error('Erreur: ID du fournisseur manquant');
      return;
    }

    this.isLoading = true;

    const fournisseurToUpdate: Fournisseur = {
      id: this.selectedFournisseur.id,
      nom: this.editFournisseur.nom!.trim(),
      email: this.editFournisseur.email!.trim(),
      telephone: this.editFournisseur.telephone!.trim(),
      adresse: this.editFournisseur.adresse!.trim(),
      ville: this.editFournisseur.ville?.trim() || undefined,
      pays: this.editFournisseur.pays?.trim() || undefined,
      contactPersonne: this.editFournisseur.contactPersonne?.trim() || undefined,
      codeFournisseur: this.selectedFournisseur.codeFournisseur
    };

    this.fournisseursService.updateFournisseur(this.selectedFournisseur.id, fournisseurToUpdate).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Fournisseur mis à jour avec succès!');
        this.closeEditModal();
        this.loadFournisseurs();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du fournisseur:', error);
        this.isLoading = false;
        const errorMessage = error.error?.message || error.message || 'Erreur lors de la mise à jour du fournisseur';
        this.toastService.error(errorMessage);
      }
    });
  }

  validateEditClient(): boolean {
    if (!this.editClient.nom || this.editClient.nom.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom du client');
      return false;
    }
    if (!this.editClient.email || this.editClient.email.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'email du client');
      return false;
    }
    if (!this.editClient.telephone || this.editClient.telephone.trim() === '') {
      this.toastService.warning('Veuillez saisir le téléphone du client');
      return false;
    }
    if (!this.editClient.adresse || this.editClient.adresse.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'adresse du client');
      return false;
    }
    return true;
  }

  validateEditFournisseur(): boolean {
    if (!this.editFournisseur.nom || this.editFournisseur.nom.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom du fournisseur');
      return false;
    }
    if (!this.editFournisseur.email || this.editFournisseur.email.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'email du fournisseur');
      return false;
    }
    if (!this.editFournisseur.telephone || this.editFournisseur.telephone.trim() === '') {
      this.toastService.warning('Veuillez saisir le téléphone du fournisseur');
      return false;
    }
    if (!this.editFournisseur.adresse || this.editFournisseur.adresse.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'adresse du fournisseur');
      return false;
    }
    return true;
  }

  deleteContact(contact: Contact) {
    this.alertService.confirm(
      `Êtes-vous sûr de vouloir supprimer ${contact.nom}?`,
      'Confirmation de suppression'
    ).subscribe(confirmed => {
      if (!confirmed) return;

      if (contact.type === 'client' && contact.id) {
        this.clientsService.deleteClient(contact.id).subscribe({
          next: () => {
            this.toastService.success('Client supprimé avec succès!');
            this.loadClients();
          },
          error: (error) => {
            console.error('Erreur lors de la suppression du client:', error);
            const errorMessage = error.error?.message || error.message || 'Erreur lors de la suppression du client';
            this.toastService.error(errorMessage);
          }
        });
      } else if (contact.type === 'fournisseur' && contact.id) {
        this.fournisseursService.deleteFournisseur(contact.id).subscribe({
          next: () => {
            this.toastService.success('Fournisseur supprimé avec succès!');
            this.loadFournisseurs();
          },
          error: (error) => {
            console.error('Erreur lors de la suppression du fournisseur:', error);
            const errorMessage = error.error?.message || error.message || 'Erreur lors de la suppression du fournisseur';
            this.toastService.error(errorMessage);
          }
        });
      }
    });
  }
}
