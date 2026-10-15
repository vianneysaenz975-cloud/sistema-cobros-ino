package com.example.proyecto_cobros.repository;

import com.example.proyecto_cobros.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Usuario findByUsername(String username);
    Usuario findByUsernameAndPasswordHash(String username, String passwordHash);
}