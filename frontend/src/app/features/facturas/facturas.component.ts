import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FacturaService } from '../../core/services/factura.service';
import { FacturacionService } from '../../core/services/facturacion.service';
import { ClienteService } from '../../core/services/cliente.service';
import { Factura, ServicioPendiente } from '../../core/models/factura.model';
import { Cliente } from '../../core/models/cliente.model';

@Component({
  selector: 'app-facturas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './facturas.component.html',
  styleUrl: './facturas.component.css'
})
export class FacturasComponent implements OnInit {

  clientes = signal<Cliente[]>([]);
  facturas = signal<Factura[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  mensajeExito = signal<string | null>(null);

  filtroClienteId = 0;

  descuentoPorcentaje = 0;

  mostrarGeneracion = signal(false);
  clienteIdGen = 0;
  desdeGen = '';
  hastaGen = '';
  fechaEmision = new Date().toISOString().split('T')[0];
  numeroManual = '';
  serviciosPendientes = signal<ServicioPendiente[]>([]);
  seleccionados = new Set<number>();
  buscandoPendientes = signal(false);
  generando = signal(false);

  constructor(
    private facturaService: FacturaService,
    private facturacionService: FacturacionService,
    private clienteService: ClienteService
  ) { }

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

  descargarPdf(factura: Factura): void {
    this.facturaService.descargarPdf(factura.id!, factura.numero);
  }

  toggleGeneracion(): void {
    this.mostrarGeneracion.set(!this.mostrarGeneracion());
  }

  buscarPendientes(): void {
    if (!this.clienteIdGen || !this.desdeGen || !this.hastaGen) return;

    this.buscandoPendientes.set(true);
    this.seleccionados.clear();
    this.facturacionService.obtenerPendientes(this.clienteIdGen, this.desdeGen, this.hastaGen).subscribe({
      next: (resultado) => {
        this.serviciosPendientes.set(resultado.servicios);
        this.buscandoPendientes.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los servicios pendientes');
        this.buscandoPendientes.set(false);
      }
    });
  }

  toggleSeleccion(id: number): void {
    if (this.seleccionados.has(id)) {
      this.seleccionados.delete(id);
    } else {
      this.seleccionados.add(id);
    }
  }

  estaSeleccionado(id: number): boolean {
    return this.seleccionados.has(id);
  }

  totalSeleccionado(): number {
    return this.serviciosPendientes()
      .filter(s => this.seleccionados.has(s.id))
      .reduce((acc, s) => acc + s.importe, 0);
  }

  generarFactura(): void {
    if (this.seleccionados.size === 0) return;

    this.generando.set(true);
    this.facturacionService.generar({
      clienteId: this.clienteIdGen,
      servicioIds: Array.from(this.seleccionados),
      numero: this.numeroManual || undefined,
      descuentoPorcentaje: this.descuentoPorcentaje,
      fechaEmision: this.fechaEmision
    }).subscribe({
      next: (resultado) => {
        this.mensajeExito.set(`Factura ${resultado.numero} generada por ${resultado.importe.toFixed(2)} €`);
        this.serviciosPendientes.set([]);
        this.seleccionados.clear();
        this.generando.set(false);
        this.mostrarGeneracion.set(false);
        this.cargarFacturas();
        setTimeout(() => this.mensajeExito.set(null), 6000);
      },
      error: (err) => {
        this.error.set(err.error?.error ?? 'No se pudo generar la factura');
        this.generando.set(false);
      }
    });
  }
  subtotalPreview(): number {
    return this.totalSeleccionado();
  }

  descuentoPreview(): number {
    return this.subtotalPreview() * (this.descuentoPorcentaje / 100);
  }

  baseImponiblePreview(): number {
    return this.subtotalPreview() - this.descuentoPreview();
  }

  ivaPreview(): number {
    return this.baseImponiblePreview() * 0.21;
  }

  totalPreview(): number {
    return this.baseImponiblePreview() + this.ivaPreview();
  }
}