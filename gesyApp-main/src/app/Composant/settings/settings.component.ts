import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UtilisateursService, Utilisateur, Role as RoleUtilisateur } from '../../services/utilisateurs.service';
import { RolesService, Role } from '../../services/roles.service';
import { DouaneService, Douane } from '../../services/douane.service';
import { DepotsService, Depot } from '../../services/depots.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';

interface Employe {
  id: number;
  identifiant?: string;
  nom: string;
  email: string;
  telephone: string;
  roles: Role[];
  actif: boolean;
  statut?: string;
  defaultPass?: string;
}

interface NewEmploye {
  nom: string;
  email: string;
  telephone: string;
  rolesIds: number[];
  actif: boolean;
  depotId?: number;
}

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class SettingsComponent implements OnInit {
  employes: Employe[] = [];
  isLoading: boolean = false;
  isLoadingRoles: boolean = false;
  showModal: boolean = false;
  isEditing: boolean = false;
  rolesDisponibles: Role[] = [];
  showPasswords: { [key: number]: boolean } = {};
  depots: Depot[] = [];
  isLoadingDepots: boolean = false;

  newEmploye: NewEmploye = {
    nom: '',
    email: '',
    telephone: '',
    rolesIds: [],
    actif: true,
  };

  currentPassword: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  editingEmployeId: number | undefined;

  // Frais Douane
  douane: Douane | null = null;
  isLoadingDouane: boolean = false;
  isEditingDouane: boolean = false;
  douaneForm: { fraisParLitre: number; fraisParLitreGasoil: number; fraisT1: number } = {
    fraisParLitre: 0,
    fraisParLitreGasoil: 0,
    fraisT1: 0
  };

  constructor(
    private utilisateursService: UtilisateursService,
    private rolesService: RolesService,
    private douaneService: DouaneService,
    private depotsService: DepotsService,
    private alertService: AlertService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.loadEmployes();
    this.loadRoles();
    this.loadDouane();
    this.loadDepots();
  }

  loadEmployes() {
    this.isLoading = true;
    this.utilisateursService.getAllUtilisateurs().subscribe({
      next: (data: Utilisateur[]) => {
        this.employes = data.map(u => ({
          id: u.id!,
          identifiant: u.identifiant,
          nom: u.nom,
          email: u.email,
          telephone: u.telephone,
          roles: u.roles || [],
          actif: u.actif ?? true,
          statut: u.statut,
          defaultPass: u.defaultPass,
          depotId: u.depotId
        }));
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des employés:', error);
        this.isLoading = false;
      }
    });
  }

  loadRoles() {
    this.isLoadingRoles = true;
    this.rolesService.getAllRoles().subscribe({
      next: (data: Role[]) => {
        // Filtrer uniquement les rôles actifs
        this.rolesDisponibles = data.filter(r => r.statut === 'ACTIF');
        this.isLoadingRoles = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des rôles:', error);
        this.isLoadingRoles = false;
      }
    });
  }

  loadDepots() {
    this.isLoadingDepots = true;
    this.depotsService.getAllDepots().subscribe({
      next: (data: Depot[]) => {
        this.depots = data.filter(d => d.statut === 'ACTIF');
        this.isLoadingDepots = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des dépôts:', error);
        this.isLoadingDepots = false;
      }
    });
  }

  ouvrirModalAjout() {
    this.isEditing = false;
    this.newEmploye = {
      nom: '',
      email: '',
      telephone: '',
      rolesIds: [],
      actif: true,
      depotId: undefined,
    };
    this.showModal = true;
  }

  editerEmploye(employe: Employe) {
    this.isEditing = true;
    this.editingEmployeId = employe.id;
    this.newEmploye = {
      nom: employe.nom,
      email: employe.email,
      telephone: employe.telephone,
      rolesIds: employe.roles.map(r => r.id),
      actif: employe.actif,
      depotId: (employe as any).depotId,
    };
    this.showModal = true;
  }

  fermerModal() {
    this.showModal = false;
    this.isEditing = false;
    this.editingEmployeId = undefined;
    this.newEmploye = {
      nom: '',
      email: '',
      telephone: '',
      rolesIds: [],
      actif: true,
      depotId: undefined,
    };
  }

  toggleRole(roleId: number) {
    const index = this.newEmploye.rolesIds.indexOf(roleId);
    if (index > -1) {
      this.newEmploye.rolesIds.splice(index, 1);
    } else {
      this.newEmploye.rolesIds.push(roleId);
    }
  }

  isRoleSelected(roleId: number): boolean {
    return this.newEmploye.rolesIds.includes(roleId);
  }

  isFormValid(): boolean {
    const hasRequiredFields = !!(
      this.newEmploye.nom &&
      this.newEmploye.email &&
      this.newEmploye.telephone &&
      this.newEmploye.rolesIds.length > 0
    );

    // Si le rôle "charger depot" est sélectionné, vérifier qu'un dépôt est sélectionné
    const hasChargerDepotRole = this.hasChargerDepotRole();
    if (hasChargerDepotRole && !this.newEmploye.depotId) {
      return false;
    }

    return hasRequiredFields;
  }

  hasChargerDepotRole(): boolean {
    return this.newEmploye.rolesIds.some(roleId => {
      const role = this.rolesDisponibles.find(r => r.id === roleId);
      return role && (role.nom.toLowerCase().includes('charger') && role.nom.toLowerCase().includes('depot'));
    });
  }

  sauvegarderEmploye() {
    if (!this.isFormValid()) {
      if (this.hasChargerDepotRole() && !this.newEmploye.depotId) {
        this.toastService.warning('Veuillez sélectionner un dépôt pour le rôle "charger depot"');
      } else {
        this.toastService.warning('Veuillez remplir tous les champs obligatoires et sélectionner au moins un rôle');
      }
      return;
    }

    // Préparer les rôles pour l'envoi au backend (le backend accepte les rôles avec ID ou nom)
    const rolesToSend = this.newEmploye.rolesIds.map(roleId => {
      const role = this.rolesDisponibles.find(r => r.id === roleId);
      // Le backend peut récupérer le rôle par ID ou par nom
      return role ? { id: role.id } : null;
    }).filter(r => r !== null) as any[];

    const utilisateurData: any = {
      nom: this.newEmploye.nom,
      email: this.newEmploye.email,
      telephone: this.newEmploye.telephone,
      roles: rolesToSend,
      actif: this.newEmploye.actif,
      statut: this.newEmploye.actif ? 'ACTIF' : 'INACTIF'
    };

    // Ajouter le dépôt si le rôle "charger depot" est sélectionné
    if (this.hasChargerDepotRole() && this.newEmploye.depotId) {
      utilisateurData.depot = { id: this.newEmploye.depotId };
    }

    if (this.isEditing && this.editingEmployeId) {
      // Mettre à jour l'employé existant
      this.utilisateursService.updateUtilisateur(this.editingEmployeId, utilisateurData).subscribe({
        next: (updated: Utilisateur) => {
          this.loadEmployes(); // Recharger la liste
          this.fermerModal();
          this.toastService.success('Employé modifié avec succès');
        },
        error: (error) => {
          console.error('Erreur lors de la modification:', error);
          this.toastService.error('Erreur lors de la modification de l\'employé');
        }
      });
    } else {
      // Créer un nouvel employé
      this.utilisateursService.createUtilisateur(utilisateurData).subscribe({
        next: (created: Utilisateur) => {
          let message = `Employé créé avec succès!\n`;
          if (created.identifiant) {
            message += `Identifiant: ${created.identifiant}\n`;
          }
          if (created.defaultPass) {
            message += `Mot de passe: ${created.defaultPass}`;
          }
          this.alertService.success(message, 'Employé créé').subscribe();
          this.loadEmployes(); // Recharger la liste
          this.fermerModal();
        },
        error: (error) => {
          console.error('Erreur lors de la création:', error);
          const errorMessage = error.error?.message || error.message || 'Erreur lors de la création de l\'employé';
          this.toastService.error(errorMessage);
        }
      });
    }
  }

  supprimerEmploye(employe: Employe) {
    this.alertService.confirm(
      `Êtes-vous sûr de vouloir supprimer ${employe.nom} ?`,
      'Confirmation de suppression'
    ).subscribe(confirmed => {
      if (!confirmed) return;
      this.utilisateursService.deleteUtilisateur(employe.id).subscribe({
        next: () => {
          this.loadEmployes(); // Recharger la liste
          this.toastService.success('Employé supprimé avec succès');
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          this.toastService.error('Erreur lors de la suppression de l\'employé');
        }
      });
    });
  }

  togglePassword(employeId: number) {
    this.showPasswords[employeId] = !this.showPasswords[employeId];
  }

  toggleActif(employe: Employe) {
    const newActif = !employe.actif;
    // Préparer les rôles avec leurs IDs pour la mise à jour
    const rolesToSend = employe.roles.map(r => ({ id: r.id }));

    const utilisateurData: any = {
      id: employe.id,
      nom: employe.nom,
      email: employe.email,
      telephone: employe.telephone,
      roles: rolesToSend,
      actif: newActif,
      statut: newActif ? 'ACTIF' : 'INACTIF'
    };

    // Conserver le dépôt existant si présent
    if ((employe as any).depotId) {
      utilisateurData.depot = { id: (employe as any).depotId };
    }

    this.utilisateursService.updateUtilisateur(employe.id, utilisateurData).subscribe({
      next: () => {
        employe.actif = newActif;
        employe.statut = newActif ? 'ACTIF' : 'INACTIF';
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du statut:', error);
        this.toastService.error('Erreur lors de la mise à jour du statut');
        // Recharger pour restaurer l'état précédent
        this.loadEmployes();
      }
    });
  }

  changerMotDePasse() {
    if (!this.currentPassword || !this.newPassword || this.newPassword !== this.confirmPassword) {
      this.toastService.warning('Veuillez vérifier vos mots de passe');
      return;
    }

    // TODO: Appeler le service pour changer le mot de passe
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.toastService.success('Mot de passe changé avec succès');
  }

  // Méthodes pour gérer les frais douane
  loadDouane() {
    this.isLoadingDouane = true;
    this.douaneService.getDouane().subscribe({
      next: (douane: Douane) => {
        this.douane = douane;
        this.douaneForm = {
          fraisParLitre: douane.fraisParLitre,
          fraisT1: douane.fraisT1,
          fraisParLitreGasoil: douane.fraisParLitreGasoil
        };
        this.isLoadingDouane = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des frais douane:', error);
        this.isLoadingDouane = false;
        this.toastService.error('Erreur lors du chargement des frais douane');
      }
    });
  }

  editDouane() {
    this.isEditingDouane = true;
    if (this.douane) {
      this.douaneForm = {
        fraisParLitre: this.douane.fraisParLitre,
        fraisParLitreGasoil: this.douane.fraisParLitreGasoil,
        fraisT1: this.douane.fraisT1
      };
    }
  }

  cancelEditDouane() {
    this.isEditingDouane = false;
    if (this.douane) {
      this.douaneForm = {
        fraisParLitre: this.douane.fraisParLitre,
        fraisParLitreGasoil: this.douane.fraisParLitreGasoil,
        fraisT1: this.douane.fraisT1
      };
    }
  }

  saveDouane() {
    if (!this.douaneForm.fraisParLitre || !this.douaneForm.fraisParLitreGasoil || !this.douaneForm.fraisT1) {
      this.toastService.warning('Veuillez remplir tous les champs');
      return;
    }

    if (this.douaneForm.fraisParLitre <= 0 || this.douaneForm.fraisParLitreGasoil <= 0 || this.douaneForm.fraisT1 <= 0) {
      this.toastService.warning('Les frais doivent être supérieurs à 0');
      return;
    }

    const douaneToUpdate: Douane = {
      id: this.douane?.id,
      fraisParLitre: this.douaneForm.fraisParLitre,
      fraisParLitreGasoil: this.douaneForm.fraisParLitreGasoil,
      fraisT1: this.douaneForm.fraisT1
    };

    this.douaneService.updateDouane(douaneToUpdate).subscribe({
      next: (updated: Douane) => {
        this.douane = updated;
        this.isEditingDouane = false;
        this.toastService.success('Frais douane mis à jour avec succès');
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour des frais douane:', error);
        this.toastService.error('Erreur lors de la mise à jour des frais douane');
      }
    });
  }
}
