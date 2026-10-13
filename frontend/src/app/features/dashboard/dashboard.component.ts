import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  LucideDynamicIcon,
  LucideEuro,
  LucideCalendarDays,
  LucideUserCheck,
  LucideUserX
} from '@lucide/angular';
import { DashboardService, DashboardResumen, DiaIngreso } from '../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideDynamicIcon],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

readonly IconEuro = LucideEuro;
readonly IconCalendario = LucideCalendarDays;
readonly IconActivos = LucideUserCheck;
readonly IconSinServicio = LucideUserX;

  readonly etiquetasDias = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

  resumen = signal<DashboardResumen | null>(null);
  cargando = signal(true);
  rangoSemana = '';

  maxPrevisto = computed(() => {
    const dias = this.resumen()?.ingresosPorDia ?? [];
    return Math.max(1, ...dias.map(d => d.previsto));
  });

  constructor(private dashboardService: DashboardService) {}

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

  alturaBarra(dia: DiaIngreso): number {
    return (dia.previsto / this.maxPrevisto()) * 100;
  }

  etiquetaTipo(tipo: string): string {
    switch (tipo) {
      case 'CASA': return 'Limpieza del hogar';
      case 'OFICINA': return 'Oficina';
      case 'EVENTO': return 'Evento';
      default: return 'Servicio';
    }
  }

  private calcularRangoSemana(): void {
    const hoy = new Date();
    const dia = hoy.getDay();
    const diff = dia === 0 ? -6 : 1 - dia;
    const lunes = new Date(hoy);
    lunes.setDate(hoy.getDate() + diff);
    const domingo = new Date(lunes);
    domingo.setDate(lunes.getDate() + 6);

    const formato = (d: Date) =>
      `${d.getDate()} ${d.toLocaleDateString('es-ES', { month: 'short' }).replace('.', '')}`;
    this.rangoSemana = `${formato(lunes)} – ${formato(domingo)}`;
  }
}