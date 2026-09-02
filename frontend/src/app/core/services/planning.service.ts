import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PlanningService {

  private apiUrl = `${environment.apiUrl}/planning`;

  constructor(private http: HttpClient) {}

  descargarPlanning(empleadoId: number, fecha: string): void {
    this.http.get(`${this.apiUrl}/empleado/${empleadoId}`, {
      params: { fecha },
      responseType: 'blob'
    }).subscribe(pdfBlob => {
      const url = window.URL.createObjectURL(pdfBlob);
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = `planning_${fecha}.pdf`;
      enlace.click();
      window.URL.revokeObjectURL(url);
    });
  }
}