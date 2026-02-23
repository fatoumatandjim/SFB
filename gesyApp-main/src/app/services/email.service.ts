import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Email {
  id?: number;
  from: string;
  fromEmail: string;
  toEmail: string;
  subject: string;
  preview?: string;
  content: string;
  date: Date | string;
  read: boolean;
  starred: boolean;
  folder: 'inbox' | 'sent' | 'draft' | 'trash';
  attachments?: string[];
  messageId?: string;
  inReplyTo?: string;
  cc?: string;
  bcc?: string;
}

export interface FolderCounts {
  inbox: number;
  sent: number;
  draft: number;
  trash: number;
}

@Injectable({
  providedIn: 'root'
})
export class EmailService {
  private apiUrl = `${environment.apiUrl}/emails`;

  constructor(private http: HttpClient) {}

  // CRUD de base
  getAllEmails(): Observable<Email[]> {
    return this.http.get<Email[]>(this.apiUrl);
  }

  getEmailById(id: number): Observable<Email> {
    return this.http.get<Email>(`${this.apiUrl}/${id}`);
  }

  createEmail(email: Email): Observable<Email> {
    return this.http.post<Email>(this.apiUrl, email);
  }

  updateEmail(id: number, email: Email): Observable<Email> {
    return this.http.put<Email>(`${this.apiUrl}/${id}`, email);
  }

  deleteEmail(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Gestion des dossiers
  getEmailsByFolder(folder: string): Observable<Email[]> {
    return this.http.get<Email[]>(`${this.apiUrl}/folder/${folder}`);
  }

  moveToFolder(id: number, folder: string): Observable<Email> {
    return this.http.put<Email>(`${this.apiUrl}/${id}/move/${folder}`, {});
  }

  getUnreadByFolder(folder: string): Observable<Email[]> {
    return this.http.get<Email[]>(`${this.apiUrl}/folder/${folder}/unread`);
  }

  getStarredByFolder(folder: string): Observable<Email[]> {
    return this.http.get<Email[]>(`${this.apiUrl}/folder/${folder}/starred`);
  }

  countUnreadByFolder(folder: string): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/folder/${folder}/count`);
  }

  // Recherche
  searchInFolder(folder: string, query: string): Observable<Email[]> {
    return this.http.get<Email[]>(`${this.apiUrl}/folder/${folder}/search`, {
      params: { query }
    });
  }

  // Gestion de la lecture
  markAsRead(id: number): Observable<Email> {
    return this.http.put<Email>(`${this.apiUrl}/${id}/read`, {});
  }

  markAsUnread(id: number): Observable<Email> {
    return this.http.put<Email>(`${this.apiUrl}/${id}/unread`, {});
  }

  // Gestion des favoris
  toggleStar(id: number): Observable<Email> {
    return this.http.put<Email>(`${this.apiUrl}/${id}/star`, {});
  }

  // Envoi d'email
  sendEmail(email: Partial<Email>): Observable<Email> {
    return this.http.post<Email>(`${this.apiUrl}/send`, email);
  }

  // Synchronisation
  syncEmails(): Observable<Email[]> {
    return this.http.post<Email[]>(`${this.apiUrl}/sync`, {});
  }

  // Statistiques
  getFolderCounts(): Observable<FolderCounts> {
    return this.http.get<FolderCounts>(`${this.apiUrl}/counts`);
  }

  // Upload de fichiers
  uploadFile(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/upload`, formData);
  }

  uploadMultipleFiles(files: File[]): Observable<any> {
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });
    return this.http.post(`${this.apiUrl}/upload-multiple`, formData);
  }

  downloadFile(fileName: string): string {
    return `${this.apiUrl}/download/${fileName}`;
  }

  downloadFileAsBlob(fileName: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${fileName}`, {
      responseType: 'blob'
    });
  }
}

