import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FacturaService } from '../../core/services/factura.service';
import { FacturacionService } from '../../core/services/facturacion.service';
import { ClienteService } from '../../core/services/cliente.service';
import { Factura, ServicioPendiente } from '../../core/models/factura.model';
import { Cliente } from '../../core/models/cliente.model';
import { PaginacionComponent } from '../../shared/components/paginacion/paginacion.component';

@Component({
  selector: 'app-facturas',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginacionComponent],
  templateUrl: './facturas.component.html',
  styleUrl: './facturas.component.css'
})
export class FacturasComponent implements OnInit {

  facturas = signal<Factura[]>([]);
  clientes = signal<Cliente[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  mensajeExito = signal<string | null>(null);

  paginaActual = signal(1);
  itemsPorPagina = 10;

  ordenadas = computed(() =>
    [...this.facturas()].sort((a, b) =>
      b.fechaEmision.localeCompare(a.fechaEmision) || (b.id ?? 0) - (a.id ?? 0))
  );

  totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.ordenadas().length / this.itemsPorPagina))
  );

  paginaEfectiva = computed(() => Math.min(this.paginaActual(), this.totalPaginas()));

  paginadas = computed(() => {
    const inicio = (this.paginaEfectiva() - 1) * this.itemsPorPagina;
    return this.ordenadas().slice(inicio, inicio + this.itemsPorPagina);
  });

  // ---------- Modal "Nueva factura" ----------

  modalAbierto = signal(false);
  errorModal = signal<string | null>(null);

  clienteId = signal(0);
  desde = signal('');
  hasta = signal('');
  concepto = signal('Servicios de limpieza');
  descuento = signal(0);
  fechaEmision = signal('');

  pendientes = signal<ServicioPendiente[]>([]);
  seleccionados = signal<Set<number>>(new Set());
  buscando = signal(false);
  buscado = signal(false);
  generando = signal(false);

  private busquedaId = 0;

  descuentoPct = computed(() => Math.min(100, Math.max(0, this.descuento() || 0)));

  subtotal = computed(() =>
    this.pendientes()
      .filter(s => this.seleccionados().has(s.id))
      .reduce((acc, s) => acc + s.importe, 0)
  );
  descuentoImporte = computed(() => this.subtotal() * (this.descuentoPct() / 100));
  baseImponible = computed(() => this.subtotal() - this.descuentoImporte());
  iva = computed(() => this.baseImponible() * 0.21);
  total = computed(() => this.baseImponible() + this.iva());

  todosSeleccionados = computed(() =>
    this.pendientes().length > 0 && this.seleccionados().size === this.pendientes().length
  );

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
    this.facturaService.listar().subscribe({
      next: (data) => {
        this.facturas.set(data);
        this.error.set(null);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las facturas');
        this.cargando.set(false);
      }
    });
  }

  cambiarPagina(pagina: number): void {
    this.paginaActual.set(pagina);
  }

  // ---------- Acciones de la lista ----------

  descargarPdf(f: Factura): void {
    this.facturaService.descargarPdf(f.id!, f.numero);
  }

  eliminar(f: Factura): void {
    if (!confirm(`¿Eliminar la factura ${f.numero}? Sus servicios volverán a quedar sin facturar.`)) return;

    this.facturaService.eliminar(f.id!).subscribe({
      next: () => this.cargarFacturas(),
      error: () => this.error.set('No se pudo eliminar la factura')
    });
  }

  // ---------- Modal ----------

  abrirModal(): void {
    const hoy = new Date();
    this.clienteId.set(0);
    this.desde.set(this.aIso(new Date(hoy.getFullYear(), hoy.getMonth(), 1)));
    this.hasta.set(this.aIso(new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0)));
    this.concepto.set('Servicios de limpieza');
    this.descuento.set(0);
    this.fechaEmision.set(this.aIso(hoy));
    this.pendientes.set([]);
    this.seleccionados.set(new Set());
    this.buscado.set(false);
    this.errorModal.set(null);
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.busquedaId++;
    this.modalAbierto.set(false);
  }

  cambiarCliente(valor: number): void {
    this.clienteId.set(valor);
    this.buscar();
  }

  cambiarDesde(valor: string): void {
    this.desde.set(valor);
    this.buscar();
  }

  cambiarHasta(valor: string): void {
    this.hasta.set(valor);
    this.buscar();
  }

  private buscar(): void {
    const cliente = this.clienteId();
    const d = this.desde();
    const h = this.hasta();
    const id = ++this.busquedaId;

    this.pendientes.set([]);
    this.seleccionados.set(new Set());
    this.buscado.set(false);

    if (!cliente || !d || !h) return;
    if (h < d) {
      this.errorModal.set('"Hasta" tiene que ser igual o posterior a "Desde".');
      return;
    }

    this.errorModal.set(null);
    this.buscando.set(true);

    this.facturacionService.obtenerPendientes(cliente, d, h).subscribe({
      next: (r) => {
        if (id !== this.busquedaId) return;
        const lista = [...r.servicios].sort((a, b) =>
          (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio));
        this.pendientes.set(lista);
        this.seleccionados.set(new Set(lista.map(s => s.id)));
        this.buscado.set(true);
        this.buscando.set(false);
      },
      error: () => {
        if (id !== this.busquedaId) return;
        this.errorModal.set('No se pudieron buscar los servicios.');
        this.buscando.set(false);
      }
    });
  }

  toggleServicio(id: number): void {
    const nuevo = new Set(this.seleccionados());
    if (nuevo.has(id)) {
      nuevo.delete(id);
    } else {
      nuevo.add(id);
    }
    this.seleccionados.set(nuevo);
  }

  toggleTodos(): void {
    this.seleccionados.set(
      this.todosSeleccionados() ? new Set() : new Set(this.pendientes().map(s => s.id))
    );
  }

  generar(): void {
    const ids = Array.from(this.seleccionados());

    if (!this.clienteId()) { this.errorModal.set('Selecciona un cliente.'); return; }
    if (ids.length === 0) { this.errorModal.set('Marca al menos un servicio.'); return; }
    if (!this.fechaEmision()) { this.errorModal.set('Elige la fecha de emisión.'); return; }

    this.errorModal.set(null);
    this.generando.set(true);

    this.facturacionService.generar({
      clienteId: this.clienteId(),
      servicioIds: ids,
      concepto: this.concepto().trim() || undefined,
      descuentoPorcentaje: this.descuentoPct(),
      fechaEmision: this.fechaEmision()
    }).subscribe({
      next: (r) => {
        const importe = r.importe.toLocaleString('es-ES', {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2
        });
        this.generando.set(false);
        this.cerrarModal();
        this.mensajeExito.set(`Factura ${r.numero} creada por ${importe} €.`);
        setTimeout(() => this.mensajeExito.set(null), 6000);
        this.cargarFacturas();
      },
      error: () => {
        this.generando.set(false);
        this.errorModal.set('No se pudo crear la factura. Revisa los datos.');
      }
    });
  }

  // ---------- Presentación ----------

  fechaCorta(iso: string): string {
    const [y, m, d] = iso.split('-').map(Number);
    const mes = new Date(y, m - 1, d).toLocaleDateString('es-ES', { month: 'short' }).replace('.', '');
    return `${d} ${mes} ${y}`;
  }

  conceptoTexto(f: Factura): string {
    return f.concepto?.trim() || 'Servicios de limpieza';
  }

  private aIso(d: Date): string {
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mes}-${dia}`;
  }
}