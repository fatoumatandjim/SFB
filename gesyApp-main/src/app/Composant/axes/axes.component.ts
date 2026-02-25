import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AxesService, Axe } from '../../services/axes.service';
import { PaysService, Pays } from '../../services/pays.service';
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
  axes: Axe[] = [];
  paysList: Pays[] = [];
  isLoading = false;
  searchTerm = '';
  showModal = false;
  isEditing = false;
  editingAxe: Axe | null = null;
  formNom = '';
  formPaysId: number | undefined = undefined;
  isSaving = false;

  constructor(
    private axesService: AxesService,
    private paysService: PaysService,
    private toastService: ToastService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadAxes();
    this.loadPays();
  }

  loadAxes() {
    this.isLoading = true;
    this.axesService.getAllAxes().subscribe({
      next: (data) => { this.axes = data; this.isLoading = false; },
      error: () => { this.isLoading = false; this.toastService.error('Erreur lors du chargement des axes'); }
    });
  }

  loadPays() {
    this.paysService.getAll().subscribe({
      next: (data) => { this.paysList = data; },
      error: () => {}
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

  openAdd() {
    this.isEditing = false;
    this.editingAxe = null;
    this.formNom = '';
    this.formPaysId = undefined;
    this.showModal = true;
  }

  openEdit(axe: Axe) {
    this.isEditing = true;
    this.editingAxe = axe;
    this.formNom = axe.nom;
    this.formPaysId = axe.paysId;
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.editingAxe = null;
  }

  save() {
    const nom = this.formNom?.trim();
    if (!nom) {
      this.toastService.error('Veuillez saisir le nom de l\'axe');
      return;
    }
    this.isSaving = true;
    const payload = { nom, paysId: this.formPaysId };

    if (this.isEditing && this.editingAxe) {
      this.axesService.updateAxe(this.editingAxe.id, payload).subscribe({
        next: () => {
          this.loadAxes();
          this.closeModal();
          this.isSaving = false;
          this.toastService.success('Axe mis à jour');
        },
        error: (err) => {
          this.isSaving = false;
          this.toastService.error(err?.error?.message || 'Erreur lors de la mise à jour');
        }
      });
    } else {
      this.axesService.createAxe(payload).subscribe({
        next: () => {
          this.loadAxes();
          this.closeModal();
          this.isSaving = false;
          this.toastService.success('Axe créé');
        },
        error: (err) => {
          this.isSaving = false;
          this.toastService.error(err?.error?.message || 'Un axe avec ce nom existe déjà.');
        }
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
        next: () => {
          this.loadAxes();
          this.toastService.success('Axe supprimé');
        },
        error: (err) => this.toastService.error(err?.error?.message || 'Erreur lors de la suppression')
      });
    });
  }
}
