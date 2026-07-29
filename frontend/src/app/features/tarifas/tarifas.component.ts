import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TarifaService } from '../../core/services/tarifa.service';
import { Tarifa, TipoServicio, Zona } from '../../core/models/tarifa.model';

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

  modalAbierto = signal(false);
  tarifaEnEdicion: Tarifa | null = null;
  formTarifa: Tarifa = this.tarifaVacia();

  constructor(private tarifaService: TarifaService) {}

  ngOnInit(): void {
    this.cargarTarifas();
  }

  cargarTarifas(): void {
    this.cargando.set(true);
    this.tarifaService.listar().subscribe({
      next: (data) => {
        this.tarifas.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las tarifas');
        this.cargando.set(false);
      }
    });
  }

  tarifaVacia(): Tarifa {
    return {
      tipoServicio: 'CASA' as TipoServicio,
      zona: 'MADRID' as Zona,
      precioHora: undefined,
      precioFijo: undefined
    };
  }

  abrirModalCrear(): void {
    this.tarifaEnEdicion = null;
    this.formTarifa = this.tarifaVacia();
    this.modalAbierto.set(true);
  }

  abrirModalEditar(tarifa: Tarifa): void {
    this.tarifaEnEdicion = tarifa;
    this.formTarifa = { ...tarifa };
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  guardarTarifa(): void {
    if (this.tarifaEnEdicion) {
      this.tarifaService.actualizar(this.tarifaEnEdicion.id!, this.formTarifa).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarTarifas();
        },
        error: () => this.error.set('No se pudo actualizar la tarifa')
      });
    } else {
      this.tarifaService.crear(this.formTarifa).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarTarifas();
        },
        error: () => this.error.set('No se pudo crear la tarifa')
      });
    }
  }

  eliminarTarifa(id: number): void {
    if (!confirm('¿Seguro que quieres eliminar esta tarifa?')) return;

    this.tarifaService.eliminar(id).subscribe({
      next: () => this.cargarTarifas(),
      error: () => this.error.set('No se pudo eliminar la tarifa')
    });
  }
}