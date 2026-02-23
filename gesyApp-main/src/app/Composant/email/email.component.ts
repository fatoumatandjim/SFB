import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmailService, Email } from '../../services/email.service';
import { ToastService } from '../../nativeComp/toast/toast.service';

@Component({
  selector: 'app-email',
  templateUrl: './email.component.html',
  styleUrls: ['./email.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class EmailComponent implements OnInit {
  emails: Email[] = [];
  selectedEmail: Email | null = null;
  searchQuery: string = '';
  showCompose: boolean = false;
  isLoading: boolean = false;
  isSending: boolean = false;

  // Nouveau message
  newEmail = {
    to: '',
    cc: '',
    bcc: '',
    subject: '',
    content: '',
    attachments: [] as string[]
  };

  // Gestion des fichiers
  selectedFiles: File[] = [];
  uploadedFiles: any[] = [];
  isUploading: boolean = false;

  constructor(
    private emailService: EmailService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.loadEmails();
  }

  loadEmails() {
    this.isLoading = true;
    // Charger uniquement les messages envoyés
    this.emailService.getEmailsByFolder('sent').subscribe({
      next: (data) => {
        this.emails = data.map(email => ({
          ...email,
          date: new Date(email.date)
        }));
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des emails:', error);
        this.toastService.error('Erreur lors du chargement des emails');
        this.isLoading = false;
      }
    });
  }

  get filteredEmails(): Email[] {
    if (!this.searchQuery) return this.emails;

    const query = this.searchQuery.toLowerCase();
    return this.emails.filter(e =>
      e.subject.toLowerCase().includes(query) ||
      e.toEmail.toLowerCase().includes(query) ||
      (e.preview?.toLowerCase().includes(query) || false)
    );
  }

  getInitials(email: string): string {
    if (!email) return '?';
    return email.charAt(0).toUpperCase();
  }

  getOriginalFileName(fileName: string): string {
    if (fileName.includes('_')) {
      return fileName.substring(fileName.indexOf('_') + 1);
    }
    return fileName;
  }

  selectEmail(email: Email) {
    this.selectedEmail = email;
  }

  toggleStar(email: Email, event: Event) {
    event.stopPropagation();
    if (!email.id) return;

    this.emailService.toggleStar(email.id).subscribe({
      next: () => {
        email.starred = !email.starred;
      },
      error: (error) => {
        console.error('Erreur lors du marquage favori:', error);
      }
    });
  }

  deleteEmail(email: Email) {
    if (!email.id) return;

    this.emailService.deleteEmail(email.id).subscribe({
      next: () => {
        this.toastService.success('Message supprimé');
        this.loadEmails();
        if (this.selectedEmail?.id === email.id) {
          this.selectedEmail = null;
        }
      },
      error: (error) => {
        console.error('Erreur lors de la suppression:', error);
        this.toastService.error('Erreur lors de la suppression');
      }
    });
  }

  openCompose() {
    this.showCompose = true;
    this.newEmail = { to: '', cc: '', bcc: '', subject: '', content: '', attachments: [] };
    this.selectedFiles = [];
    this.uploadedFiles = [];
  }

  closeCompose() {
    this.showCompose = false;
    this.selectedFiles = [];
    this.uploadedFiles = [];
  }

  onFileSelected(event: any) {
    const files: FileList = event.target.files;
    if (files && files.length > 0) {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        // Vérifier la taille (10MB max par fichier)
        if (file.size > 10 * 1024 * 1024) {
          this.toastService.error(`Le fichier ${file.name} est trop volumineux (max 10MB)`);
          continue;
        }
        this.selectedFiles.push(file);
      }
      this.uploadFiles();
    }
  }

  uploadFiles() {
    if (this.selectedFiles.length === 0) return;

    this.isUploading = true;
    this.emailService.uploadMultipleFiles(this.selectedFiles).subscribe({
      next: (responses) => {
        responses.forEach((response: any) => {
          if (response.fileName) {
            this.uploadedFiles.push(response);
            this.newEmail.attachments.push(response.fileName);
            this.toastService.success(`Fichier ${response.originalName} uploadé`);
          } else if (response.error) {
            this.toastService.error(`Erreur: ${response.originalName}`);
          }
        });
        this.selectedFiles = [];
        this.isUploading = false;
      },
      error: (error) => {
        console.error('Erreur lors de l\'upload:', error);
        this.toastService.error('Erreur lors de l\'upload des fichiers');
        this.isUploading = false;
      }
    });
  }

  removeAttachment(index: number) {
    this.uploadedFiles.splice(index, 1);
    this.newEmail.attachments.splice(index, 1);
  }

  getFileDownloadUrl(fileName: string): string {
    return this.emailService.downloadFile(fileName);
  }

  downloadFile(fileName: string) {
    const originalFileName = this.getOriginalFileName(fileName);
    this.emailService.downloadFileAsBlob(fileName).subscribe({
      next: (blob: Blob) => {
        // Créer un lien de téléchargement
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = originalFileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        this.toastService.success(`Téléchargement de ${originalFileName} réussi`);
      },
      error: (error) => {
        console.error('Erreur lors du téléchargement:', error);
        this.toastService.error('Erreur lors du téléchargement du fichier');
      }
    });
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  sendEmail() {
    if (!this.newEmail.to || !this.newEmail.subject) {
      this.toastService.error('Veuillez renseigner le destinataire et l\'objet');
      return;
    }

    this.isSending = true;
    this.emailService.sendEmail({
      toEmail: this.newEmail.to,
      cc: this.newEmail.cc?.trim() || undefined,
      bcc: this.newEmail.bcc?.trim() || undefined,
      subject: this.newEmail.subject,
      content: this.newEmail.content,
      attachments: this.newEmail.attachments,
      fromEmail: '',
      from: '',
      read: true,
      starred: false,
      folder: 'sent',
      date: new Date()
    }).subscribe({
      next: () => {
        const attachmentCount = this.newEmail.attachments.length;
        const message = attachmentCount > 0
          ? `Email envoyé avec ${attachmentCount} pièce(s) jointe(s)`
          : 'Email envoyé avec succès';
        this.toastService.success(message);
        this.closeCompose();
        this.loadEmails();
        this.isSending = false;
      },
      error: (error) => {
        console.error('Erreur lors de l\'envoi de l\'email:', error);
        this.toastService.error('Erreur lors de l\'envoi de l\'email');
        this.isSending = false;
      }
    });
  }

  formatDate(date: Date | string): string {
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    const now = new Date();
    const diff = now.getTime() - dateObj.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) {
      return dateObj.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    } else if (days === 1) {
      return 'Hier';
    } else if (days < 7) {
      return dateObj.toLocaleDateString('fr-FR', { weekday: 'long' });
    } else {
      return dateObj.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
    }
  }

  backToList() {
    this.selectedEmail = null;
  }
}
