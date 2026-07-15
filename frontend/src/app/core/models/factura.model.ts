export type EstadoFactura = 'PENDIENTE' | 'PAGADA';

export interface Factura {
  id?: number;
  servicioId: number;
  numero: string;
  importe: number;
  estado: EstadoFactura;
  fechaEmision: string;
}

export interface FacturaRequest {
  servicioId: number;
  numero: string;
  importe: number;
  fechaEmision: string;
}