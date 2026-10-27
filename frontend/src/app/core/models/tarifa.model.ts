export type TipoServicio = 'CASA' | 'OFICINA' | 'EVENTO';
export type Zona = 'MADRID' | 'FUERA_MADRID';

export interface Tarifa {
  id?: number;
  nombre: string;
  descripcion?: string | null;
  tipoServicio?: TipoServicio | null;
  zona?: Zona | null;
  precioHora?: number | null;
  precioFijo?: number | null;
}