import { TestBed } from '@angular/core/testing';

import { Tarifa } from './tarifa';

describe('Tarifa', () => {
  let service: Tarifa;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Tarifa);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
