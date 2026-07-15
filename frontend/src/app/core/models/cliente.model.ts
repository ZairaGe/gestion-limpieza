export type TipoCliente = 'PARTICULAR' | 'EMPRESA';

export interface Cliente {
  id?: number;
  nombre: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  tipo: TipoCliente;
}