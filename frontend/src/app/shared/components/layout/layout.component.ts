import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent {

  menuItems = [
    { label: 'Dashboard', ruta: '/dashboard', icono: '⊞' },
    { label: 'Servicios', ruta: '/servicios', icono: '📅' },
    { label: 'Distribución', ruta: '/distribucion', icono: '⇄' },
    { label: 'Clientes', ruta: '/clientes', icono: '👥' },
    { label: 'Empleados', ruta: '/empleados', icono: '👤' },
    { label: 'Tarifas', ruta: '/tarifas', icono: '🏷' },
    { label: 'Facturas', ruta: '/facturas', icono: '🧾' }
  ];

  constructor(public authService: AuthService) {}

  cerrarSesion(): void {
    this.authService.logout();
  }
}