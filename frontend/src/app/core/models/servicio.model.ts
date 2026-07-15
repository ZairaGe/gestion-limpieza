export type EstadoServicio = 'PENDIENTE' | 'CONFIRMADO' | 'COMPLETADO' | 'CANCELADO';

export interface Servicio {
  id?: number;
  cliente: { id: number; nombre: string };
  tarifa: { id: number; tipoServicio: string; zona: string };
  direccion: string;
  fecha: string;       // formato 'YYYY-MM-DD'
  horaInicio: string;  // formato 'HH:mm'
  horaFin: string;
  estado: EstadoServicio;
  empleados: { id: number; nombre: string }[];
}

export interface ServicioRequest {
  clienteId: number;
  tarifaId: number;
  direccion: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  empleadoIds: number[];
}