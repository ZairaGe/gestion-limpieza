import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardResumen } from '../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  resumen = signal<DashboardResumen | null>(null);
  cargando = signal(true);
  rangoSemana = '';

  constructor(private dashboardService: DashboardService) { }

  ngOnInit(): void {
    this.calcularRangoSemana();
    this.dashboardService.obtenerResumen().subscribe({
      next: (data) => {
        this.resumen.set(data);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  private calcularRangoSemana(): void {
    const hoy = new Date();
    const dia = hoy.getDay();
    const diff = dia === 0 ? -6 : 1 - dia;
    const lunes = new Date(hoy);
    lunes.setDate(hoy.getDate() + diff);
    const domingo = new Date(lunes);
    domingo.setDate(lunes.getDate() + 6);

    const formato = (d: Date) => `${d.getDate()} ${d.toLocaleDateString('es-ES', { month: 'short' }).replace('.', '')}`;
    this.rangoSemana = `${formato(lunes)} – ${formato(domingo)}`;
  }

  hoyFormateado = new Date().toLocaleDateString('es-ES', {
    weekday: 'long',
    day: 'numeric',
    month: 'long'
  });
}