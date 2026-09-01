package com.mokeal.gestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern SUFIJOS_PISO = Pattern.compile(
        "(?i)\\s*,?\\s*\\d+[ºª°]?\\s*[A-Za-z]?\\s*$|(?i)\\s*,?\\s*(piso|puerta|planta|izq|izquierda|dcha|derecha|bajo|ático|atico)\\s*\\S*\\s*$"
    );

    public double[] geocodificar(String direccion) {
        double[] resultado = intentarGeocodificar(direccion);
        if (resultado != null) {
            return resultado;
        }

        String direccionLimpia = limpiarDireccion(direccion);
        if (!direccionLimpia.equals(direccion)) {
            return intentarGeocodificar(direccionLimpia);
        }

        return null;
    }

    private String limpiarDireccion(String direccion) {
        return SUFIJOS_PISO.matcher(direccion).replaceAll("").trim();
    }

    private double[] intentarGeocodificar(String direccion) {
        try {
            String direccionCodificada = URLEncoder.encode(direccion + ", Madrid, España", StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + direccionCodificada + "&format=json&limit=1";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "LimpiaGest/1.0 (app de gestion Mokeal)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> respuesta = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, String.class);
            JsonNode nodo = objectMapper.readTree(respuesta.getBody());

            if (nodo.isArray() && nodo.size() > 0) {
                double lat = nodo.get(0).get("lat").asDouble();
                double lon = nodo.get(0).get("lon").asDouble();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            System.err.println("No se pudo geocodificar la dirección: " + direccion + " — " + e.getMessage());
        }
        return null;
    }
}