import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MapaServicios } from './mapa-servicios';

describe('MapaServicios', () => {
  let component: MapaServicios;
  let fixture: ComponentFixture<MapaServicios>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MapaServicios],
    }).compileComponents();

    fixture = TestBed.createComponent(MapaServicios);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
