package com.example.proyecto_cobros.controller;

import com.example.proyecto_cobros.entity.Estudiante;
import com.example.proyecto_cobros.repository.EstudianteRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/estudiantes")
public class EstudianteController {

    @Autowired
    private EstudianteRepository repo;

    private boolean estaLogueado(HttpSession session) {
        return session.getAttribute("usuario") != null;
    }

    @GetMapping
    public String listarEstudiantes(HttpSession session, Model model) {
        if (!estaLogueado(session)) {
            return "redirect:/usuarios";
        }
        model.addAttribute("estudiantes", repo.findAll());
        return "estudiantes";
    }

    @PostMapping("/crear")
    public String crearEstudiante(
            @RequestParam String nombre,
            @RequestParam String nie,
            @RequestParam String grado,
            @RequestParam String seccion,
            @RequestParam String encargado,
            @RequestParam String telefono,
            HttpSession session
    ) {
        if (!estaLogueado(session)) {
            return "redirect:/usuarios";
        }

        Estudiante e = new Estudiante();
        e.setNombre(nombre);
        e.setNie(nie);
        e.setGrado(grado);
        e.setSeccion(seccion);
        e.setEncargado(encargado);
        e.setTelefono(telefono);
        e.setEstado("activo");
        e.setFechaRegistro(LocalDateTime.now());

        repo.save(e);
        return "redirect:/estudiantes?success=created";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarEstudiante(@PathVariable Integer id, HttpSession session) {
        if (!estaLogueado(session)) {
            return "redirect:/usuarios";
        }
        repo.deleteById(id);
        return "redirect:/estudiantes?success=deleted";
    }
}