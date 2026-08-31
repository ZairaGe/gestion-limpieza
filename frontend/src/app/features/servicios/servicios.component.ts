import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ServicioService } from '../../core/services/servicio.service';
import { ClienteService } from '../../core/services/cliente.service';
import { TarifaService } from '../../core/services/tarifa.service';
import { EmpleadoService } from '../../core/services/empleado.service';
import { Servicio, ServicioRequest, EstadoServicio } from '../../core/models/servicio.model';
import { Cliente } from '../../core/models/cliente.model';
import { Tarifa } from '../../core/models/tarifa.model';
import { Empleado } from '../../core/models/empleado.model';

@Component({
  selector: 'app-servicios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './servicios.component.html',
  styleUrl: './servicios.component.css'
})
export class ServiciosComponent implements OnInit {

  servicios = signal<Servicio[]>([]);
  clientes = signal<Cliente[]>([]);
  tarifas = signal<Tarifa[]>([]);
  empleados = signal<Empleado[]>([]);

  cargando = signal(true);
  error = signal<string | null>(null);

  modalAbierto = signal(false);
  servicioEnEdicion: Servicio | null = null;
  formServicio: ServicioRequest = this.servicioVacio();

  constructor(
    private servicioService: ServicioService,
    private clienteService: ClienteService,
    private tarifaService: TarifaService,
    private empleadoService: EmpleadoService
  ) {}

  ngOnInit(): void {
    this.cargarTodo();
  }

  cargarTodo(): void {
    this.cargando.set(true);
    this.servicioService.listar().subscribe({
      next: (data) => {
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

  servicioVacio(): ServicioRequest {
    return {
      clienteId: 0,
      tarifaId: 0,
      direccion: '',
      fecha: '',
      horaInicio: '',
      horaFin: '',
      empleadoIds: []
    };
  }

  abrirModalCrear(): void {
    this.servicioEnEdicion = null;
    this.formServicio = this.servicioVacio();
    this.modalAbierto.set(true);
  }

  abrirModalEditar(servicio: Servicio): void {
  this.servicioEnEdicion = servicio;
  this.formServicio = {
    clienteId: servicio.clienteId,
    tarifaId: servicio.tarifaId,
    direccion: servicio.direccion,
    fecha: servicio.fecha,
    horaInicio: servicio.horaInicio,
    horaFin: servicio.horaFin,
    empleadoIds: servicio.empleados.map(e => e.id)
  };
  this.modalAbierto.set(true);
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

  guardarServicio(): void {
    if (this.servicioEnEdicion) {
      this.servicioService.actualizar(this.servicioEnEdicion.id!, this.formServicio).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarTodo();
        },
        error: () => this.error.set('No se pudo actualizar el servicio')
      });
    } else {
      this.servicioService.crear(this.formServicio).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarTodo();
        },
        error: () => this.error.set('No se pudo crear el servicio')
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
}