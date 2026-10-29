export interface Empleado {
  id?: number;
  nombre: string;
  apellido?: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  datosEmergencia?: string;
  activo: boolean;
}