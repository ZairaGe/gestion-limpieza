export type EstadoPresupuesto = 'PENDIENTE' | 'ACEPTADO' | 'RECHAZADO';

export interface LineaPresupuesto {
  concepto: string;
  horas: number;
  precioHora: number;
  subtotal: number;
}

export interface Presupuesto {
  id?: number;
  clienteId: number;
  clienteNombre: string;
  numero: string;
  lineas: LineaPresupuesto[];
  subtotal: number;
  descuentoPorcentaje: number;
  total: number;
  estado: EstadoPresupuesto;
  fechaEmision: string;
}

export interface LineaPresupuestoRequest {
  concepto: string;
  horas: number;
  precioHora: number;
}

export interface PresupuestoRequest {
  clienteId: number;
  lineas: LineaPresupuestoRequest[];
  descuentoPorcentaje: number;
  fechaEmision: string;
}