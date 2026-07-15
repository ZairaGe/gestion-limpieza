package com.mokeal.gestion.service;

import com.mokeal.gestion.model.Tarifa;
import com.mokeal.gestion.repository.TarifaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TarifaService {

    private final TarifaRepository tarifaRepository;

    public TarifaService(TarifaRepository tarifaRepository) {
        this.tarifaRepository = tarifaRepository;
    }

    public List<Tarifa> listarTodas() {
        return tarifaRepository.findAll();
    }

    public Tarifa buscarPorId(Long id) {
        return tarifaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarifa no encontrada con id: " + id));
    }

    public Tarifa crear(Tarifa tarifa) {
        return tarifaRepository.save(tarifa);
    }

    public Tarifa actualizar(Long id, Tarifa datosNuevos) {
        Tarifa tarifa = buscarPorId(id);
        tarifa.setTipoServicio(datosNuevos.getTipoServicio());
        tarifa.setZona(datosNuevos.getZona());
        tarifa.setPrecioHora(datosNuevos.getPrecioHora());
        tarifa.setPrecioFijo(datosNuevos.getPrecioFijo());
        return tarifaRepository.save(tarifa);
    }

    public void eliminar(Long id) {
        Tarifa tarifa = buscarPorId(id);
        tarifaRepository.delete(tarifa);
    }
}