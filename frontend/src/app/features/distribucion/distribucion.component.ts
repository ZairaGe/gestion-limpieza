import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServicioService } from '../../core/services/servicio.service';
import { EmpleadoService } from '../../core/services/empleado.service';
import { PlanningService } from '../../core/services/planning.service';
import { Servicio, ServicioRequest } from '../../core/models/servicio.model';
import { Empleado } from '../../core/models/empleado.model';
import { MapaServiciosComponent } from '../../shared/components/mapa-servicios/mapa-servicios.component';

interface DiaVista {
  fecha: string;
  numero: number;
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

  readonly nombresCortos = ['LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB', 'DOM'];

  dias = signal<DiaVista[]>([]);
  empleados = signal<Empleado[]>([]);
  error = signal<string | null>(null);
  diaSeleccionado = signal<string>(this.aIso(new Date()));

  inicioSemana = this.obtenerLunes(new Date());

  modalAbierto = signal(false);
  servicioSeleccionado: Servicio | null = null;
  empleadosSeleccionados: number[] = [];

  serviciosDia = computed(() =>
    this.dias().find(d => d.fecha === this.diaSeleccionado())?.servicios ?? []
  );

  tituloDia = computed(() => {
    const d = this.deIso(this.diaSeleccionado());
    const semana = d.toLocaleDateString('es-ES', { weekday: 'long' });
    const mes = d.toLocaleDateString('es-ES', { month: 'long' });
    return `${this.capitalizar(semana)} ${d.getDate()} ${this.capitalizar(mes)}`;
  });

  rangoSemana = computed(() => {
    const dias = this.dias();
    if (dias.length === 0) return '';
    const ini = this.deIso(dias[0].fecha);
    const fin = this.deIso(dias[dias.length - 1].fecha);
    const corto = (d: Date) =>
      `${d.getDate()} ${d.toLocaleDateString('es-ES', { month: 'short' }).replace('.', '')}`;

    return ini.getFullYear() === fin.getFullYear()
      ? `${corto(ini)} – ${corto(fin)} ${fin.getFullYear()}`
      : `${corto(ini)} ${ini.getFullYear()} – ${corto(fin)} ${fin.getFullYear()}`;
  });

  constructor(
    private servicioService: ServicioService,
    private empleadoService: EmpleadoService,
    private planningService: PlanningService
  ) { }

  ngOnInit(): void {
    this.empleadoService.listar().subscribe(data => this.empleados.set(data));
    this.cargarSemana();
  }

  // ---------- Fechas ----------

