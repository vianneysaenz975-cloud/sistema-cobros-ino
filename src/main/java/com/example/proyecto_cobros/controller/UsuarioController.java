package com.example.proyecto_cobros.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.proyecto_cobros.entity.Usuario;
import com.example.proyecto_cobros.repository.UsuarioRepository;
import com.example.proyecto_cobros.repository.PagoRepository;
import com.example.proyecto_cobros.repository.EstudianteRepository;
import com.example.proyecto_cobros.security.HashUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository repo;

    @Autowired
    private PagoRepository pagoRepo;

    @Autowired
    private EstudianteRepository estudianteRepo;

    // =========================================
    // LOGIN
    // =========================================

    @GetMapping
    public String mostrarLogin(HttpSession session) {
        if (session.getAttribute("usuario") != null) {
            return "redirect:/usuarios/dashboard";
        }
        return "login";
    }

    // =========================================
    // PROCESAR LOGIN (CON HASH)
    // =========================================

    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model
    ) {
        System.out.println("=========================================");
        System.out.println("LOGIN - Usuario: '" + username + "'");
        System.out.println("=========================================");

        if (username == null || password == null
                || username.trim().isEmpty() || password.trim().isEmpty()) {
            model.addAttribute("error", "Debe ingresar usuario y contraseña");
            return "login";
        }

        // Generar hash de la contraseña
        String passwordHash = HashUtil.sha256(password);
        System.out.println("Hash generado: " + passwordHash);

        // Buscar usuario por username y hash
        Usuario usuario = repo.findByUsernameAndPasswordHash(username.trim(), passwordHash);

        if (usuario == null) {
            System.out.println("Usuario o contraseña incorrectos");
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }

        System.out.println("Usuario encontrado: " + usuario.getUsername());

        if (usuario.getActivo() != null && !usuario.getActivo()) {
            System.out.println("Usuario DESACTIVADO");
            model.addAttribute("error", "El usuario está desactivado");
            return "login";
        }

        session.setAttribute("usuario", usuario);
        session.setAttribute("rol", usuario.getRol());

        System.out.println("LOGIN EXITOSO!");
        return "redirect:/usuarios/dashboard";
    }

    // =========================================
    // DASHBOARD
    // =========================================

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios";
        }

        long totalPagos = pagoRepo.count();
        long totalEstudiantes = estudianteRepo.count();

        BigDecimal totalRecaudado = BigDecimal.ZERO;
        try {
            totalRecaudado = pagoRepo.findAll()
                    .stream()
                    .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            totalRecaudado = BigDecimal.ZERO;
        }

        long pagosPendientes = pagoRepo.findAll()
                .stream()
                .filter(p -> "pendiente".equalsIgnoreCase(p.getEstado()))
                .count();

        model.addAttribute("usuario", usuario);
        model.addAttribute("totalPagos", totalPagos);
        model.addAttribute("pagosPendientes", pagosPendientes);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("totalEstudiantes", totalEstudiantes);

        return "dashboard";
    }

    // =========================================
    // PANEL ADMIN
    // =========================================

    @GetMapping("/panel-admin")
    public String panelAdmin(HttpSession session, Model model) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuario");
        if (usuarioSesion == null) {
            return "redirect:/usuarios";
        }
        if (usuarioSesion.getRol() == null || !usuarioSesion.getRol().equalsIgnoreCase("admin")) {
            return "redirect:/usuarios/dashboard";
        }
        List<Usuario> usuarios = repo.findAll();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("usuarioObj", new Usuario());
        return "admin_usuarios";
    }

    // =========================================
    // CREAR USUARIO (CON HASH)
    // =========================================

    @PostMapping("/crear")
    public String crearUsuario(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmarPassword,
            @RequestParam String rol,
            HttpSession session,
            Model model
    ) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuario");
        if (usuarioSesion == null) {
            return "redirect:/usuarios";
        }
        if (usuarioSesion.getRol() == null || !usuarioSesion.getRol().equalsIgnoreCase("admin")) {
            return "redirect:/usuarios/dashboard";
        }

        if (username == null || password == null || confirmarPassword == null || rol == null
                || username.trim().isEmpty() || password.trim().isEmpty()
                || confirmarPassword.trim().isEmpty() || rol.trim().isEmpty()) {
            model.addAttribute("error", "Todos los campos son obligatorios");
            model.addAttribute("usuarios", repo.findAll());
            model.addAttribute("usuarioObj", new Usuario());
            return "admin_usuarios";
        }

        if (password.length() < 4) {
            model.addAttribute("error", "La contraseña debe tener mínimo 4 caracteres");
            model.addAttribute("usuarios", repo.findAll());
            model.addAttribute("usuarioObj", new Usuario());
            return "admin_usuarios";
        }

        if (!password.equals(confirmarPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            model.addAttribute("usuarios", repo.findAll());
            model.addAttribute("usuarioObj", new Usuario());
            return "admin_usuarios";
        }

        Usuario existe = repo.findByUsername(username.trim());
        if (existe != null) {
            model.addAttribute("error", "El usuario ya existe");
            model.addAttribute("usuarios", repo.findAll());
            model.addAttribute("usuarioObj", new Usuario());
            return "admin_usuarios";
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username.trim());
        usuario.setPasswordHash(HashUtil.sha256(password));  // ← HASH
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        repo.save(usuario);

        return "redirect:/usuarios/panel-admin?success=created";
    }

    // =========================================
    // LOGOUT
    // =========================================

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/usuarios";
    }
}