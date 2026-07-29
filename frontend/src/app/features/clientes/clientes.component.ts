import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClienteService } from '../../core/services/cliente.service';
import { Cliente, TipoCliente } from '../../core/models/cliente.model';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.css'
})
export class ClientesComponent implements OnInit {

  clientes = signal<Cliente[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);

  modalAbierto = signal(false);
  clienteEnEdicion: Cliente | null = null;
  formCliente: Cliente = this.clienteVacio();

  constructor(private clienteService: ClienteService) {}

  ngOnInit(): void {
    this.cargarClientes();
  }

  cargarClientes(): void {
    this.cargando.set(true);
    this.clienteService.listar().subscribe({
      next: (data) => {
        this.clientes.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los clientes');
        this.cargando.set(false);
      }
    });
  }

  clienteVacio(): Cliente {
    return { nombre: '', telefono: '', email: '', direccion: '', tipo: 'PARTICULAR' as TipoCliente };
  }

  abrirModalCrear(): void {
    this.clienteEnEdicion = null;
    this.formCliente = this.clienteVacio();
    this.modalAbierto.set(true);
  }

  abrirModalEditar(cliente: Cliente): void {
    this.clienteEnEdicion = cliente;
    this.formCliente = { ...cliente };
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  guardarCliente(): void {
    if (this.clienteEnEdicion) {
      this.clienteService.actualizar(this.clienteEnEdicion.id!, this.formCliente).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarClientes();
        },
        error: () => this.error.set('No se pudo actualizar el cliente')
      });
    } else {
      this.clienteService.crear(this.formCliente).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarClientes();
        },
        error: () => this.error.set('No se pudo crear el cliente')
      });
    }
  }

  eliminarCliente(id: number): void {
    if (!confirm('¿Seguro que quieres eliminar este cliente?')) return;

    this.clienteService.eliminar(id).subscribe({
      next: () => this.cargarClientes(),
      error: () => this.error.set('No se pudo eliminar el cliente')
    });
  }
}