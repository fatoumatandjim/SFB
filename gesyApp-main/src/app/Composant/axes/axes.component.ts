import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AxesService, Axe } from '../../services/axes.service';
import { PaysService, Pays, HistoriquePays } from '../../services/pays.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { AlertService } from '../../nativeComp/alert/alert.service';

@Component({
  selector: 'app-axes',
  templateUrl: './axes.component.html',
  styleUrls: ['./axes.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class AxesComponent implements OnInit {
  activeTab: 'pays' | 'axes' = 'pays';

  // Pays
  paysList: Pays[] = [];
  isLoadingPays = false;
  showPaysModal = false;
  isEditingPays = false;
  editingPays: Pays | null = null;
  formPaysNom = '';
  formPaysFraisParLitre = 0;
  formPaysFraisParLitreGasoil = 0;
  formPaysFraisT1 = 0;
  isSavingPays = false;

  // Historique
  showHistoriqueForPaysId: number | null = null;
  historiquePays: HistoriquePays[] = [];
  isLoadingHistorique = false;

  // Axes
  axes: Axe[] = [];
  isLoadingAxes = false;
  searchTerm = '';
  showAxeModal = false;
  isEditingAxe = false;
  editingAxe: Axe | null = null;
  formNom = '';
  formPaysId: number | undefined = undefined;
  isSavingAxe = false;

  constructor(
    private axesService: AxesService,
    private paysService: PaysService,
    private toastService: ToastService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadPays();
    this.loadAxes();
  }

  // --- PAYS ---

  loadPays() {
    this.isLoadingPays = true;
    this.paysService.getAll().subscribe({
      next: (data) => { this.paysList = data; this.isLoadingPays = false; },
      error: () => { this.isLoadingPays = false; this.toastService.error('Erreur lors du chargement des pays'); }
    });
  }

  openAddPays() {
    this.isEditingPays = false;
    this.editingPays = null;
    this.formPaysNom = '';
    this.formPaysFraisParLitre = 0;
    this.formPaysFraisParLitreGasoil = 0;
    this.formPaysFraisT1 = 0;
    this.showPaysModal = true;
  }

  openEditPays(pays: Pays) {
    this.isEditingPays = true;
    this.editingPays = pays;
    this.formPaysNom = pays.nom;
    this.formPaysFraisParLitre = pays.fraisParLitre ?? 0;
    this.formPaysFraisParLitreGasoil = pays.fraisParLitreGasoil ?? 0;
    this.formPaysFraisT1 = pays.fraisT1 ?? 0;
    this.showPaysModal = true;
  }

  closePaysModal() {
    this.showPaysModal = false;
    this.editingPays = null;
  }

  savePays() {
    const payload: Partial<Pays> = {
      nom: this.formPaysNom?.trim(),
      fraisParLitre: this.formPaysFraisParLitre,
      fraisParLitreGasoil: this.formPaysFraisParLitreGasoil,
      fraisT1: this.formPaysFraisT1
    };

    if (this.isEditingPays && this.editingPays) {
      this.isSavingPays = true;
      this.paysService.update(this.editingPays.id, payload).subscribe({
        next: () => {
          this.loadPays();
          this.closePaysModal();
          this.isSavingPays = false;
          this.toastService.success('Pays mis à jour');
          if (this.showHistoriqueForPaysId === this.editingPays!.id) {
            this.loadHistorique(this.editingPays!.id);
          }
        },
        error: (err) => { this.isSavingPays = false; this.toastService.error(err?.error?.message || 'Erreur lors de la mise à jour'); }
      });
    } else {
      if (!payload.nom) { this.toastService.error('Veuillez saisir le nom du pays'); return; }
      this.isSavingPays = true;
      this.paysService.create(payload).subscribe({
        next: () => {
          this.loadPays();
          this.closePaysModal();
          this.isSavingPays = false;
          this.toastService.success('Pays créé');
        },
        error: (err) => { this.isSavingPays = false; this.toastService.error(err?.error?.message || 'Un pays avec ce nom existe déjà.'); }
      });
    }
  }

  deletePays(pays: Pays) {
    this.alertService.confirm(
      `Supprimer le pays « ${pays.nom} » et ses frais ? Les axes liés perdront leur pays.`,
      'Confirmer la suppression'
    ).subscribe(confirmed => {
      if (!confirmed) return;
      this.paysService.delete(pays.id).subscribe({
        next: () => {
          this.loadPays();
          this.loadAxes();
          this.toastService.success('Pays supprimé');
        },
        error: (err) => this.toastService.error(err?.error?.message || 'Erreur lors de la suppression')
      });
    });
  }

  toggleHistorique(pays: Pays) {
    if (this.showHistoriqueForPaysId === pays.id) {
      this.showHistoriqueForPaysId = null;
      this.historiquePays = [];
      return;
    }
    this.showHistoriqueForPaysId = pays.id;
    this.loadHistorique(pays.id);
  }

  loadHistorique(paysId: number) {
    this.isLoadingHistorique = true;
    this.paysService.getHistorique(paysId).subscribe({
      next: (data) => { this.historiquePays = data; this.isLoadingHistorique = false; },
      error: () => { this.isLoadingHistorique = false; }
    });
  }

  // --- AXES ---

  loadAxes() {
    this.isLoadingAxes = true;
    this.axesService.getAllAxes().subscribe({
      next: (data) => { this.axes = data; this.isLoadingAxes = false; },
      error: () => { this.isLoadingAxes = false; this.toastService.error('Erreur lors du chargement des axes'); }
    });
  }

  get filteredAxes(): Axe[] {
    if (!this.searchTerm.trim()) return this.axes;
    const term = this.searchTerm.toLowerCase();
    return this.axes.filter(a =>
      a.nom.toLowerCase().includes(term) ||
      (a.paysNom || '').toLowerCase().includes(term)
    );
  }

  openAddAxe() {
    this.isEditingAxe = false;
    this.editingAxe = null;
    this.formNom = '';
    this.formPaysId = undefined;
    this.showAxeModal = true;
  }

  openEditAxe(axe: Axe) {
    this.isEditingAxe = true;
    this.editingAxe = axe;
    this.formNom = axe.nom;
    this.formPaysId = axe.paysId;
    this.showAxeModal = true;
  }

  closeAxeModal() {
    this.showAxeModal = false;
    this.editingAxe = null;
  }

  saveAxe() {
    const nom = this.formNom?.trim();
    if (!nom) { this.toastService.error('Veuillez saisir le nom de l\'axe'); return; }
    this.isSavingAxe = true;
    const payload = { nom, paysId: this.formPaysId };

    if (this.isEditingAxe && this.editingAxe) {
      this.axesService.updateAxe(this.editingAxe.id, payload).subscribe({
        next: () => { this.loadAxes(); this.closeAxeModal(); this.isSavingAxe = false; this.toastService.success('Axe mis à jour'); },
        error: (err) => { this.isSavingAxe = false; this.toastService.error(err?.error?.message || 'Erreur lors de la mise à jour'); }
      });
    } else {
      this.axesService.createAxe(payload).subscribe({
        next: () => { this.loadAxes(); this.closeAxeModal(); this.isSavingAxe = false; this.toastService.success('Axe créé'); },
        error: (err) => { this.isSavingAxe = false; this.toastService.error(err?.error?.message || 'Un axe avec ce nom existe déjà.'); }
      });
    }
  }

  deleteAxe(axe: Axe) {
    this.alertService.confirm(
      `Supprimer l'axe « ${axe.nom} » ? Cette action est irréversible.`,
      'Confirmer la suppression'
    ).subscribe(confirmed => {
      if (!confirmed) return;
      this.axesService.deleteAxe(axe.id).subscribe({
        next: () => { this.loadAxes(); this.toastService.success('Axe supprimé'); },
        error: (err) => this.toastService.error(err?.error?.message || 'Erreur lors de la suppression')
      });
    });
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }
}