  private aIso(d: Date): string {
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mes}-${dia}`;
  }

  private deIso(iso: string): Date {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d);
  }

  private sumarDias(d: Date, n: number): Date {
    return new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);
  }

  private obtenerLunes(fecha: Date): Date {
    const diaSemana = fecha.getDay();
    const diff = diaSemana === 0 ? -6 : 1 - diaSemana;
    return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate() + diff);
  }

  private capitalizar(texto: string): string {
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }

  // ---------- Carga y navegación ----------

  cargarSemana(): void {
    const desde = this.aIso(this.inicioSemana);
    const hasta = this.aIso(this.sumarDias(this.inicioSemana, 6));

    this.servicioService.listarPorRango(desde, hasta).subscribe({
      next: (servicios) => {
        const dias = this.agruparPorDia(servicios);
        this.dias.set(dias);
        if (!dias.some(d => d.fecha === this.diaSeleccionado())) {
          this.diaSeleccionado.set(dias[0].fecha);
        }
        this.error.set(null);
      },
      error: () => this.error.set('No se pudieron cargar los servicios de la semana')
    });
  }

  private agruparPorDia(servicios: Servicio[]): DiaVista[] {
    const hoy = this.aIso(new Date());
    const activos = servicios.filter(s => s.estado !== 'CANCELADO');
    const dias: DiaVista[] = [];

    for (let i = 0; i < 7; i++) {
      const fecha = this.sumarDias(this.inicioSemana, i);
      const iso = this.aIso(fecha);
      dias.push({
        fecha: iso,
        numero: fecha.getDate(),
        esHoy: iso === hoy,
        servicios: activos
          .filter(s => s.fecha === iso)
          .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))
      });
    }
    return dias;
  }

  semanaAnterior(): void {
    this.inicioSemana = this.sumarDias(this.inicioSemana, -7);
    this.cargarSemana();
  }

  semanaSiguiente(): void {
    this.inicioSemana = this.sumarDias(this.inicioSemana, 7);
    this.cargarSemana();
  }

  irAHoy(): void {
    const hoy = new Date();
    this.inicioSemana = this.obtenerLunes(hoy);
    this.diaSeleccionado.set(this.aIso(hoy));
    this.cargarSemana();
  }

  seleccionarDia(fecha: string): void {
    this.diaSeleccionado.set(fecha);
  }

  // ---------- Presentación ----------

  claseEstado(s: Servicio): string {
    return 'chip-' + s.estado.toLowerCase();
  }

  primerNombre(nombre: string): string {
    return nombre.trim().split(/\s+/)[0];
  }

  etiquetaTipo(s: Servicio): string {
    switch (s.tarifaTipoServicio) {
      case 'CASA': return 'Limpieza de hogar';
      case 'OFICINA': return 'Limpieza de oficina';
      case 'EVENTO': return 'Limpieza de evento';
      default: return 'Servicio de limpieza';
    }
  }

  // ---------- Descargas ----------

  descargarMes(): void {
    const d = this.deIso(this.diaSeleccionado());
    const desde = this.aIso(new Date(d.getFullYear(), d.getMonth(), 1));
    const hasta = this.aIso(new Date(d.getFullYear(), d.getMonth() + 1, 0));
    this.planningService.descargarPlanningGeneral(desde, hasta);
  }

  descargarSemanaPorTrabajador(): void {
    const dias = this.dias();
    if (dias.length === 0) return;
    this.planningService.descargarPorTrabajador(dias[0].fecha, dias[dias.length - 1].fecha);
  }

  descargarDiaPorTrabajador(): void {
    const dia = this.diaSeleccionado();
    this.planningService.descargarPorTrabajador(dia, dia);
  }

  // ---------- Asignar trabajadores ----------

  abrirAsignacion(servicio: Servicio): void {
    this.servicioSeleccionado = servicio;
    this.empleadosSeleccionados = servicio.empleados.map(e => e.id);
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  empleadoMarcado(id: number): boolean {
    return this.empleadosSeleccionados.includes(id);
  }

  toggleEmpleado(id: number): void {
    const idx = this.empleadosSeleccionados.indexOf(id);
    if (idx > -1) {
      this.empleadosSeleccionados.splice(idx, 1);
    } else {
      this.empleadosSeleccionados.push(id);
    }
  }

  private calcularDuracionHoras(horaInicio: string, horaFin: string): number {
    const [hI, mI] = horaInicio.split(':').map(Number);
    const [hF, mF] = horaFin.split(':').map(Number);
    const minutos = (hF * 60 + mF) - (hI * 60 + mI);
    return Math.round((minutos / 60) * 4) / 4;
  }

  guardarAsignacion(): void {
    const s = this.servicioSeleccionado;
    if (!s) return;

    const request: ServicioRequest = {
      clienteId: s.clienteId,
      tarifaId: s.tarifaId,
      direccion: s.direccion,
      fecha: s.fecha,
      horaInicio: s.horaInicio,
      duracionHoras: s.duracionHoras ?? this.calcularDuracionHoras(s.horaInicio, s.horaFin),
      empleadoIds: this.empleadosSeleccionados,
      precio: s.precioManual ?? null,
      notas: s.notas ?? null
    };

    this.servicioService.actualizar(s.id!, request).subscribe({
      next: () => {
        this.cerrarModal();
        this.cargarSemana();
      },
      error: () => this.error.set('No se pudo actualizar la asignación')
    });
  }
}