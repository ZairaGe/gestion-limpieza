export type DiaSemana = 'LUNES' | 'MARTES' | 'MIERCOLES' | 'JUEVES' | 'VIERNES' | 'SABADO' | 'DOMINGO';

export interface ServicioRecurrenteRequest {
  clienteId: number;
  tarifaId: number;
  direccion: string;
  horaInicio: string;
  duracionHoras: number;
  diasSemana: DiaSemana[];
  empleadoIds: number[];
  fechaInicio: string;
  fechaFin: string;
}

export interface ServicioRecurrenteResultado {
  mensaje: string;
  serviciosGenerados: number;
}