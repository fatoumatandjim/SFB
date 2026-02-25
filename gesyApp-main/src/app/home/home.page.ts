import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationService, MenuSection } from '../services/navigation.service';
import { NavbarComponent } from '../Composant/navbar/navbar.component';
import { HeaderComponent } from '../Composant/header/header.component';
import { DashbordComponent } from '../Composant/dashbord/dashbord.component';
import { CamionComponent } from '../Composant/camion/camion.component';
import { FacturationComponent } from '../Composant/facturation/facturation.component';
import { PaiementComponent } from '../Composant/paiement/paiement.component';
import { BanqueCaisseComponent } from '../Composant/banque-caisse/banque-caisse.component';
import { StockComponent } from '../Composant/stock/stock.component';
import { ClientFournisseurComponent } from '../Composant/client-fournisseur/client-fournisseur.component';
import { SuiviTransportComponent } from '../Composant/suivi-transport/suivi-transport.component';
import { TransitaireComponent } from '../Composant/transitaire/transitaire.component';
import { AxesComponent } from '../Composant/axes/axes.component';
import { DepotComponent } from '../Composant/depot/depot.component';
import { RecouvrementComponent } from '../Composant/recouvrement/recouvrement.component';
import { RapportComponent } from '../Composant/rapport/rapport.component';
import { AnalyseComponent } from '../Composant/analyse/analyse.component';
import { SettingsComponent } from '../Composant/settings/settings.component';
import { GestionAchatComponent } from '../Composant/gestion-achat/gestion-achat.component';
import { CoutComponent } from '../Composant/cout/cout.component';
import { EmailComponent } from '../Composant/email/email.component';
import { DepensesComponent } from '../Composant/depenses/depenses.component';
import { CapitalComponent } from '../Composant/capital/capital.component';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: true,
  imports: [
    CommonModule,
    NavbarComponent,
    HeaderComponent,
    DashbordComponent,
    CamionComponent,
    FacturationComponent,
    PaiementComponent,
    BanqueCaisseComponent,
    StockComponent,
    ClientFournisseurComponent,
    SuiviTransportComponent,
    TransitaireComponent,
    AxesComponent,
    DepotComponent,
    RecouvrementComponent,
    RapportComponent,
    AnalyseComponent,
    SettingsComponent,
    GestionAchatComponent,
    CoutComponent,
    EmailComponent,
    DepensesComponent,
    CapitalComponent
  ],
})
export class HomePage implements OnInit {
  @ViewChild(NavbarComponent) navbarComponent!: NavbarComponent;
  currentSection: MenuSection = 'dashbord';

  constructor(private navigationService: NavigationService) {}

  ngOnInit() {
    this.navigationService.currentSection$.subscribe(section => {
      this.currentSection = section;
    });
  }

  onMenuToggle() {
    if (this.navbarComponent) {
      this.navbarComponent.toggleMenu();
    }
  }
}
