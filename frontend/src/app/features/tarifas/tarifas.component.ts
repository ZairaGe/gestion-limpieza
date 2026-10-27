import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TarifaService } from '../../core/services/tarifa.service';
import { Tarifa } from '../../core/models/tarifa.model';

interface FormTarifa {
  nombre: string;
  descripcion: string;
  precio: number | null;
  cobro: 'HORA' | 'SERVICIO';
}

@Component({
  selector: 'app-tarifas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tarifas.component.html',
  styleUrl: './tarifas.component.css'
})
export class TarifasComponent implements OnInit {

  tarifas = signal<Tarifa[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  errorModal = signal<string | null>(null);

  modalAbierto = signal(false);
  tarifaEnEdicion: Tarifa | null = null;
  form: FormTarifa = this.formVacio();

  constructor(private tarifaService: TarifaService) { }

  ngOnInit(): void {
    this.cargarTarifas();
  }

  cargarTarifas(): void {
    this.cargando.set(true);
    this.tarifaService.listar().subscribe({
      next: (data) => {
        const ordenadas = [...data].sort((a, b) =>
          (a.nombre ?? '').localeCompare(b.nombre ?? '', 'es'));
        this.tarifas.set(ordenadas);
        this.error.set(null);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las tarifas');
        this.cargando.set(false);
      }
    });
  }

  precioDe(t: Tarifa): number {
    return t.precioFijo ?? t.precioHora ?? 0;
  }

  unidadDe(t: Tarifa): string {
    return t.precioFijo != null ? 'servicio' : 'hora';
  }

  formVacio(): FormTarifa {
    return { nombre: '', descripcion: '', precio: null, cobro: 'HORA' };
  }

  abrirModalCrear(): void {
    this.tarifaEnEdicion = null;
    this.form = this.formVacio();
    this.errorModal.set(null);
    this.modalAbierto.set(true);
  }

  abrirModalEditar(t: Tarifa): void {
    this.tarifaEnEdicion = t;
    this.form = {
      nombre: t.nombre ?? '',
      descripcion: t.descripcion ?? '',
      precio: t.precioFijo ?? t.precioHora ?? null,
      cobro: t.precioFijo != null ? 'SERVICIO' : 'HORA'
    };
    this.errorModal.set(null);
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  guardar(): void {
    const f = this.form;

    if (!f.nombre.trim()) {
      this.errorModal.set('Escribe el nombre de la tarifa.');
      return;
    }
    if (f.precio == null || f.precio <= 0) {
      this.errorModal.set('El precio tiene que ser mayor que 0.');
      return;
    }

    const tarifa: Tarifa = {
      nombre: f.nombre.trim(),
      descripcion: f.descripcion.trim() || null,
      precioHora: f.cobro === 'HORA' ? f.precio : null,
      precioFijo: f.cobro === 'SERVICIO' ? f.precio : null
    };

    this.errorModal.set(null);

    const peticion = this.tarifaEnEdicion
      ? this.tarifaService.actualizar(this.tarifaEnEdicion.id!, tarifa)
      : this.tarifaService.crear(tarifa);

    peticion.subscribe({
      next: () => {
        this.cerrarModal();
        this.cargarTarifas();
      },
      error: () => this.errorModal.set('No se pudo guardar la tarifa. Revisa los datos.')
    });
  }

  eliminar(t: Tarifa): void {
    if (!confirm(`¿Eliminar la tarifa "${t.nombre}"?`)) return;

    this.tarifaService.eliminar(t.id!).subscribe({
      next: () => this.cargarTarifas(),
      error: () => this.error.set(
        'No se pudo eliminar la tarifa. Si hay servicios que la usan, cámbiales la tarifa primero.')
    });
  }
}