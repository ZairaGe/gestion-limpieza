import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DiaIngreso {
  fecha: string;
  hoy: boolean;
  previsto: number;
  cobrado: number;
}

export interface ServicioHoy {
  id: number;
  clienteNombre: string;
  tipoServicio: string;
  horaInicio: string;
  horaFin: string;
  estado: string;
  empleados: string[];
}


export interface DashboardResumen {
  ingresosSemana: number;
  previstoSemana: number;
  serviciosHoy: number;
  trabajadoresActivos: number;
  sinServicioHoy: number;
  ingresosPorDia: DiaIngreso[];
  agendaHoy: ServicioHoy[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {

  private apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  obtenerResumen(): Observable<DashboardResumen> {
    return this.http.get<DashboardResumen>(`${this.apiUrl}/resumen`);
  }
}