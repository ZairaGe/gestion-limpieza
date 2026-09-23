import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PresupuestoService } from '../../core/services/presupuesto.service';
import { ClienteService } from '../../core/services/cliente.service';
import { Presupuesto, PresupuestoRequest, LineaPresupuestoRequest } from '../../core/models/presupuesto.model';
import { Cliente } from '../../core/models/cliente.model';

@Component({
  selector: 'app-presupuestos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './presupuestos.component.html',
  styleUrl: './presupuestos.component.css'
})
export class PresupuestosComponent implements OnInit {

  presupuestos = signal<Presupuesto[]>([]);
  clientes = signal<Cliente[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  mensajeExito = signal<string | null>(null);

  modalAbierto = signal(false);
  formPresupuesto: PresupuestoRequest = this.presupuestoVacio();

  estadosDisponibles = ['PENDIENTE', 'ACEPTADO', 'RECHAZADO'];

  constructor(
    private presupuestoService: PresupuestoService,
    private clienteService: ClienteService
  ) { }

  ngOnInit(): void {
    this.clienteService.listar().subscribe(data => this.clientes.set(data));
    this.cargarPresupuestos();
  }

  cargarPresupuestos(): void {
    this.cargando.set(true);
    this.presupuestoService.listar().subscribe({
      next: (data) => {
        this.presupuestos.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los presupuestos');
        this.cargando.set(false);
      }
    });
  }

  presupuestoVacio(): PresupuestoRequest {
    return {
      clienteId: 0,
      lineas: [this.lineaVacia()],
      descuentoPorcentaje: 0,
      fechaEmision: new Date().toISOString().split('T')[0]
    };
  }

  lineaVacia(): LineaPresupuestoRequest {
    return { concepto: '', horas: 1, precioHora: 15 };
  }

  agregarLinea(): void {
    this.formPresupuesto.lineas.push(this.lineaVacia());
  }

  abrirModalCrear(): void {
    this.formPresupuesto = this.presupuestoVacio();
    this.modalAbierto.set(true);
  }

  quitarLinea(index: number): void {
    if (this.formPresupuesto.lineas.length === 1) return;
    this.formPresupuesto.lineas.splice(index, 1);
  }

  subtotalLinea(linea: LineaPresupuestoRequest): number {
    return (linea.horas || 0) * (linea.precioHora || 0);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  subtotalPreview(): number {
    return this.formPresupuesto.lineas.reduce((acc, l) => acc + this.subtotalLinea(l), 0);
  }

  descuentoPreview(): number {
    return this.subtotalPreview() * (this.formPresupuesto.descuentoPorcentaje / 100);
  }

  totalPreview(): number {
    return this.subtotalPreview() - this.descuentoPreview();
  }

  guardarPresupuesto(): void {
    this.presupuestoService.crear(this.formPresupuesto).subscribe({
      next: (resultado) => {
        this.cerrarModal();
        this.mensajeExito.set(`Presupuesto ${resultado.numero} creado por ${resultado.total.toFixed(2)} €`);
        this.cargarPresupuestos();
        setTimeout(() => this.mensajeExito.set(null), 5000);
      },
      error: () => this.error.set('No se pudo crear el presupuesto')
    });
  }

  cambiarEstado(presupuesto: Presupuesto, nuevoEstado: string): void {
    this.presupuestoService.cambiarEstado(presupuesto.id!, nuevoEstado).subscribe({
      next: () => this.cargarPresupuestos(),
      error: () => this.error.set('No se pudo cambiar el estado')
    });
  }

  descargarPdf(presupuesto: Presupuesto): void {
    this.presupuestoService.descargarPdf(presupuesto.id!, presupuesto.numero);
  }

  eliminarPresupuesto(id: number): void {
    if (!confirm('¿Seguro que quieres eliminar este presupuesto?')) return;
    this.presupuestoService.eliminar(id).subscribe({
      next: () => this.cargarPresupuestos(),
      error: () => this.error.set('No se pudo eliminar el presupuesto')
    });
  }
}