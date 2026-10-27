import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PresupuestoService } from '../../core/services/presupuesto.service';
import { TarifaService } from '../../core/services/tarifa.service';
import { Presupuesto, PresupuestoRequest } from '../../core/models/presupuesto.model';
import { Tarifa } from '../../core/models/tarifa.model';
import { PaginacionComponent } from '../../shared/components/paginacion/paginacion.component';

interface LineaForm {
  concepto: string;
  horas: number | null;
  precioHora: number | null;
}

const MAX_LINEAS = 10;

@Component({
  selector: 'app-presupuestos',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginacionComponent],
  templateUrl: './presupuestos.component.html',
  styleUrl: './presupuestos.component.css'
})
export class PresupuestosComponent implements OnInit {

  readonly maxLineas = MAX_LINEAS;

  presupuestos = signal<Presupuesto[]>([]);
  tarifas = signal<Tarifa[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  mensajeExito = signal<string | null>(null);

  paginaActual = signal(1);
  itemsPorPagina = 10;

  ordenados = computed(() =>
    [...this.presupuestos()].sort((a, b) =>
      b.fechaEmision.localeCompare(a.fechaEmision) || (b.id ?? 0) - (a.id ?? 0))
  );

  totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.ordenados().length / this.itemsPorPagina))
  );

  paginaEfectiva = computed(() => Math.min(this.paginaActual(), this.totalPaginas()));

  paginados = computed(() => {
    const inicio = (this.paginaEfectiva() - 1) * this.itemsPorPagina;
    return this.ordenados().slice(inicio, inicio + this.itemsPorPagina);
  });

  // ---------- Modal ----------

  modalAbierto = signal(false);
  errorModal = signal<string | null>(null);
  guardando = signal(false);
  presupuestoEditando = signal<Presupuesto | null>(null);

  form = this.formVacio();
  lineas: LineaForm[] = [];

  constructor(
    private presupuestoService: PresupuestoService,
    private tarifaService: TarifaService
  ) { }

  ngOnInit(): void {
    this.tarifaService.listar().subscribe(data =>
      this.tarifas.set([...data].sort((a, b) => (a.nombre ?? '').localeCompare(b.nombre ?? '', 'es'))));
    this.cargarPresupuestos();
  }

  cargarPresupuestos(): void {
    this.presupuestoService.listar().subscribe({
      next: (data) => {
        this.presupuestos.set(data);
        this.error.set(null);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los presupuestos');
        this.cargando.set(false);
      }
    });
  }

  cambiarPagina(pagina: number): void {
    this.paginaActual.set(pagina);
  }

  // ---------- Lista ----------

  descargarPdf(p: Presupuesto): void {
    this.presupuestoService.descargarPdf(p.id!, p.numero);
  }

  eliminar(p: Presupuesto): void {
    if (!confirm(`¿Eliminar el presupuesto ${p.numero}?`)) return;

    this.presupuestoService.eliminar(p.id!).subscribe({
      next: () => this.cargarPresupuestos(),
      error: () => this.error.set('No se pudo eliminar el presupuesto')
    });
  }

  conceptoTexto(p: Presupuesto): string {
    if (p.lineas.length === 0) return '—';
    const extra = p.lineas.length > 1 ? ` +${p.lineas.length - 1} más` : '';
    return p.lineas[0].concepto + extra;
  }

  fechaCorta(iso: string): string {
    const [y, m, d] = iso.split('-').map(Number);
    const mes = new Date(y, m - 1, d).toLocaleDateString('es-ES', { month: 'short' }).replace('.', '');
    return `${d} ${mes} ${y}`;
  }

  // ---------- Formulario ----------

  private formVacio() {
    return {
      nombre: '',
      telefono: '',
      correo: '',
      direccion: '',
      notas: '',
      iva: 21 as number | null,
      descuento: 0 as number | null,
      fecha: this.aIso(new Date())
    };
  }

  private lineaVacia(): LineaForm {
    return { concepto: '', horas: 1, precioHora: null };
  }

  abrirModal(): void {
    this.presupuestoEditando.set(null);
    this.form = this.formVacio();
    this.lineas = [this.lineaVacia()];
    this.errorModal.set(null);
    this.modalAbierto.set(true);
  }

  editar(p: Presupuesto): void {
    this.presupuestoService.buscarPorId(p.id!).subscribe({
      next: (presupuesto) => {
        this.presupuestoEditando.set(presupuesto);

        this.form = {
          nombre: presupuesto.nombre ?? '',
          telefono: presupuesto.telefono ?? '',
          correo: presupuesto.correo ?? '',
          direccion: presupuesto.direccion ?? '',
          notas: presupuesto.notas ?? '',
          iva: presupuesto.ivaPorcentaje,
          descuento: presupuesto.descuentoPorcentaje,
          fecha: presupuesto.fechaEmision
        };

        this.lineas = presupuesto.lineas.map(l => ({
          concepto: l.concepto,
          horas: l.horas,
          precioHora: l.precioHora
        }));

        if (this.lineas.length === 0) {
          this.lineas = [this.lineaVacia()];
        }

        this.errorModal.set(null);
        this.modalAbierto.set(true);
      },
      error: () => {
        this.error.set('No se pudo cargar el presupuesto para editarlo.');
      }
    });
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  agregarLinea(): void {
    if (this.lineas.length < MAX_LINEAS) this.lineas.push(this.lineaVacia());
  }

  quitarLinea(linea: LineaForm): void {
    if (this.lineas.length === 1) {
      this.lineas[0] = this.lineaVacia();
      return;
    }
    this.lineas = this.lineas.filter(l => l !== linea);
  }

  etiquetaTarifa(t: Tarifa): string {
    return `${t.nombre} · ${this.numeroLibre(this.precioDe(t))}€/${this.unidadDe(t)}`;
  }

  elegirTarifa(linea: LineaForm, evento: Event): void {
    const select = evento.target as HTMLSelectElement;
    const tarifa = this.tarifas().find(t => t.id === Number(select.value));
    select.value = '';
    if (!tarifa) return;

    linea.concepto = `${tarifa.nombre} (${this.unidadDe(tarifa)})`;
    linea.precioHora = this.precioDe(tarifa);
    if (!linea.horas) linea.horas = 1;
  }

  private precioDe(t: Tarifa): number {
    return t.precioFijo ?? t.precioHora ?? 0;
  }

  private unidadDe(t: Tarifa): string {
    return t.precioFijo != null ? 'servicio' : 'hora';
  }

  private numeroLibre(v: number): string {
    const entero = Math.abs(v - Math.round(v)) < 0.005;
    return v.toLocaleString('es-ES', {
      minimumFractionDigits: entero ? 0 : 2,
      maximumFractionDigits: entero ? 0 : 2
    });
  }

  // ---------- Cálculo en vivo (igual que el del backend) ----------

  private num(v: number | null | undefined): number {
    return Number(v) || 0;
  }

  lineasValidas(): LineaForm[] {
    return this.lineas.filter(l => l.concepto.trim() !== '');
  }

  subtotalLinea(l: LineaForm): number {
    return this.num(l.horas) * this.num(l.precioHora);
  }

  subtotal(): number {
    return this.lineasValidas().reduce((acc, l) => acc + this.subtotalLinea(l), 0);
  }

  descuentoPct(): number {
    return Math.min(100, Math.max(0, this.num(this.form.descuento)));
  }

  ivaPct(): number {
    return this.form.iva === null ? 21 : Math.min(100, Math.max(0, this.num(this.form.iva)));
  }

  descuentoImporte(): number {
    return this.subtotal() * (this.descuentoPct() / 100);
  }

  baseImponible(): number {
    return this.subtotal() - this.descuentoImporte();
  }

  ivaImporte(): number {
    return this.baseImponible() * (this.ivaPct() / 100);
  }

  total(): number {
    return this.baseImponible() + this.ivaImporte();
  }

  // ---------- Guardar ----------

  guardar(): void {
    const f = this.form;
    const lineas = this.lineasValidas();

    if (!f.nombre.trim()) { this.errorModal.set('Escribe el nombre.'); return; }
    if (lineas.length === 0) { this.errorModal.set('Añade al menos un concepto.'); return; }

    const mala = lineas.find(l => !(this.num(l.horas) > 0) || !(this.num(l.precioHora) > 0));
    if (mala) {
      this.errorModal.set(`Revisa la cantidad y el precio de "${mala.concepto.trim()}".`);
      return;
    }
    if (!f.fecha) { this.errorModal.set('Elige la fecha de emisión.'); return; }

    const request: PresupuestoRequest = {
      nombre: f.nombre.trim(),
      telefono: f.telefono.trim() || null,
      correo: f.correo.trim() || null,
      direccion: f.direccion.trim() || null,
      lineas: lineas.map(l => ({
        concepto: l.concepto.trim(),
        horas: Number(l.horas),
        precioHora: Number(l.precioHora)
      })),
      notas: f.notas.trim() || null,
      descuentoPorcentaje: this.descuentoPct(),
      ivaPorcentaje: this.ivaPct(),
      fechaEmision: f.fecha
    };

    this.errorModal.set(null);
    this.guardando.set(true);

    const presupuestoActual = this.presupuestoEditando();

    const peticion = presupuestoActual?.id
      ? this.presupuestoService.actualizar(presupuestoActual.id, request)
      : this.presupuestoService.crear(request);

    peticion.subscribe({
      next: (r) => {
        const total = r.total.toLocaleString('es-ES', {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2
        });

        this.guardando.set(false);
        this.cerrarModal();

        this.mensajeExito.set(
          presupuestoActual
            ? `Presupuesto ${r.numero} actualizado correctamente. Total: ${total} €.`
            : `Presupuesto ${r.numero} creado por ${total} €.`
        );

        setTimeout(() => this.mensajeExito.set(null), 6000);
        this.cargarPresupuestos();
      },
      error: () => {
        this.guardando.set(false);
        this.errorModal.set(
          presupuestoActual
            ? 'No se pudo actualizar el presupuesto. Revisa los datos.'
            : 'No se pudo crear el presupuesto. Revisa los datos.'
        );
      }
    });
  }

  private aIso(d: Date): string {
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mes}-${dia}`;
  }
}