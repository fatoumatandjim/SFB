package com.backend.gesy.analyse;

import com.backend.gesy.analyse.dto.AnalyseDTO;

import java.time.LocalDate;

public interface AnalyseService {
    AnalyseDTO getAnalyse(String periode, Integer annee, LocalDate dateDebut, LocalDate dateFin);
}

