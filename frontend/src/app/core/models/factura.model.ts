export type EstadoFactura = 'PENDIENTE' | 'PAGADA';

export interface Factura {
  id?: number;
  clienteId: number;
  clienteNombre: string;
  numero: string;
  concepto?: string;
  subtotal?: number;
  descuentoPorcentaje?: number;
  ivaImporte?: number;
  importe: number;
  estado: EstadoFactura;
  fechaEmision: string;
  cantidadServicios: number;
}

export interface ServicioPendiente {
  id: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  direccion: string;
  importe: number;
}

export interface FacturacionPendienteResponse {
  servicios: ServicioPendiente[];
  totalImporte: number;
}

export interface GenerarFacturaRequest {
  clienteId: number;
  servicioIds: number[];
  concepto?: string;
  descuentoPorcentaje?: number;
  fechaEmision: string;
}

export interface GenerarFacturaResultado {
  mensaje: string;
  numero: string;
  importe: number;
}