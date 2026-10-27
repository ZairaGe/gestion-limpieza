import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ServicioService } from '../../core/services/servicio.service';
import { FacturacionService } from '../../core/services/facturacion.service';
import { Servicio } from '../../core/models/servicio.model';
import { PaginacionComponent } from '../../shared/components/paginacion/paginacion.component';

type Filtro = 'PENDIENTES' | 'PAGADOS' | 'TODOS';

@Component({
  selector: 'app-facturacion',
  standalone: true,
  imports: [CommonModule, RouterLink, PaginacionComponent],
  templateUrl: './facturacion.component.html',
  styleUrl: './facturacion.component.css'
})
export class FacturacionComponent implements OnInit {

  servicios = signal<Servicio[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  mensajeExito = signal<string | null>(null);
  procesando = signal<number | null>(null);

  filtro = signal<Filtro>('TODOS');
  paginaActual = signal(1);
  itemsPorPagina = 10;

  opcionesFiltro: { valor: Filtro; etiqueta: string }[] = [
    { valor: 'PENDIENTES', etiqueta: 'Pendientes' },
    { valor: 'PAGADOS', etiqueta: 'Pagados' },
    { valor: 'TODOS', etiqueta: 'Todos' }
  ];

  completados = computed(() =>
    this.servicios()
      .filter(s => s.estado === 'COMPLETADO')
      .sort((a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio))
  );

  pendienteCobro = computed(() => this.suma(this.completados().filter(s => !s.pagado)));
  cobrado = computed(() => this.suma(this.completados().filter(s => s.pagado)));

  filtrados = computed(() => {
    const f = this.filtro();
    return this.completados().filter(s =>
      f === 'TODOS' || (f === 'PAGADOS' ? !!s.pagado : !s.pagado)
    );
  });

  totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / this.itemsPorPagina))
  );

  paginaEfectiva = computed(() => Math.min(this.paginaActual(), this.totalPaginas()));

  paginados = computed(() => {
    const inicio = (this.paginaEfectiva() - 1) * this.itemsPorPagina;
    return this.filtrados().slice(inicio, inicio + this.itemsPorPagina);
  });

  constructor(
    private servicioService: ServicioService,
    private facturacionService: FacturacionService
  ) { }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.servicioService.listar().subscribe({
      next: (data) => {
        this.servicios.set(data);
        this.error.set(null);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los servicios');
        this.cargando.set(false);
      }
    });
  }

  cambiarFiltro(valor: Filtro): void {
    this.filtro.set(valor);
    this.paginaActual.set(1);
  }

  cambiarPagina(pagina: number): void {
    this.paginaActual.set(pagina);
  }

  // ---------- Acciones ----------

  togglePago(s: Servicio): void {
    if (this.procesando() !== null) return;
    this.procesando.set(s.id!);

    const accion = s.pagado
      ? this.servicioService.marcarPendiente(s.id!)
      : this.servicioService.marcarPagado(s.id!);

    accion.subscribe({
      next: () => {
        this.procesando.set(null);
        this.cargar();
      },
      error: () => {
        this.procesando.set(null);
        this.error.set('No se pudo actualizar el pago');
      }
    });
  }

  private conceptoFactura(s: Servicio): string {
    const d = this.aFecha(s.fecha);
    return `${this.nombreServicio(s)} · ${this.diaMes(d)} ${d.getFullYear()}`;
  }

  crearFactura(s: Servicio): void {
    if (!s.pagado || s.facturado || this.procesando() !== null) return;

    const importe = (s.importe ?? 0).toLocaleString('es-ES', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    if (!confirm(`¿Crear la factura de ${s.clienteNombre} por ${importe} € (más IVA)?`)) return;

    this.procesando.set(s.id!);
    this.facturacionService.generar({
      clienteId: s.clienteId,
      servicioIds: [s.id!],
      concepto: this.conceptoFactura(s),
      fechaEmision: this.hoy()
    }).subscribe({
      next: (resultado) => {
        this.procesando.set(null);
        this.mensajeExito.set(`Factura ${resultado.numero} creada. La tienes en Facturas.`);
        setTimeout(() => this.mensajeExito.set(null), 6000);
        this.cargar();
      },
      error: () => {
        this.procesando.set(null);
        this.error.set('No se pudo crear la factura');
      }
    });
  }

  // ---------- Presentación ----------

  nombreServicio(s: Servicio): string {
    return s.tarifaNombre ?? 'Servicio de limpieza';
  }

  fechaHora(s: Servicio): string {
    const d = this.aFecha(s.fecha);
    return `${this.diaMes(d)} ${d.getFullYear()} · ${s.horaInicio.substring(0, 5)}`;
  }

  textoPago(s: Servicio): string {
    if (!s.pagado) return 'Pendiente';
    return s.fechaPago ? `Pagado ${this.diaMes(this.aFecha(s.fechaPago))}` : 'Pagado';
  }

  private suma(lista: Servicio[]): number {
    return lista.reduce((acc, s) => acc + (s.importe ?? 0), 0);
  }

  private aFecha(iso: string): Date {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d);
  }

  private diaMes(d: Date): string {
    const mes = d.toLocaleDateString('es-ES', { month: 'short' }).replace('.', '');
    return `${d.getDate()} ${mes}`;
  }

  private hoy(): string {
    const d = new Date();
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mes}-${dia}`;
  }
}