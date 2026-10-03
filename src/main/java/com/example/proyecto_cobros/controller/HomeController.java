package com.example.proyecto_cobros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Redirigir al login
        return "redirect:/usuarios";
    }
}