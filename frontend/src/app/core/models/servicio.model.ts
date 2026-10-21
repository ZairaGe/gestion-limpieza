import { DiaSemana } from './servicio-recurrente.model';

export type EstadoServicio = 'PENDIENTE' | 'CONFIRMADO' | 'COMPLETADO' | 'CANCELADO';

export interface EmpleadoResumen {
  id: number;
  nombre: string;
  horasAsignadas: number;
}

export interface Servicio {
  tarifaNombre: string;
  id?: number;
  clienteId: number;
  clienteNombre: string;
  tarifaId: number | null;
  tarifaTipoServicio: string | null;
  tarifaZona: string | null;
  direccion: string;
  latitud?: number;
  longitud?: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  duracionHoras?: number;
  estado: EstadoServicio;
  empleados: EmpleadoResumen[];
  pagado?: boolean;
  fechaPago?: string;
  importe?: number;
  recurrente?: boolean;
  precioManual?: number | null;
  notas?: string | null;
}

export interface ServicioRequest {
  clienteId: number;
  tarifaId: number | null;
  tarifaNombre: string | null;
  direccion: string;
  fecha: string;
  horaInicio: string;
  duracionHoras: number;
  empleadoIds: number[];
  precio?: number | null;
  notas?: string | null;
  estado?: EstadoServicio;
  diasSemana?: DiaSemana[];
  repetirHasta?: string;
}