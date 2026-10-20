import { Component, OnInit, computed, signal } from '@angular/core';
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
  busqueda = signal('');

  modalAbierto = signal(false);
  clienteEnEdicion: Cliente | null = null;
  formCliente: Cliente = this.clienteVacio();

  constructor(private clienteService: ClienteService) { }

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
    return { nombre: '', telefono: '', email: '', direccion: '', codigoPostal: '', tipo: 'PARTICULAR' as TipoCliente, nif: '' };
  }

  abrirModalCrear(): void {
    this.clienteEnEdicion = null;
    this.formCliente = this.clienteVacio();
    this.formCliente.apellido = '';
    this.modalAbierto.set(true);
  }


  abrirModalEditar(cliente: Cliente): void {
    console.log('Cliente recibido:', cliente);

    this.clienteEnEdicion = cliente;
    this.formCliente = {
      ...cliente,
      apellido: cliente.apellido ?? ''
    };

    console.log('Formulario de edición:', this.formCliente);

    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }


  guardarCliente(): void {
    console.log('Datos que voy a guardar:', this.formCliente);

    if (this.clienteEnEdicion) {
      this.clienteService.actualizar(
        this.clienteEnEdicion.id!,
        this.formCliente
      ).subscribe({
        next: () => {
          console.log('Cliente actualizado correctamente');
          this.cerrarModal();
          this.cargarClientes();
        },
        error: (error) => {
          console.error('Error al actualizar:', error);
          this.error.set('No se pudo actualizar el cliente');
        }
      });
    } else {
      this.clienteService.crear(this.formCliente).subscribe({
        next: () => {
          this.cerrarModal();
          this.cargarClientes();
        },
        error: (error) => {
          console.error('Error al crear:', error);
          this.error.set('No se pudo crear el cliente');
        }
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

  clientesFiltrados = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();

    if (!texto) {
      return this.clientes();
    }

    return this.clientes().filter(cliente =>
      [
        cliente.nombre,
        cliente.apellido,
        cliente.telefono,
        cliente.email,
        cliente.direccion
      ].some(valor =>
        String(valor ?? '').toLowerCase().includes(texto)
      )
    );
  });
}