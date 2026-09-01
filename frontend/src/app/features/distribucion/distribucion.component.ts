import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServicioService } from '../../core/services/servicio.service';
import { EmpleadoService } from '../../core/services/empleado.service';
import { Servicio, ServicioRequest } from '../../core/models/servicio.model';
import { Empleado } from '../../core/models/empleado.model';

interface DiaSemana {
  fecha: string;
  etiqueta: string;
  esHoy: boolean;
  servicios: Servicio[];
}

@Component({
  selector: 'app-distribucion',
  standalone: true,
  imports: [CommonModule],
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

  constructor(
    private servicioService: ServicioService,
    private empleadoService: EmpleadoService
  ) {}

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
    return fecha.toISOString().split('T')[0];
  }

  cargarSemana(): void {
    this.cargando.set(true);
    const desde = this.formatearFecha(this.inicioSemana);
    const finSemana = new Date(this.inicioSemana);
    finSemana.setDate(finSemana.getDate() + 6);
    const hasta = this.formatearFecha(finSemana);

    this.servicioService.listarPorRango(desde, hasta).subscribe({
      next: (servicios) => {
        this.dias.set(this.agruparPorDia(servicios));
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
}