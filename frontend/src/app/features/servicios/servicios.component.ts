import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideDynamicIcon,
  LucidePlus,
  LucidePencil,
  LucideTrash2,
  LucideChevronDown
} from '@lucide/angular';
import { ServicioService } from '../../core/services/servicio.service';
import { ClienteService } from '../../core/services/cliente.service';
import { TarifaService } from '../../core/services/tarifa.service';
import { EmpleadoService } from '../../core/services/empleado.service';
import { Servicio, ServicioRequest, EstadoServicio } from '../../core/models/servicio.model';
import { Cliente } from '../../core/models/cliente.model';
import { Tarifa } from '../../core/models/tarifa.model';
import { Empleado } from '../../core/models/empleado.model';
import { ServicioRecurrenteService } from '../../core/services/servicio-recurrente.service';
import { ServicioRecurrenteRequest, DiaSemana } from '../../core/models/servicio-recurrente.model';
import { PaginacionComponent } from '../../shared/components/paginacion/paginacion.component';

interface FormServicio {
  clienteId: number;
  tarifaId: number | null;
  fecha: string;
  horaInicio: string;
  duracion: number | null;
  direccion: string;
  precio: number | null;
  estado: EstadoServicio;
  empleadoIds: number[];
  notas: string;
  recurrente: boolean;
  diasSemana: DiaSemana[];
  repetirHasta: string;
}

@Component({
  selector: 'app-servicios',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginacionComponent, LucideDynamicIcon],
  templateUrl: './servicios.component.html',
  styleUrl: './servicios.component.css'
})
export class ServiciosComponent implements OnInit {

  readonly IconPlus = LucidePlus;
  readonly IconEditar = LucidePencil;
  readonly IconEliminar = LucideTrash2;
  readonly IconChevron = LucideChevronDown;

  servicios = signal<Servicio[]>([]);
  clientes = signal<Cliente[]>([]);
  tarifas = signal<Tarifa[]>([]);
  empleados = signal<Empleado[]>([]);

  cargando = signal(true);
  error = signal<string | null>(null);

  modalAbierto = signal(false);
  servicioEnEdicion: Servicio | null = null;
  formServicio: FormServicio = this.servicioVacio();
  errorModal = signal<string | null>(null);

  diasModal: { valor: DiaSemana; letra: string; nombre: string }[] = [
    { valor: 'LUNES', letra: 'L', nombre: 'Lunes' },
    { valor: 'MARTES', letra: 'M', nombre: 'Martes' },
    { valor: 'MIERCOLES', letra: 'X', nombre: 'Miércoles' },
    { valor: 'JUEVES', letra: 'J', nombre: 'Jueves' },
    { valor: 'VIERNES', letra: 'V', nombre: 'Viernes' },
    { valor: 'SABADO', letra: 'S', nombre: 'Sábado' },
    { valor: 'DOMINGO', letra: 'D', nombre: 'Domingo' }
  ];

  modalRecurrenteAbierto = signal(false);
  mensajeExito = signal<string | null>(null);

  paginaActual = signal(1);
  itemsPorPagina = 10;

  filtroDesde = signal('');
  filtroHasta = signal('');
  filtroEstado = signal('TODOS');

