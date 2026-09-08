import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FacturaService } from '../../core/services/factura.service';
import { ClienteService } from '../../core/services/cliente.service';
import { Factura } from '../../core/models/factura.model';
import { Cliente } from '../../core/models/cliente.model';

@Component({
  selector: 'app-facturas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './facturas.component.html',
  styleUrl: './facturas.component.css'
})
export class FacturasComponent implements OnInit {

  facturas = signal<Factura[]>([]);
  clientes = signal<Cliente[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);

  filtroClienteId = 0;

  constructor(
    private facturaService: FacturaService,
    private clienteService: ClienteService
  ) {}

  ngOnInit(): void {
    this.clienteService.listar().subscribe(data => this.clientes.set(data));
    this.cargarFacturas();
  }

  cargarFacturas(): void {
    this.cargando.set(true);
    const id = this.filtroClienteId > 0 ? this.filtroClienteId : undefined;
    this.facturaService.listar(id).subscribe({
      next: (data) => {
        this.facturas.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las facturas');
        this.cargando.set(false);
      }
    });
  }

  marcarComoPagada(id: number): void {
    this.facturaService.marcarComoPagada(id).subscribe({
      next: () => this.cargarFacturas(),
      error: () => this.error.set('No se pudo actualizar la factura')
    });
  }

  eliminarFactura(id: number): void {
    if (!confirm('¿Seguro que quieres eliminar esta factura? Los servicios volverán a quedar sin facturar.')) return;

    this.facturaService.eliminar(id).subscribe({
      next: () => this.cargarFacturas(),
      error: () => this.error.set('No se pudo eliminar la factura')
    });
  }
}