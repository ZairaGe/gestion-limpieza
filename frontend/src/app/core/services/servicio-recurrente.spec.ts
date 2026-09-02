import { TestBed } from '@angular/core/testing';

import { ServicioRecurrente } from './servicio-recurrente';

describe('ServicioRecurrente', () => {
  let service: ServicioRecurrente;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ServicioRecurrente);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
