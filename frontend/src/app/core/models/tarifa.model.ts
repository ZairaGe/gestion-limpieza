export type TipoServicio = 'CASA' | 'OFICINA' | 'EVENTO';
export type Zona = 'MADRID' | 'FUERA_MADRID';

export interface Tarifa {
  id?: number;
  tipoServicio: TipoServicio;
  zona: Zona;
  precioHora?: number;
  precioFijo?: number;
}