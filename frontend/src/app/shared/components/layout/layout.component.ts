import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

import {
  LucideDynamicIcon,
  LucideSparkles,
  LucideLayoutGrid,
  LucideCalendarCheck,
  LucideMap,
  LucideUsers,
  LucideConciergeBell,
  LucideTag,
  LucideWallet,
  LucideFileText,
  LucideCalculator,
  LucideLogOut
} from '@lucide/angular';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
  RouterOutlet,
  RouterLink,
  RouterLinkActive,
  LucideDynamicIcon
],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent {
  readonly IconLogo = LucideSparkles;
  readonly IconSalir = LucideLogOut;

  readonly nombreApp = 'Mokeal';

  menuItems = [
    { label: 'Inicio', ruta: '/dashboard', icono: LucideLayoutGrid },
    { label: 'Servicios', ruta: '/servicios', icono: LucideCalendarCheck },
    { label: 'Distribución', ruta: '/distribucion', icono: LucideMap },
    { label: 'Clientes', ruta: '/clientes', icono: LucideUsers },
    { label: 'Empleados', ruta: '/empleados', icono: LucideConciergeBell },
    { label: 'Tarifas', ruta: '/tarifas', icono: LucideTag },
    { label: 'Facturación', ruta: '/facturacion', icono: LucideWallet },
    { label: 'Facturas', ruta: '/facturas', icono: LucideFileText },
    { label: 'Presupuestos', ruta: '/presupuestos', icono: LucideCalculator }
  ];

  constructor(public authService: AuthService) {}

  cerrarSesion(): void {
    this.authService.logout();
  }
}

