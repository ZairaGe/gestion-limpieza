package com.mokeal.gestion.controller;

import com.mokeal.gestion.dto.DashboardResponse;
import com.mokeal.gestion.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumen")
    public DashboardResponse resumen() {
        return dashboardService.obtenerResumen();
    }
}