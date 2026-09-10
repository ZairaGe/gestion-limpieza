import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ServicioService } from '../../core/services/servicio.service';
import { ClienteService } from '../../core/services/cliente.service';
import { Servicio } from '../../core/models/servicio.model';
import { Cliente } from '../../core/models/cliente.model';

@Component({
  selector: 'app-facturacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './facturacion.component.html',
  styleUrl: './facturacion.component.css'
})
export class FacturacionComponent implements OnInit {

  clientes = signal<Cliente[]>([]);
  servicios = signal<Servicio[]>([]);
  cargando = signal(false);
  error = signal<string | null>(null);

  clienteId = 0;
  desde = '';
  hasta = '';

  constructor(
    private servicioService: ServicioService,
    private clienteService: ClienteService
  ) {}

  ngOnInit(): void {
    this.clienteService.listar().subscribe(data => this.clientes.set(data));
  }

  buscar(): void {
    if (!this.clienteId || !this.desde || !this.hasta) return;

    this.cargando.set(true);
    this.error.set(null);
    this.servicioService.listarPorClienteYRango(this.clienteId, this.desde, this.hasta).subscribe({
      next: (data) => {
        this.servicios.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los servicios');
        this.cargando.set(false);
      }
    });
  }

  togglePago(servicio: Servicio): void {
    const accion = servicio.pagado
      ? this.servicioService.marcarPendiente(servicio.id!)
      : this.servicioService.marcarPagado(servicio.id!);

    accion.subscribe({
      next: () => this.buscar(),
      error: () => this.error.set('No se pudo actualizar el estado de pago')
    });
  }

  totalImporte(): number {
    return this.servicios().reduce((acc, s) => acc + (s.importe ?? 0), 0);
  }

  totalPagado(): number {
    return this.servicios().filter(s => s.pagado).reduce((acc, s) => acc + (s.importe ?? 0), 0);
  }

  totalPendiente(): number {
    return this.servicios().filter(s => !s.pagado).reduce((acc, s) => acc + (s.importe ?? 0), 0);
  }
}