import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Factura, FacturaRequest } from '../models/factura.model';

@Injectable({ providedIn: 'root' })
export class FacturaService {

  private apiUrl = `${environment.apiUrl}/facturas`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Factura[]> {
    return this.http.get<Factura[]>(this.apiUrl);
  }

  crear(factura: FacturaRequest): Observable<Factura> {
    return this.http.post<Factura>(this.apiUrl, factura);
  }

  marcarComoPagada(id: number): Observable<Factura> {
    return this.http.patch<Factura>(`${this.apiUrl}/${id}/pagar`, {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}