import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Factura } from '../models/factura.model';

@Injectable({ providedIn: 'root' })
export class FacturaService {

  private apiUrl = `${environment.apiUrl}/facturas`;

  constructor(private http: HttpClient) { }

  listar(clienteId?: number): Observable<Factura[]> {
    let params: Record<string, string | number> = {};
    if (clienteId) {
      params = { clienteId };
    }
    return this.http.get<Factura[]>(this.apiUrl, { params });
  }

  marcarComoPagada(id: number): Observable<Factura> {
    return this.http.patch<Factura>(`${this.apiUrl}/${id}/pagar`, {});
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