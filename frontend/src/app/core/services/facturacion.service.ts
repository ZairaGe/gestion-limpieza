import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FacturacionPendienteResponse, GenerarFacturaRequest, GenerarFacturaResultado } from '../models/factura.model';

@Injectable({ providedIn: 'root' })
export class FacturacionService {

  private apiUrl = `${environment.apiUrl}/facturacion`;

  constructor(private http: HttpClient) {}

  obtenerPendientes(clienteId: number, desde: string, hasta: string): Observable<FacturacionPendienteResponse> {
    return this.http.get<FacturacionPendienteResponse>(`${this.apiUrl}/pendientes`, {
      params: { clienteId, desde, hasta }
    });
  }

  generar(request: GenerarFacturaRequest): Observable<GenerarFacturaResultado> {
    return this.http.post<GenerarFacturaResultado>(`${this.apiUrl}/generar`, request);
  }
}