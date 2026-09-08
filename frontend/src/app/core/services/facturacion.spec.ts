import { TestBed } from '@angular/core/testing';

import { Facturacion } from './facturacion';

describe('Facturacion', () => {
  let service: Facturacion;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Facturacion);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
