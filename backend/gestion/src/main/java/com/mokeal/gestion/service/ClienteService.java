package com.mokeal.gestion.service;

import com.mokeal.gestion.model.Cliente;
import com.mokeal.gestion.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + id));
    }

    public Cliente crear(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public Cliente actualizar(Long id, Cliente datosNuevos) {
        Cliente cliente = buscarPorId(id);
        cliente.setNombre(datosNuevos.getNombre());
        cliente.setTelefono(datosNuevos.getTelefono());
        cliente.setEmail(datosNuevos.getEmail());
        cliente.setDireccion(datosNuevos.getDireccion());
        cliente.setTipo(datosNuevos.getTipo());
        return clienteRepository.save(cliente);
    }

    public void eliminar(Long id) {
        Cliente cliente = buscarPorId(id);
        clienteRepository.delete(cliente);
    }
}