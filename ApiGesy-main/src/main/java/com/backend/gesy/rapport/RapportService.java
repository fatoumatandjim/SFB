package com.backend.gesy.rapport;

import com.backend.gesy.rapport.dto.RapportFinancierDTO;

import java.time.LocalDate;

public interface RapportService {
    RapportFinancierDTO getRapportFinancier(String periode, Integer annee, LocalDate dateDebut, LocalDate dateFin);
}