  opcionesFiltro = [
    { valor: 'TODOS', etiqueta: 'Todos' },
    { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
    { valor: 'CONFIRMADO', etiqueta: 'Confirmado' },
    { valor: 'COMPLETADO', etiqueta: 'Completado' },
    { valor: 'CANCELADO', etiqueta: 'Cancelado' }
  ];

  estadosDisponibles = ['PENDIENTE', 'CONFIRMADO', 'COMPLETADO', 'CANCELADO'];

  estadoEtiquetas: Record<string, string> = {
    PENDIENTE: 'Pendiente',
    CONFIRMADO: 'Confirmado',
    COMPLETADO: 'Completado',
    CANCELADO: 'Cancelado'
  };

  serviciosFiltrados = computed(() => {
    const desde = this.filtroDesde();
    const hasta = this.filtroHasta();
    const estado = this.filtroEstado();
    return this.servicios().filter(s =>
      (estado === 'TODOS' || s.estado === estado) &&
      (!desde || s.fecha >= desde) &&
      (!hasta || s.fecha <= hasta)
    );
  });

  serviciosPaginados = computed(() => {
    const inicio = (this.paginaActual() - 1) * this.itemsPorPagina;
    return this.serviciosFiltrados().slice(inicio, inicio + this.itemsPorPagina);
  });

  totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.serviciosFiltrados().length / this.itemsPorPagina))
  );

  constructor(
    private servicioService: ServicioService,
    private clienteService: ClienteService,
    private tarifaService: TarifaService,
    private empleadoService: EmpleadoService,
    private servicioRecurrenteService: ServicioRecurrenteService
  ) { }

  ngOnInit(): void {
    this.cargarTodo();
  }

  cargarTodo(): void {
    this.cargando.set(true);
    this.servicioService.listar().subscribe({
      next: (data) => {
        this.paginaActual.set(1);
        this.servicios.set(data);
        this.cargarDatosApoyo();
      },
      error: () => {
        this.error.set('No se pudieron cargar los servicios');
        this.cargando.set(false);
      }
    });
  }

  cargarDatosApoyo(): void {
    this.clienteService.listar().subscribe(data => this.clientes.set(data));
    this.tarifaService.listar().subscribe(data => this.tarifas.set(data));
    this.empleadoService.listar().subscribe({
      next: (data) => {
        this.empleados.set(data);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  // ---------- Filtros y formato ----------

  cambiarDesde(valor: string): void {
    this.filtroDesde.set(valor);
    this.paginaActual.set(1);
  }

  cambiarHasta(valor: string): void {
    this.filtroHasta.set(valor);
    this.paginaActual.set(1);
  }

  cambiarFiltroEstado(valor: string): void {
    this.filtroEstado.set(valor);
    this.paginaActual.set(1);
  }

  formatearFecha(fecha: string): string {
    const [y, m, d] = fecha.split('-').map(Number);
    const f = new Date(y, m - 1, d);
    const dia = f.toLocaleDateString('es-ES', { weekday: 'short' }).replace('.', '');
    const mes = f.toLocaleDateString('es-ES', { month: 'short' }).replace('.', '');
    return `${dia} ${d} ${mes}`;
  }

  duracionTexto(s: Servicio): string {
    const horas = s.duracionHoras ?? this.calcularDuracionHoras(s.horaInicio, s.horaFin);
    return `${horas.toLocaleString('es-ES', { maximumFractionDigits: 2 })}h`;
  }

  etiquetaTipo(s: Servicio): string {
    return s.tarifaNombre ?? 'Servicio de limpieza';
  }

  nombresEmpleados(s: Servicio): string {
    return s.empleados.map(e => e.nombre).join(', ');
  }

  cambiarPagina(pagina: number): void {
    this.paginaActual.set(pagina);
  }

  // ---------- Crear / editar ----------

  servicioVacio(): FormServicio {
    return {
      clienteId: 0,
      tarifaId: null,
      fecha: this.aFechaLocal(new Date()),
      horaInicio: '09:00',
      duracion: 2,
      direccion: '',
      precio: null,
      estado: 'PENDIENTE',
      empleadoIds: [],
      notas: '',
      recurrente: false,
      diasSemana: [],
      repetirHasta: ''
    };
  }

  abrirModalCrear(): void {
    this.servicioEnEdicion = null;
    this.formServicio = this.servicioVacio();
    this.errorModal.set(null);
    this.modalAbierto.set(true);
  }

  cambiarCliente(clienteId: number): void {
    this.formServicio.clienteId = clienteId;

    const cliente = this.clientes().find(c => c.id === clienteId);

    this.formServicio.direccion = cliente?.direccion ?? '';
  }

  abrirModalEditar(servicio: Servicio): void {
    this.servicioEnEdicion = servicio;
    this.errorModal.set(null);
    const duracion = servicio.duracionHoras ?? this.calcularDuracionHoras(servicio.horaInicio, servicio.horaFin);
    const inicio = servicio.horaInicio.substring(0, 5);

    this.formServicio = {
      clienteId: servicio.clienteId,
      tarifaId: servicio.tarifaId,
      fecha: servicio.fecha,
      horaInicio: inicio,
      duracion: duracion,
      direccion: servicio.direccion,
      precio: servicio.precioManual ?? null,
      estado: servicio.estado,
      empleadoIds: servicio.empleados.map(e => e.id),
      notas: servicio.notas ?? '',
      recurrente: false,
      diasSemana: [],
      repetirHasta: ''
    };
    this.modalAbierto.set(true);
  }

  private calcularDuracionHoras(horaInicio: string, horaFin: string): number {
    const [hI, mI] = horaInicio.split(':').map(Number);
    const [hF, mF] = horaFin.split(':').map(Number);
    const minutosTotales = (hF * 60 + mF) - (hI * 60 + mI);
    return Math.round((minutosTotales / 60) * 4) / 4;
  }

  private aFechaLocal(d: Date): string {
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mes}-${dia}`;
  }

  private sumarHoras(hora: string, horas: number): string {
    const [h, m] = hora.split(':').map(Number);
    const total = Math.round(h * 60 + m + horas * 60);
    const hh = Math.floor(total / 60) % 24;
    const mm = total % 60;
    return `${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}`;
  }

  duracionForm(): number {
    const d = Number(this.formServicio.duracion);
    return d > 0 ? d : 0;
  }

  horaFinReal(): string {
    const f = this.formServicio;
    if (!f.horaInicio || this.duracionForm() === 0) return '';
    const n = Math.max(1, f.empleadoIds.length);
    return this.sumarHoras(f.horaInicio, this.duracionForm() / n);
  }

  private cruzaMedianoche(): boolean {
  const f = this.formServicio;
  const [h, m] = f.horaInicio.split(':').map(Number);
  const n = Math.max(1, f.empleadoIds.length);
  return h * 60 + m + Math.round((this.duracionForm() / n) * 60) >= 24 * 60;
}

  etiquetaTarifa(t: Tarifa): string {
    return t.nombre;
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  empleadoSeleccionado(id: number): boolean {
    return this.formServicio.empleadoIds.includes(id);
  }

  toggleEmpleado(id: number): void {
    const idx = this.formServicio.empleadoIds.indexOf(id);
    if (idx > -1) {
      this.formServicio.empleadoIds.splice(idx, 1);
    } else {
      this.formServicio.empleadoIds.push(id);
    }
  }

  toggleRecurrente(valor: boolean): void {
    this.formServicio.recurrente = valor;
    if (valor && !this.formServicio.repetirHasta && this.formServicio.fecha) {
      const [y, m, d] = this.formServicio.fecha.split('-').map(Number);
      const f = new Date(y, m - 1, d);
      f.setDate(f.getDate() + 30);
      this.formServicio.repetirHasta = this.aFechaLocal(f);
    }
  }

  toggleDiaServicio(dia: DiaSemana): void {
    const idx = this.formServicio.diasSemana.indexOf(dia);
    if (idx > -1) {
      this.formServicio.diasSemana.splice(idx, 1);
    } else {
      this.formServicio.diasSemana.push(dia);
    }
  }

  guardarServicio(): void {
    const f = this.formServicio;
    const duracion = this.duracionForm();

    if (!f.clienteId) { this.errorModal.set('Selecciona un cliente.'); return; }
    if (!f.direccion.trim()) { this.errorModal.set('Escribe la dirección del servicio.'); return; }
    if (!f.fecha) { this.errorModal.set('Elige la fecha.'); return; }
    if (!f.horaInicio) { this.errorModal.set('Indica la hora de inicio.'); return; }
    if (duracion <= 0) { this.errorModal.set('Indica cuántas horas dura el servicio.'); return; }
    if (this.cruzaMedianoche()) { this.errorModal.set('El servicio no puede acabar pasada la medianoche.'); return; }

    const recurrente = f.recurrente && !this.servicioEnEdicion;
    if (recurrente) {
      if (f.diasSemana.length === 0) { this.errorModal.set('Elige al menos un día de la semana.'); return; }
      if (!f.repetirHasta || f.repetirHasta < f.fecha) {
        this.errorModal.set('"Hasta" tiene que ser igual o posterior a "Desde".');
        return;
      }
    }

    const request: ServicioRequest = {
      clienteId: f.clienteId,
      tarifaId: f.tarifaId,
      tarifaNombre: this.tarifas().find(t => t.id === f.tarifaId)?.nombre ?? null,
      direccion: f.direccion.trim(),
      fecha: f.fecha,
      horaInicio: f.horaInicio,
      duracionHoras: duracion,
      empleadoIds: f.empleadoIds,
      precio: f.precio,
      notas: f.notas.trim() || null,
      estado: f.estado,
      ...(recurrente ? { diasSemana: f.diasSemana, repetirHasta: f.repetirHasta } : {})
    };

    this.errorModal.set(null);

    if (this.servicioEnEdicion) {
      this.servicioService.actualizar(this.servicioEnEdicion.id!, request).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarTodo();
        },
        error: () => this.errorModal.set('No se pudo actualizar el servicio. Revisa los datos.')
      });
    } else {
      this.servicioService.crear(request).subscribe({
        next: (creados) => {
          this.cerrarModal();
          if (creados.length > 1) {
            this.mensajeExito.set(`Se han creado ${creados.length} servicios.`);
            setTimeout(() => this.mensajeExito.set(null), 5000);
          }
          this.cargarTodo();
        },
        error: () => this.errorModal.set('No se pudo crear el servicio. Revisa los datos.')
      });
    }
  }

  eliminarServicio(id: number): void {
    if (!confirm('¿Seguro que quieres eliminar este servicio?')) return;

    this.servicioService.eliminar(id).subscribe({
      next: () => this.cargarTodo(),
      error: () => this.error.set('No se pudo eliminar el servicio')
    });
  }

  cambiarEstado(servicio: Servicio, nuevoEstado: string): void {
    this.servicioService.cambiarEstado(servicio.id!, nuevoEstado).subscribe({
      next: () => this.cargarTodo(),
      error: () => this.error.set('No se pudo cambiar el estado')
    });
  }

  // ---------- Servicio recurrente (sin botón por ahora, se moverá al modal de "Nuevo servicio") ----------

  diasDisponibles: { valor: DiaSemana; etiqueta: string }[] = [
    { valor: 'LUNES', etiqueta: 'Lunes' },
    { valor: 'MARTES', etiqueta: 'Martes' },
    { valor: 'MIERCOLES', etiqueta: 'Miércoles' },
    { valor: 'JUEVES', etiqueta: 'Jueves' },
    { valor: 'VIERNES', etiqueta: 'Viernes' },
    { valor: 'SABADO', etiqueta: 'Sábado' },
    { valor: 'DOMINGO', etiqueta: 'Domingo' }
  ];

  formRecurrente: ServicioRecurrenteRequest = this.recurrenteVacio();

  recurrenteVacio(): ServicioRecurrenteRequest {
    return {
      clienteId: 0,
      tarifaId: 0,
      direccion: '',
      horaInicio: '',
      duracionHoras: 2,
      diasSemana: [],
      empleadoIds: [],
      fechaInicio: '',
      fechaFin: ''
    };
  }

  abrirModalRecurrente(): void {
    this.formRecurrente = this.recurrenteVacio();
    this.modalRecurrenteAbierto.set(true);
  }

  cerrarModalRecurrente(): void {
    this.modalRecurrenteAbierto.set(false);
  }

  toggleDia(dia: DiaSemana): void {
    const idx = this.formRecurrente.diasSemana.indexOf(dia);
    if (idx > -1) {
      this.formRecurrente.diasSemana.splice(idx, 1);
    } else {
      this.formRecurrente.diasSemana.push(dia);
    }
  }

  toggleEmpleadoRecurrente(id: number): void {
    const idx = this.formRecurrente.empleadoIds.indexOf(id);
    if (idx > -1) {
      this.formRecurrente.empleadoIds.splice(idx, 1);
    } else {
      this.formRecurrente.empleadoIds.push(id);
    }
  }

  guardarRecurrente(): void {
    this.servicioRecurrenteService.crear(this.formRecurrente).subscribe({
      next: (resultado) => {
        this.cerrarModalRecurrente();
        this.mensajeExito.set(`Se generaron ${resultado.serviciosGenerados} servicios correctamente.`);
        this.cargarTodo();
        setTimeout(() => this.mensajeExito.set(null), 5000);
      },
      error: () => this.error.set('No se pudo crear el servicio recurrente')
    });
  }
}