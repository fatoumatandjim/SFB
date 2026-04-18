import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  JustificatifFinancier,
  JustificatifsFinanciersService
} from '../../services/justificatifs-financiers.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { AlertService } from '../../nativeComp/alert/alert.service';

@Component({
  selector: 'app-justificatifs-financiers-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './justificatifs-financiers-panel.component.html',
  styleUrls: ['./justificatifs-financiers-panel.component.scss']
})
export class JustificatifsFinanciersPanelComponent implements OnChanges {
  /** DEPENSE | PAIEMENT | TRANSACTION */
  @Input({ required: true }) ownerType!: string;
  @Input({ required: true }) ownerId: number | null | undefined;
  /** Si true : bouton pour déplier et charger (listes avec plusieurs lignes). */
  @Input() lazy = false;
  @Input() title = 'Justificatifs financiers';

  items: JustificatifFinancier[] = [];
  loading = false;
  uploading = false;
  expanded = false;
  private loadedOnce = false;

  constructor(
    private justificatifsService: JustificatifsFinanciersService,
    private toastService: ToastService,
    private alertService: AlertService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['ownerId'] || changes['ownerType']) {
      this.items = [];
      this.loadedOnce = false;
      if (!this.lazy && this.effectiveOwnerId != null) {
        this.load();
      } else if (this.lazy && this.expanded && this.effectiveOwnerId != null) {
        this.load();
      }
    }
  }

  get effectiveOwnerId(): number | null {
    const id = this.ownerId;
    if (id == null || id <= 0) return null;
    return id;
  }

  toggleExpand(): void {
    this.expanded = !this.expanded;
    if (this.expanded && !this.loadedOnce && this.effectiveOwnerId != null) {
      this.load();
    }
  }

  load(): void {
    const oid = this.effectiveOwnerId;
    if (oid == null || !this.ownerType) return;
    this.loading = true;
    this.justificatifsService.list(this.ownerType, oid).subscribe({
      next: (list) => {
        this.items = list;
        this.loading = false;
        this.loadedOnce = true;
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Impossible de charger les justificatifs');
      }
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    input.value = '';
    if (files.length === 0) return;
    const oid = this.effectiveOwnerId;
    if (oid == null) {
      this.toastService.warning('Enregistrez d’abord pour obtenir un identifiant');
      return;
    }
    this.uploading = true;
    this.justificatifsService.upload(this.ownerType, oid, files).subscribe({
      next: (list) => {
        this.items = list;
        this.uploading = false;
        this.loadedOnce = true;
        this.toastService.success('Fichier(s) ajouté(s)');
      },
      error: (err) => {
        this.uploading = false;
        const msg = err?.error?.message || 'Échec de l’envoi';
        this.toastService.error(msg);
      }
    });
  }

  download(j: JustificatifFinancier): void {
    this.justificatifsService.download(j.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = j.originalFilename || `justificatif-${j.id}`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toastService.error('Téléchargement impossible')
    });
  }

  confirmDelete(j: JustificatifFinancier): void {
    this.alertService
      .confirm(`Supprimer « ${j.originalFilename} » ?`, 'Justificatif')
      .subscribe((ok) => {
        if (!ok) return;
        this.justificatifsService.delete(j.id).subscribe({
          next: () => {
            this.items = this.items.filter((x) => x.id !== j.id);
            this.toastService.success('Justificatif supprimé');
          },
          error: () => this.toastService.error('Suppression impossible')
        });
      });
  }
}
