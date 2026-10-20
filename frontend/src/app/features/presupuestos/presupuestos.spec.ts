import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Presupuestos } from './presupuestos';

describe('Presupuestos', () => {
  let component: Presupuestos;
  let fixture: ComponentFixture<Presupuestos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Presupuestos],
    }).compileComponents();

    fixture = TestBed.createComponent(Presupuestos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
