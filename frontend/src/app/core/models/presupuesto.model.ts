export interface LineaPresupuesto {
  concepto: string;
  horas: number;
  precioHora: number;
  subtotal: number;
}

export interface Presupuesto {
  id?: number;
  nombre: string | null;
  telefono?: string | null;
  correo?: string | null;
  direccion?: string | null;
  numero: string;
  lineas: LineaPresupuesto[];
  notas?: string | null;
  subtotal: number;
  descuentoPorcentaje: number;
  ivaPorcentaje: number;
  ivaImporte: number;
  total: number;
  fechaEmision: string;
}

export interface LineaPresupuestoRequest {
  concepto: string;
  horas: number;
  precioHora: number;
}

export interface PresupuestoRequest {
  nombre: string;
  telefono: string | null;
  correo: string | null;
  direccion: string | null;
  lineas: LineaPresupuestoRequest[];
  notas: string | null;
  descuentoPorcentaje: number;
  ivaPorcentaje: number;
  fechaEmision: string;
}