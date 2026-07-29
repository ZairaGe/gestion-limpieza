import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Tarifa } from '../models/tarifa.model';

@Injectable({ providedIn: 'root' })
export class TarifaService {

  private apiUrl = `${environment.apiUrl}/tarifas`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Tarifa[]> {
    return this.http.get<Tarifa[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<Tarifa> {
    return this.http.get<Tarifa>(`${this.apiUrl}/${id}`);
  }

  crear(tarifa: Tarifa): Observable<Tarifa> {
    return this.http.post<Tarifa>(this.apiUrl, tarifa);
  }

  actualizar(id: number, tarifa: Tarifa): Observable<Tarifa> {
    return this.http.put<Tarifa>(`${this.apiUrl}/${id}`, tarifa);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}