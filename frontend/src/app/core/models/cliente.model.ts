export type TipoCliente = 'PARTICULAR' | 'EMPRESA';

export interface Cliente {
  id?: number;
  nombre: string;
  apellido?: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  codigoPostal?: string;
  tipo: TipoCliente;
  nif?: string;
}