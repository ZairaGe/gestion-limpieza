import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Distribucion } from './distribucion';

describe('Distribucion', () => {
  let component: Distribucion;
  let fixture: ComponentFixture<Distribucion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Distribucion],
    }).compileComponents();

    fixture = TestBed.createComponent(Distribucion);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
