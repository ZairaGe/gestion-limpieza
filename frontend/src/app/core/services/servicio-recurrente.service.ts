import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ServicioRecurrenteRequest, ServicioRecurrenteResultado } from '../models/servicio-recurrente.model';

@Injectable({ providedIn: 'root' })
export class ServicioRecurrenteService {

  private apiUrl = `${environment.apiUrl}/servicios-recurrentes`;

  constructor(private http: HttpClient) {}

  crear(request: ServicioRecurrenteRequest): Observable<ServicioRecurrenteResultado> {
    return this.http.post<ServicioRecurrenteResultado>(this.apiUrl, request);
  }
}