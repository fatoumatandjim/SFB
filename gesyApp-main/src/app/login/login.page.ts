import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class LoginPage implements OnInit {
  credentials = {
    identifiant: '',
    motDePasse: ''
  };

  showPassword: boolean = false;
  isLoading: boolean = false;
  errorMessage: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit() {
    // Le guard gère maintenant la redirection si l'utilisateur est déjà connecté
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  onLogin() {
    if (!this.credentials.identifiant || !this.credentials.motDePasse) {
      this.errorMessage = 'Veuillez remplir tous les champs';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.credentials.identifiant, this.credentials.motDePasse).subscribe({
      next: () => {
        this.isLoading = false;
        // Redirection selon le rôle (authService a déjà stocké les rôles)
        if (this.authService.isTransitaire()) {
          this.router.navigate(['/for-transitaire']);
        } else {
          this.router.navigate(['/home']);
        }
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Identifiant ou mot de passe incorrect';
        console.error('Erreur de connexion:', error);
      }
    });
  }
}
