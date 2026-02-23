import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CategorieDepense {
  id?: number;
  nom: string;
  description?: string;
  statut?: string;
}

export interface Depense {
  id?: number;
  libelle: string;
  montant: number;
  dateDepense: string;
  categorieId: number;
  categorieNom?: string;
  description?: string;
  reference?: string;
  dateCreation?: string;
  creePar?: string;
  /** Compte bancaire pour déduire le montant (exclusif avec caisseId). */
  compteId?: number;
  /** Caisse pour déduire le montant (exclusif avec compteId). */
  caisseId?: number;
}

export interface DepensePage {
  depenses: Depense[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({
  providedIn: 'root'
})
export class DepensesService {
  private apiUrl = `${environment.apiUrl}/depenses`;
  private categoriesUrl = `${environment.apiUrl}/categories-depenses`;

  constructor(private http: HttpClient) {}

  // === CATEGORIES ===
  getCategories(): Observable<CategorieDepense[]> {
    return this.http.get<CategorieDepense[]>(this.categoriesUrl);
  }

  getCategoriesActives(): Observable<CategorieDepense[]> {
    return this.http.get<CategorieDepense[]>(`${this.categoriesUrl}/actives`);
  }

  getCategorieById(id: number): Observable<CategorieDepense> {
    return this.http.get<CategorieDepense>(`${this.categoriesUrl}/${id}`);
  }

  createCategorie(categorie: CategorieDepense): Observable<CategorieDepense> {
    return this.http.post<CategorieDepense>(this.categoriesUrl, categorie);
  }

  updateCategorie(id: number, categorie: CategorieDepense): Observable<CategorieDepense> {
    return this.http.put<CategorieDepense>(`${this.categoriesUrl}/${id}`, categorie);
  }

  deleteCategorie(id: number): Observable<void> {
    return this.http.delete<void>(`${this.categoriesUrl}/${id}`);
  }

  // === DEPENSES ===
  getDepenses(page: number = 0, size: number = 10): Observable<DepensePage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<DepensePage>(this.apiUrl, { params });
  }

  getAllDepenses(): Observable<Depense[]> {
    return this.http.get<Depense[]>(`${this.apiUrl}/all`);
  }

  getDepenseById(id: number): Observable<Depense> {
    return this.http.get<Depense>(`${this.apiUrl}/${id}`);
  }

  createDepense(depense: Depense): Observable<Depense> {
    return this.http.post<Depense>(this.apiUrl, depense);
  }

  updateDepense(id: number, depense: Depense): Observable<Depense> {
    return this.http.put<Depense>(`${this.apiUrl}/${id}`, depense);
  }

  deleteDepense(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // === FILTRES PAR CATEGORIE ===
  getDepensesByCategorie(categorieId: number, page: number = 0, size: number = 10): Observable<DepensePage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<DepensePage>(`${this.apiUrl}/categorie/${categorieId}`, { params });
  }

  // === FILTRES PAR DATE ===
  getDepensesByDate(date: string, page: number = 0, size: number = 10): Observable<DepensePage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<DepensePage>(`${this.apiUrl}/date/${date}`, { params });
  }

  getDepensesByDateRange(startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<DepensePage> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<DepensePage>(`${this.apiUrl}/date-range`, { params });
  }

  // === FILTRES PAR CATEGORIE ET DATE ===
  getDepensesByCategorieAndDate(categorieId: number, date: string, page: number = 0, size: number = 10): Observable<DepensePage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<DepensePage>(`${this.apiUrl}/categorie/${categorieId}/date/${date}`, { params });
  }

  getDepensesByCategorieAndDateRange(categorieId: number, startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<DepensePage> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<DepensePage>(`${this.apiUrl}/categorie/${categorieId}/date-range`, { params });
  }

  // === STATISTIQUES ===
  getSumByCategorie(categorieId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/stats/sum/categorie/${categorieId}`);
  }

  getSumByDateRange(startDate: string, endDate: string): Observable<number> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<number>(`${this.apiUrl}/stats/sum/date-range`, { params });
  }
}

