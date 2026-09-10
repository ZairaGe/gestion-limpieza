export type EstadoServicio = 'PENDIENTE' | 'CONFIRMADO' | 'COMPLETADO' | 'CANCELADO';

export interface EmpleadoResumen {
  id: number;
  nombre: string;
  horasAsignadas?: number;
}

export interface Servicio {
  id?: number;
  clienteId: number;
  clienteNombre: string;
  tarifaId: number;
  tarifaTipoServicio: string;
  tarifaZona: string;
  direccion: string;
  latitud?: number;
  longitud?: number;
  fecha: string;
  horaInicio: string;
  duracionHoras: number;
  horaFin: string;
  estado: EstadoServicio;
  empleados: EmpleadoResumen[];
  pagado?: boolean;
  fechaPago?: string;
  importe?: number;
}

export interface ServicioRequest {
  clienteId: number;
  tarifaId: number;
  direccion: string;
  fecha: string;
  horaInicio: string;
  duracionHoras: number;
  horaFin?: string;
  empleadoIds: number[];
}