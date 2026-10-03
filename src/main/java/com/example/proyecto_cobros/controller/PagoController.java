package com.example.proyecto_cobros.controller;

import com.example.proyecto_cobros.entity.Pago;
import com.example.proyecto_cobros.entity.Estudiante;
import com.example.proyecto_cobros.repository.PagoRepository;
import com.example.proyecto_cobros.repository.EstudianteRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    @Autowired
    private PagoRepository repo;

    @Autowired
    private EstudianteRepository estudianteRepo;

    private boolean estaLogueado(HttpSession session) {
        return session.getAttribute("usuario") != null;
    }

    // =========================================
    // LISTAR PAGOS
    // =========================================

    @GetMapping
    public String listarPagos(HttpSession session, Model model) {
        if (!estaLogueado(session)) {
            return "redirect:/usuarios";
        }
        model.addAttribute("pagos", repo.findAll());
        model.addAttribute("pagoObj", new Pago());
        return "pagos";
    }

    // =========================================
    // BUSCAR ESTUDIANTE POR NOMBRE
    // =========================================

    @GetMapping("/buscar-estudiante")
    @ResponseBody
    public Estudiante buscarEstudiante(@RequestParam String nombre) {
        List<Estudiante> estudiantes = estudianteRepo.findAll();
        for (Estudiante e : estudiantes) {
            if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(nombre.trim())) {
                return e;
            }
        }
        return null;
    }

    // =========================================
    // EDITAR PAGO
    // =========================================

    @GetMapping("/editar/{id}")
    public String editarPago(@PathVariable Integer id, HttpSession session, Model model) {
        if (!estaLogueado(session)) {
            return "redirect:/usuarios";
        }
        Pago pago = repo.findById(id).orElse(null);
        model.addAttribute("pagos", repo.findAll());
        model.addAttribute("pagoObj", pago);
        return "pagos";
    }

    // =========================================
    // GUARDAR (crear o actualizar) y REDIRIGIR A RECIBO
    // =========================================

    @PostMapping("/guardar")
    public String guardarPago(
            @RequestParam(required = false) Integer id,
            @RequestParam String estudiante,
            @RequestParam String concepto,
            @RequestParam BigDecimal monto,
            @RequestParam String fechaPago,
            @RequestParam String estado,
            @RequestParam String metodoPago,
            HttpSession session
    ) {
        if (!estaLogueado(session)) {
            return "redirect:/usuarios";
        }

        Pago pago;
        if (id != null) {
            pago = repo.findById(id).orElse(new Pago());
        } else {
            pago = new Pago();
        }

        pago.setEstudiante(estudiante);
        pago.setConcepto(concepto);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDate.parse(fechaPago));
        pago.setEstado(estado);
        pago.setMetodoPago(metodoPago);

        // Guardar y obtener el ID generado
        Pago pagoGuardado = repo.save(pago);

        // Redirigir al comprobante
        return "redirect:/pagos/recibo/" + pagoGuardado.getId();
    }

    // =========================================
    // MOSTRAR RECIBO
    // =========================================

    @GetMapping("/recibo/{id}")
    public String mostrarRecibo(@PathVariable Integer id, HttpSession session, Model model) {
        if (!estaLogueado(session)) {
            return "redirect:/usuarios";
        }

        Pago pago = repo.findById(id).orElse(null);
        if (pago == null) {
            return "redirect:/pagos";
        }

        model.addAttribute("pago", pago);

        // Buscar datos del estudiante
        Estudiante estudiante = null;
        List<Estudiante> estudiantes = estudianteRepo.findAll();
        for (Estudiante e : estudiantes) {
            if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(pago.getEstudiante())) {
                estudiante = e;
                break;
            }
        }
        model.addAttribute("estudiante", estudiante);

        return "recibo";
    }

    // =========================================
    // ELIMINAR
    // =========================================

    @GetMapping("/eliminar/{id}")
    public String eliminarPago(@PathVariable Integer id, HttpSession session) {
        if (!estaLogueado(session)) {
            return "redirect:/usuarios";
        }
        repo.deleteById(id);
        return "redirect:/pagos?success=deleted";
    }
}