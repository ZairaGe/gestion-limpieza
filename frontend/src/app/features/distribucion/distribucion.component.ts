import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServicioService } from '../../core/services/servicio.service';
import { EmpleadoService } from '../../core/services/empleado.service';
import { Servicio, ServicioRequest } from '../../core/models/servicio.model';
import { Empleado } from '../../core/models/empleado.model';
import { MapaServiciosComponent } from '../../shared/components/mapa-servicios/mapa-servicios.component';
import { PlanningService } from '../../core/services/planning.service';

interface DiaSemana {
  fecha: string;
  etiqueta: string;
  esHoy: boolean;
  servicios: Servicio[];
}

@Component({
  selector: 'app-distribucion',
  standalone: true,
  imports: [CommonModule, MapaServiciosComponent],
  templateUrl: './distribucion.component.html',
  styleUrl: './distribucion.component.css'
})
export class DistribucionComponent implements OnInit {

  dias = signal<DiaSemana[]>([]);
  empleados = signal<Empleado[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);

  inicioSemana = this.obtenerLunes(new Date());

  modalAbierto = signal(false);
  servicioSeleccionado: Servicio | null = null;
  empleadosSeleccionados: number[] = [];

  serviciosSemana = signal<Servicio[]>([]);
  diaSeleccionado = signal<string>(this.formatearFecha(new Date()));

  constructor(
    private servicioService: ServicioService,
    private empleadoService: EmpleadoService,
    private planningService: PlanningService
  ) { }

  ngOnInit(): void {
    this.empleadoService.listar().subscribe(data => this.empleados.set(data));
    this.cargarSemana();
  }

  obtenerLunes(fecha: Date): Date {
    const d = new Date(fecha);
    const diaSemana = d.getDay();
    const diff = diaSemana === 0 ? -6 : 1 - diaSemana;
    d.setDate(d.getDate() + diff);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  formatearFecha(fecha: Date): string {
  const year = fecha.getFullYear();
  const mes = (fecha.getMonth() + 1).toString().padStart(2, '0');
  const dia = fecha.getDate().toString().padStart(2, '0');
  return `${year}-${mes}-${dia}`;
}

  cargarSemana(): void {
    this.cargando.set(true);
    const desde = this.formatearFecha(this.inicioSemana);
    const finSemana = new Date(this.inicioSemana);
    finSemana.setDate(finSemana.getDate() + 6);
    const hasta = this.formatearFecha(finSemana);

    this.servicioService.listarPorRango(desde, hasta).subscribe({
      next: (servicios) => {
        this.serviciosSemana.set(servicios);
        //this.dias.set(this.agruparPorDia(servicios));
        const diasNuevos = this.agruparPorDia(servicios);
        this.dias.set(diasNuevos);

        const siguSeleccionado = diasNuevos.some(d => d.fecha === this.diaSeleccionado());
        if (!siguSeleccionado) {
          this.diaSeleccionado.set(diasNuevos[0].fecha);
        }
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los servicios de la semana');
        this.cargando.set(false);
      }
    });
  }

  agruparPorDia(servicios: Servicio[]): DiaSemana[] {
    const hoyStr = this.formatearFecha(new Date());
    const dias: DiaSemana[] = [];
    const nombresDias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

    for (let i = 0; i < 7; i++) {
      const fecha = new Date(this.inicioSemana);
      fecha.setDate(fecha.getDate() + i);
      const fechaStr = this.formatearFecha(fecha);

      dias.push({
        fecha: fechaStr,
        etiqueta: `${nombresDias[i]} ${fecha.getDate()}/${fecha.getMonth() + 1}`,
        esHoy: fechaStr === hoyStr,
        servicios: servicios
          .filter(s => s.fecha === fechaStr)
          .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))
      });
    }

    return dias;
  }

  semanaAnterior(): void {
    this.inicioSemana.setDate(this.inicioSemana.getDate() - 7);
    this.cargarSemana();
  }

  semanaSiguiente(): void {
    this.inicioSemana.setDate(this.inicioSemana.getDate() + 7);
    this.cargarSemana();
  }

  irAHoy(): void {
    this.inicioSemana = this.obtenerLunes(new Date());
    this.cargarSemana();
  }

  abrirAsignacion(servicio: Servicio): void {
    this.servicioSeleccionado = servicio;
    this.empleadosSeleccionados = servicio.empleados.map(e => e.id);
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  toggleEmpleado(id: number): void {
    const idx = this.empleadosSeleccionados.indexOf(id);
    if (idx > -1) {
      this.empleadosSeleccionados.splice(idx, 1);
    } else {
      this.empleadosSeleccionados.push(id);
    }
  }

  guardarAsignacion(): void {
    if (!this.servicioSeleccionado) return;

    const request: ServicioRequest = {
      clienteId: this.servicioSeleccionado.clienteId,
      tarifaId: this.servicioSeleccionado.tarifaId,
      direccion: this.servicioSeleccionado.direccion,
      fecha: this.servicioSeleccionado.fecha,
      horaInicio: this.servicioSeleccionado.horaInicio,
      duracionHoras: this.servicioSeleccionado.duracionHoras,
      horaFin: this.servicioSeleccionado.horaFin,
      empleadoIds: this.empleadosSeleccionados
    };

    this.servicioService.actualizar(this.servicioSeleccionado.id!, request).subscribe({
      next: () => {
        this.cerrarModal();
        this.cargarSemana();
      },
      error: () => this.error.set('No se pudo actualizar la asignación')
    });
  }

  calcularDistanciaKm(s1: Servicio, s2: Servicio): number | null {
    if (s1.latitud == null || s1.longitud == null || s2.latitud == null || s2.longitud == null) {
      return null;
    }

    const R = 6371; // radio de la Tierra en km
    const dLat = this.aRadianes(s2.latitud - s1.latitud);
    const dLon = this.aRadianes(s2.longitud - s1.longitud);

    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.aRadianes(s1.latitud)) * Math.cos(this.aRadianes(s2.latitud)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  private aRadianes(grados: number): number {
    return grados * (Math.PI / 180);
  }

  distanciasDia(servicios: Servicio[]): (number | null)[] {
    const distancias: (number | null)[] = [];
    for (let i = 0; i < servicios.length - 1; i++) {
      distancias.push(this.calcularDistanciaKm(servicios[i], servicios[i + 1]));
    }
    return distancias;
  }

  servicioDiaSeleccionado = computed(() => {
    const dia = this.dias().find(d => d.fecha === this.diaSeleccionado());
    return dia ? dia.servicios : [];
  });

  etiquetaDiaSeleccionado = computed(() => {
    const dia = this.dias().find(d => d.fecha === this.diaSeleccionado());
    return dia ? dia.etiqueta : '';
  });

  seleccionarDia(fecha: string): void {
    this.diaSeleccionado.set(fecha);
  }

  empleadosDelDia = computed(() => {
    const servicios = this.servicioDiaSeleccionado();
    const mapa = new Map<number, string>();
    servicios.forEach(s => s.empleados.forEach(e => mapa.set(e.id, e.nombre)));
    return Array.from(mapa.entries()).map(([id, nombre]) => ({ id, nombre }));
  });

  descargarPlanning(empleadoId: number): void {
    this.planningService.descargarPlanning(empleadoId, this.diaSeleccionado());
  }

}