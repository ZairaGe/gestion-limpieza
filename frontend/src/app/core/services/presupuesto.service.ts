import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Presupuesto, PresupuestoRequest } from '../models/presupuesto.model';

@Injectable({ providedIn: 'root' })
export class PresupuestoService {

  private apiUrl = `${environment.apiUrl}/presupuestos`;

  constructor(private http: HttpClient) {}

  listar(clienteId?: number): Observable<Presupuesto[]> {
    const params: Record<string, string | number> = clienteId ? { clienteId } : {};
    return this.http.get<Presupuesto[]>(this.apiUrl, { params });
  }

  crear(request: PresupuestoRequest): Observable<Presupuesto> {
    return this.http.post<Presupuesto>(this.apiUrl, request);
  }

  cambiarEstado(id: number, estado: string): Observable<Presupuesto> {
    return this.http.patch<Presupuesto>(`${this.apiUrl}/${id}/estado`, { estado });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  descargarPdf(id: number, numero: string): void {
    this.http.get(`${this.apiUrl}/${id}/pdf`, { responseType: 'blob' }).subscribe(pdfBlob => {
      const url = window.URL.createObjectURL(pdfBlob);
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = `${numero}.pdf`;
      enlace.click();
      window.URL.revokeObjectURL(url);
    });
  }
}