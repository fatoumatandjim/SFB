import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface VoyagePrixRef {
  id: number;
  numeroVoyage?: string;
  quantite?: number;
  prixUnitaire?: number;
}

@Component({
  selector: 'app-edit-prix-transport-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './edit-prix-transport-modal.component.html',
  styleUrls: ['./edit-prix-transport-modal.component.scss']
})
export class EditPrixTransportModalComponent implements OnChanges, AfterViewInit {
  @Input() voyageRef: VoyagePrixRef | null = null;
  @Input() isLoading = false;
  @Output() save = new EventEmitter<number>();
  @Output() cancel = new EventEmitter<void>();

  @ViewChild('prixInput') prixInputRef?: ElementRef<HTMLInputElement>;

  prixUnitaire = 0;

  ngOnChanges(changes: SimpleChanges): void {
    const refChange = changes['voyageRef'];
    if (!refChange || !this.voyageRef) return;
    const prev = refChange.previousValue as VoyagePrixRef | null;
    const curr = refChange.currentValue as VoyagePrixRef | null;
    if (!prev || prev.id !== curr?.id) {
      this.prixUnitaire = this.voyageRef.prixUnitaire ?? 0;
    }
  }

  ngAfterViewInit(): void {
    // Focus initial sur le champ prix (bonne pratique accessibilité)
    setTimeout(() => this.prixInputRef?.nativeElement?.focus(), 0);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.voyageRef) this.onCancel();
  }

  get displayRef(): string {
    if (!this.voyageRef) return '';
    const num = this.voyageRef.numeroVoyage || 'N/A';
    const qty = this.voyageRef.quantite != null ? this.voyageRef.quantite : 0;
    return `Voyage ${num} - ${qty.toLocaleString('fr-FR')} L`;
  }

  onSave(): void {
    if (this.prixUnitaire <= 0) return;
    this.save.emit(this.prixUnitaire);
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
