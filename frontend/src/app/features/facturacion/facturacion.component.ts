import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FacturacionService } from '../../core/services/facturacion.service';
import { ClienteService } from '../../core/services/cliente.service';
import { ServicioPendiente } from '../../core/models/factura.model';
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
  serviciosPendientes = signal<ServicioPendiente[]>([]);
  totalImporte = signal<number>(0);

  clienteId = 0;
  desde = '';
  hasta = '';
  fechaEmision = new Date().toISOString().split('T')[0];

  buscando = signal(false);
  generando = signal(false);
  mensajeExito = signal<string | null>(null);
  error = signal<string | null>(null);

  constructor(
    private facturacionService: FacturacionService,
    private clienteService: ClienteService
  ) {}

  ngOnInit(): void {
    this.clienteService.listar().subscribe(data => this.clientes.set(data));
  }

  buscarPendientes(): void {
    if (!this.clienteId || !this.desde || !this.hasta) return;

    this.buscando.set(true);
    this.error.set(null);
    this.facturacionService.obtenerPendientes(this.clienteId, this.desde, this.hasta).subscribe({
      next: (resultado) => {
        this.serviciosPendientes.set(resultado.servicios);
        this.totalImporte.set(resultado.totalImporte);
        this.buscando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los servicios pendientes');
        this.buscando.set(false);
      }
    });
  }

  generarFactura(): void {
    this.generando.set(true);
    this.error.set(null);

    this.facturacionService.generar({
      clienteId: this.clienteId,
      desde: this.desde,
      hasta: this.hasta,
      fechaEmision: this.fechaEmision
    }).subscribe({
      next: (resultado) => {
        this.mensajeExito.set(`Factura ${resultado.numero} generada por ${resultado.importe.toFixed(2)} €`);
        this.serviciosPendientes.set([]);
        this.totalImporte.set(0);
        this.generando.set(false);
        setTimeout(() => this.mensajeExito.set(null), 6000);
      },
      error: (err) => {
        this.error.set(err.error?.error ?? 'No se pudo generar la factura');
        this.generando.set(false);
      }
    });
  }
}