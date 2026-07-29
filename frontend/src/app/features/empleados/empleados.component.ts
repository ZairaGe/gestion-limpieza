import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmpleadoService } from '../../core/services/empleado.service';
import { Empleado } from '../../core/models/empleado.model';

@Component({
  selector: 'app-empleados',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './empleados.component.html',
  styleUrl: './empleados.component.css'
})
export class EmpleadosComponent implements OnInit {

  empleados = signal<Empleado[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);

  modalAbierto = signal(false);
  empleadoEnEdicion: Empleado | null = null;
  formEmpleado: Empleado = this.empleadoVacio();

  constructor(private empleadoService: EmpleadoService) {}

  ngOnInit(): void {
    this.cargarEmpleados();
  }

  cargarEmpleados(): void {
    this.cargando.set(true);
    this.empleadoService.listar().subscribe({
      next: (data) => {
        this.empleados.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los empleados');
        this.cargando.set(false);
      }
    });
  }

  empleadoVacio(): Empleado {
    return { nombre: '', telefono: '', activo: true };
  }

  abrirModalCrear(): void {
    this.empleadoEnEdicion = null;
    this.formEmpleado = this.empleadoVacio();
    this.modalAbierto.set(true);
  }

  abrirModalEditar(empleado: Empleado): void {
    this.empleadoEnEdicion = empleado;
    this.formEmpleado = { ...empleado };
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  guardarEmpleado(): void {
    if (this.empleadoEnEdicion) {
      this.empleadoService.actualizar(this.empleadoEnEdicion.id!, this.formEmpleado).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarEmpleados();
        },
        error: () => this.error.set('No se pudo actualizar el empleado')
      });
    } else {
      this.empleadoService.crear(this.formEmpleado).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarEmpleados();
        },
        error: () => this.error.set('No se pudo crear el empleado')
      });
    }
  }

  eliminarEmpleado(id: number): void {
    if (!confirm('¿Seguro que quieres eliminar este empleado?')) return;

    this.empleadoService.eliminar(id).subscribe({
      next: () => this.cargarEmpleados(),
      error: () => this.error.set('No se pudo eliminar el empleado')
    });
  }
}