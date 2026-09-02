import { TestBed } from '@angular/core/testing';

import { Planning } from './planning';

describe('Planning', () => {
  let service: Planning;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Planning);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
