package com.backend.gesy.security.controller;

import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.roles.Roles;
import com.backend.gesy.security.dto.LoginRequest;
import com.backend.gesy.security.dto.LoginResponse;
import com.backend.gesy.security.service.JwtTokenBlacklistService;
import com.backend.gesy.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final CompteRepository compteRepository;
    private final JwtTokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getIdentifiant(),
                            loginRequest.getMotDePasse()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getIdentifiant());
            String token = jwtUtil.generateToken(userDetails);

            // Récupérer les rôles du compte
            Compte compte = compteRepository.findByIdentifiant(loginRequest.getIdentifiant())
                    .orElseThrow(() -> new RuntimeException("Compte non trouvé"));

            List<String> roles = compte.getRoles().stream()
                    .filter(role -> role.getStatut() == Roles.StatutRole.ACTIF)
                    .map(role -> "ROLE_" + role.getNom().toUpperCase())
                    .collect(Collectors.toList());

            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .identifiant(loginRequest.getIdentifiant())
                    .roles(roles)
                    .build();

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Identifiant ou mot de passe incorrect");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'authentification: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tokenBlacklistService.blacklistToken(token);
                return ResponseEntity.ok("Déconnexion réussie");
            }
            return ResponseEntity.ok("Déconnexion réussie");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }
}

