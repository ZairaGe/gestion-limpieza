import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FacturaService } from '../../core/services/factura.service';
import { ServicioService } from '../../core/services/servicio.service';
import { Factura, FacturaRequest } from '../../core/models/factura.model';
import { Servicio } from '../../core/models/servicio.model';

@Component({
  selector: 'app-facturas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './facturas.component.html',
  styleUrl: './facturas.component.css'
})
export class FacturasComponent implements OnInit {

  facturas = signal<Factura[]>([]);
  servicios = signal<Servicio[]>([]);

  cargando = signal(true);
  error = signal<string | null>(null);

  modalAbierto = signal(false);
  formFactura: FacturaRequest = this.facturaVacia();

  constructor(
    private facturaService: FacturaService,
    private servicioService: ServicioService
  ) {}

  ngOnInit(): void {
    this.cargarTodo();
  }

  cargarTodo(): void {
    this.cargando.set(true);
    this.facturaService.listar().subscribe({
      next: (data) => {
        this.facturas.set(data);
        this.servicioService.listar().subscribe({
          next: (servicios) => {
            this.servicios.set(servicios);
            this.cargando.set(false);
          },
          error: () => this.cargando.set(false)
        });
      },
      error: () => {
        this.error.set('No se pudieron cargar las facturas');
        this.cargando.set(false);
      }
    });
  }

  facturaVacia(): FacturaRequest {
    return { servicioId: 0, numero: '', importe: 0, fechaEmision: '' };
  }

  serviciosSinFactura(): Servicio[] {
    const idsFacturados = this.facturas().map(f => f.servicioId);
    return this.servicios().filter(s => !idsFacturados.includes(s.id!));
  }

  abrirModalCrear(): void {
    this.formFactura = this.facturaVacia();
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  guardarFactura(): void {
    this.facturaService.crear(this.formFactura).subscribe({
      next: () => {
        this.cerrarModal();
        this.cargarTodo();
      },
      error: (err) => this.error.set(err.error?.error ?? 'No se pudo crear la factura')
    });
  }

  marcarComoPagada(id: number): void {
    this.facturaService.marcarComoPagada(id).subscribe({
      next: () => this.cargarTodo(),
      error: () => this.error.set('No se pudo actualizar la factura')
    });
  }

  eliminarFactura(id: number): void {
    if (!confirm('¿Seguro que quieres eliminar esta factura?')) return;

    this.facturaService.eliminar(id).subscribe({
      next: () => this.cargarTodo(),
      error: () => this.error.set('No se pudo eliminar la factura')
    });
  }

  nombreServicio(servicioId: number): string {
    const servicio = this.servicios().find(s => s.id === servicioId);
    return servicio ? `${servicio.clienteNombre} — ${servicio.fecha}` : `Servicio #${servicioId}`;
  }
}