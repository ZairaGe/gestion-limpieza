import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PlanningService {

  private apiUrl = `${environment.apiUrl}/planning`;

  constructor(private http: HttpClient) { }

  descargarPlanning(empleadoId: number, fecha: string, alError?: (mensaje: string) => void): void {
    this.descargar(`empleado/${empleadoId}`, { fecha }, `planning_${fecha}.pdf`, alError);
  }

  descargarPlanningGeneral(desde: string, hasta: string, alError?: (mensaje: string) => void): void {
    this.descargar('general', { desde, hasta }, `horario_${desde}_${hasta}.pdf`, alError);
  }

  descargarPorTrabajador(desde: string, hasta: string, alError?: (mensaje: string) => void): void {
    this.descargar('por-trabajador', { desde, hasta }, `planning_trabajadores_${desde}_${hasta}.pdf`, alError);
  }

  private descargar(ruta: string, params: Record<string, string>, nombre: string,
    alError?: (mensaje: string) => void): void {
    this.http.get(`${this.apiUrl}/${ruta}`, { params, responseType: 'blob' }).subscribe({
      next: (pdf) => {
        const url = window.URL.createObjectURL(pdf);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = nombre;
        document.body.appendChild(enlace);
        enlace.click();
        document.body.removeChild(enlace);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        let motivo: string;
        if (err.status === 0) {
          motivo = 'no hay conexión con el servidor';
        } else if (err.status === 401 || err.status === 403) {
          motivo = 'error 403 (sesión caducada o fallo interno del servidor)';
        } else {
          motivo = `error ${err.status} del servidor`;
        }
        alError?.(`No se pudo descargar el PDF: ${motivo}.`);
      }
    });
  }
}