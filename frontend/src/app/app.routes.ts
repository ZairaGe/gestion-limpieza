import { Routes } from '@angular/router';
import { LoginComponent } from './features/login/login.component';
import { LayoutComponent } from './shared/components/layout/layout.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'clientes',
        loadComponent: () => import('./features/clientes/clientes.component').then(m => m.ClientesComponent)
      },
      {
        path: 'empleados',
        loadComponent: () => import('./features/empleados/empleados.component').then(m => m.EmpleadosComponent)
      },
      {
        path: 'tarifas',
        loadComponent: () => import('./features/tarifas/tarifas.component').then(m => m.TarifasComponent)
      },
      {
        path: 'servicios',
        loadComponent: () => import('./features/servicios/servicios.component').then(m => m.ServiciosComponent)
      },
      {
        path: 'facturas',
        loadComponent: () => import('./features/facturas/facturas.component').then(m => m.FacturasComponent)
      }
      
    ]
  },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];