import { Component, AfterViewInit, OnChanges, Input, ElementRef, ViewChild } from '@angular/core';
import * as L from 'leaflet';
import { Servicio } from '../../../core/models/servicio.model';

// Arregla el problema típico de los iconos por defecto de Leaflet con bundlers como Angular
const iconoDefecto = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41]
});

@Component({
  selector: 'app-mapa-servicios',
  standalone: true,
  imports: [],
  templateUrl: './mapa-servicios.component.html',
  styleUrl: './mapa-servicios.component.css'
})
export class MapaServiciosComponent implements AfterViewInit, OnChanges {

  @Input() servicios: Servicio[] = [];
  @ViewChild('mapaContenedor') mapaContenedor!: ElementRef;

  private mapa: L.Map | null = null;
  private marcadores: L.Marker[] = [];

  ngAfterViewInit(): void {
    this.mapa = L.map(this.mapaContenedor.nativeElement).setView([40.4168, -3.7038], 12); // centro: Madrid

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(this.mapa);

    this.pintarMarcadores();

    setTimeout(() => {
    this.mapa?.invalidateSize();
  }, 100);
  }

  ngOnChanges(): void {
    if (this.mapa) {
      this.pintarMarcadores();
    }
  }

  private pintarMarcadores(): void {
    if (!this.mapa) return;

    this.marcadores.forEach(m => m.remove());
    this.marcadores = [];

    const conCoordenadas = this.servicios.filter(s => s.latitud != null && s.longitud != null);

    conCoordenadas.forEach(servicio => {
      const marcador = L.marker([servicio.latitud!, servicio.longitud!], { icon: iconoDefecto })
        .addTo(this.mapa!)
        .bindPopup(`
          <strong>${servicio.clienteNombre}</strong><br>
          ${servicio.horaInicio} - ${servicio.horaFin}<br>
          ${servicio.direccion}
        `);
      this.marcadores.push(marcador);
    });

    if (conCoordenadas.length > 0) {
      const grupo = L.featureGroup(this.marcadores);
      this.mapa.fitBounds(grupo.getBounds(), { padding: [30, 30] });
    }
  }
}