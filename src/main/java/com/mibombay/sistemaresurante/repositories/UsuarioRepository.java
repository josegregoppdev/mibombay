package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameAndEmpresaId(String username, Long empresaId);

    Optional<Usuario> findByUsernameAndEsSuperadminTrue(String username);

    boolean existsByUsernameAndEmpresaId(String username, Long empresaId);

    List<Usuario> findAllByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<Usuario> findByIdAndActivoTrue(Long id);

    long countByEmpresaId(Long empresaId);

    long count();
}
