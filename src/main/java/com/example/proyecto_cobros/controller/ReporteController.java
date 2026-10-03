package com.example.proyecto_cobros.controller;

import com.example.proyecto_cobros.entity.Pago;
import com.example.proyecto_cobros.entity.Estudiante;
import com.example.proyecto_cobros.repository.PagoRepository;
import com.example.proyecto_cobros.repository.EstudianteRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    @Autowired
    private PagoRepository pagoRepo;

    @Autowired
    private EstudianteRepository estudianteRepo;

    private boolean estaLogueado(HttpSession session) {
        return session.getAttribute("usuario") != null;
    }

    @GetMapping
    public String verReportes(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String estadoFiltro,
            @RequestParam(required = false) String estudianteFiltro
    ) {
        if (!estaLogueado(session)) {
            return "redirect:/usuarios";
        }

        List<Pago> todosLosPagos = pagoRepo.findAll();
        List<Estudiante> todosLosEstudiantes = estudianteRepo.findAll();

        // =========================================
        // FILTROS
        // =========================================
        List<Pago> pagosFiltrados = new ArrayList<>(todosLosPagos);

        // Filtro por fecha
        if (fechaInicio != null && !fechaInicio.isEmpty() && fechaFin != null && !fechaFin.isEmpty()) {
            try {
                LocalDate inicio = LocalDate.parse(fechaInicio);
                LocalDate fin = LocalDate.parse(fechaFin);
                pagosFiltrados = pagosFiltrados.stream()
                        .filter(p -> p.getFechaPago() != null
                                && !p.getFechaPago().isBefore(inicio)
                                && !p.getFechaPago().isAfter(fin))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // Ignorar error
            }
        }

        // Filtro por estado
        if (estadoFiltro != null && !estadoFiltro.isEmpty() && !estadoFiltro.equals("todos")) {
            pagosFiltrados = pagosFiltrados.stream()
                    .filter(p -> estadoFiltro.equalsIgnoreCase(p.getEstado()))
                    .collect(Collectors.toList());
        }

        // Filtro por estudiante
        if (estudianteFiltro != null && !estudianteFiltro.isEmpty() && !estudianteFiltro.equals("todos")) {
            pagosFiltrados = pagosFiltrados.stream()
                    .filter(p -> estudianteFiltro.equalsIgnoreCase(p.getEstudiante()))
                    .collect(Collectors.toList());
        }

        // =========================================
        // TOTALES GENERALES
        // =========================================
        long totalPagos = pagosFiltrados.size();
        long totalEstudiantes = todosLosEstudiantes.size();

        BigDecimal totalRecaudado = pagosFiltrados.stream()
                .filter(p -> "pagado".equalsIgnoreCase(p.getEstado()))
                .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPendiente = pagosFiltrados.stream()
                .filter(p -> "pendiente".equalsIgnoreCase(p.getEstado()))
                .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // =========================================
        // PAGOS POR MES
        // =========================================
        Map<String, BigDecimal> pagosPorMes = new TreeMap<>(Collections.reverseOrder());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));

        for (Pago p : pagosFiltrados) {
            if (p.getFechaPago() != null && "pagado".equalsIgnoreCase(p.getEstado())) {
                String mes = p.getFechaPago().format(formatter);
                BigDecimal monto = p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO;
                pagosPorMes.merge(mes, monto, BigDecimal::add);
            }
        }

        // =========================================
        // ESTUDIANTES CON DEUDAS (pagos pendientes)
        // =========================================
        Map<String, BigDecimal> deudasPorEstudiante = new HashMap<>();
        for (Pago p : todosLosPagos) {
            if ("pendiente".equalsIgnoreCase(p.getEstado()) && p.getMonto() != null) {
                String est = p.getEstudiante() != null ? p.getEstudiante() : "Sin nombre";
                deudasPorEstudiante.merge(est, p.getMonto(), BigDecimal::add);
            }
        }

        List<Map.Entry<String, BigDecimal>> estudiantesConDeudas = deudasPorEstudiante.entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

        // =========================================
        // TOP 5 ESTUDIANTES (los que más pagaron)
        // =========================================
        Map<String, BigDecimal> montosPorEstudiante = new HashMap<>();
        for (Pago p : todosLosPagos) {
            if ("pagado".equalsIgnoreCase(p.getEstado()) && p.getMonto() != null) {
                String est = p.getEstudiante() != null ? p.getEstudiante() : "Sin nombre";
                montosPorEstudiante.merge(est, p.getMonto(), BigDecimal::add);
            }
        }

        List<Map.Entry<String, BigDecimal>> topEstudiantes = montosPorEstudiante.entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        // =========================================
        // ENVIAR AL MODELO
        // =========================================
        model.addAttribute("pagosFiltrados", pagosFiltrados);
        model.addAttribute("todosLosEstudiantes", todosLosEstudiantes);
        model.addAttribute("totalPagos", totalPagos);
        model.addAttribute("totalEstudiantes", totalEstudiantes);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("pagosPorMes", pagosPorMes);
        model.addAttribute("estudiantesConDeudas", estudiantesConDeudas);
        model.addAttribute("topEstudiantes", topEstudiantes);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("estadoFiltro", estadoFiltro);
        model.addAttribute("estudianteFiltro", estudianteFiltro);

        return "reportes";
    }
}