import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ServicioResumen {
  id: number;
  clienteNombre: string;
  horaInicio: string;
  horaFin: string;
  direccion: string;
  estado: string;
}

export interface EmpleadoResumen {
  id: number;
  nombre: string;
}

export interface TarifaResumen {
  tipoServicio: string;
  zona: string;
  precioHora: number | null;
  precioFijo: number | null;
}

export interface ResumenSemanal {
  totalServicios: number;
  pendientes: number;
}

export interface DashboardResumen {
  serviciosHoy: number;
  ingresosSemana: number;
  trabajadoresActivos: number;
  pendientesAsignar: number;
  agendaHoy: ServicioResumen[];
  trabajadores: EmpleadoResumen[];
  tarifasActivas: TarifaResumen[];
  resumenSemanal: ResumenSemanal;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {

  private apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  obtenerResumen(): Observable<DashboardResumen> {
    return this.http.get<DashboardResumen>(`${this.apiUrl}/resumen`);
  }
}