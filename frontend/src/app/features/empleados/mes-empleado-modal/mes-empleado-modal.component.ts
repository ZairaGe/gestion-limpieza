import { Component, EventEmitter, OnInit, Output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmpleadoService } from '../../../core/services/empleado.service';
import { ServicioService } from '../../../core/services/servicio.service';
import { Empleado } from '../../../core/models/empleado.model';
import { Servicio } from '../../../core/models/servicio.model';

interface Fila {
  id: number;
  fecha: string;
  horario: string;
  minutos: number;
  cliente: string;
  direccion: string;
  estado: string;
}

@Component({
  selector: 'app-mes-empleado-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mes-empleado-modal.component.html',
  styleUrl: './mes-empleado-modal.component.css'
})
export class MesEmpleadoModalComponent implements OnInit {

  @Output() cerrar = new EventEmitter<void>();

  empleados = signal<Empleado[]>([]);
  servicios = signal<Servicio[]>([]);
  empleadoId = signal<number | null>(null);
  mes = signal<string>(this.mesActual());
  cargando = signal(false);
  error = signal<string | null>(null);

  private peticion = 0;

  readonly etiquetasEstado: Record<string, string> = {
    PENDIENTE: 'Pendiente',
    CONFIRMADO: 'Confirmado',
    COMPLETADO: 'Completado',
    CANCELADO: 'Cancelado'
  };

  filas = computed<Fila[]>(() => {
    const id = this.empleadoId();
    if (id === null) return [];

    return this.servicios()
      .filter(s => s.empleados.some(e => e.id === id))
      .sort((a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio))
      .map(s => ({
        id: s.id!,
        fecha: this.fechaCorta(s.fecha),
        horario: `${s.horaInicio.substring(0, 5)}–${s.horaFin.substring(0, 5)}`,
        minutos: this.minutosDe(s),
        cliente: s.clienteNombre,
        direccion: s.direccion,
        estado: s.estado
      }));
  });

  minutosTrabajados = computed(() =>
    this.servicios()
      .filter(s => s.estado === 'COMPLETADO' && s.empleados.some(e => e.id === this.empleadoId()))
      .reduce((acc, s) => acc + this.minutosDe(s), 0)
  );

  minutosProgramados = computed(() =>
    this.servicios()
      .filter(s => s.estado !== 'CANCELADO' && s.empleados.some(e => e.id === this.empleadoId()))
      .reduce((acc, s) => acc + this.minutosDe(s), 0)
  );

  constructor(
    private empleadoService: EmpleadoService,
    private servicioService: ServicioService
  ) { }

  ngOnInit(): void {
    this.empleadoService.listar().subscribe({
      next: (data) => {
        const ordenados = [...data].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
        this.empleados.set(ordenados);
        if (ordenados.length > 0) {
          this.empleadoId.set(ordenados[0].id!);
          this.cargar();
        }
      },
      error: () => this.error.set('No se pudieron cargar los trabajadores.')
    });
  }

  cambiarEmpleado(valor: number | null): void {
    this.empleadoId.set(valor);
    this.cargar();
  }

  cambiarMes(valor: string): void {
    this.mes.set(valor);
    this.cargar();
  }

  private cargar(): void {
    const mes = this.mes();
    const id = ++this.peticion;

    if (this.empleadoId() === null || !/^\d{4}-\d{2}$/.test(mes)) {
      this.servicios.set([]);
      return;
    }

    const [y, m] = mes.split('-').map(Number);
    const ultimo = new Date(y, m, 0).getDate();

    this.cargando.set(true);
    this.error.set(null);

    this.servicioService.listarPorRango(`${mes}-01`, `${mes}-${String(ultimo).padStart(2, '0')}`).subscribe({
      next: (data) => {
        if (id !== this.peticion) return;
        this.servicios.set(data);
        this.cargando.set(false);
      },
      error: () => {
        if (id !== this.peticion) return;
        this.error.set('No se pudieron cargar los servicios del mes.');
        this.cargando.set(false);
      }
    });
  }

  puedeDescargar(): boolean {
    return this.empleadoId() !== null && /^\d{4}-\d{2}$/.test(this.mes());
  }

  descargar(): void {
    const id = this.empleadoId();
    const empleado = this.empleados().find(e => e.id === id);
    if (id === null || !empleado || !this.puedeDescargar()) return;
    this.empleadoService.descargarResumenMensual(id, this.mes(), empleado.nombre);
  }

  // ---------- Presentación ----------

  textoHoras(min: number): string {
    return `${Math.floor(min / 60)}h ${min % 60}m`;
  }

  textoDuracion(min: number): string {
    return min % 60 === 0 ? `${min / 60}h` : `${Math.floor(min / 60)}h ${min % 60}m`;
  }

  private minutosDe(s: Servicio): number {
    const [hI, mI] = s.horaInicio.split(':').map(Number);
    const [hF, mF] = s.horaFin.split(':').map(Number);
    return (hF * 60 + mF) - (hI * 60 + mI);
  }

  private fechaCorta(iso: string): string {
    const [y, m, d] = iso.split('-').map(Number);
    const mes = new Date(y, m - 1, d).toLocaleDateString('es-ES', { month: 'short' }).replace('.', '');
    return `${d} ${mes} ${y}`;
  }

  private mesActual(): string {
    const hoy = new Date();
    return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}`;
  }
}