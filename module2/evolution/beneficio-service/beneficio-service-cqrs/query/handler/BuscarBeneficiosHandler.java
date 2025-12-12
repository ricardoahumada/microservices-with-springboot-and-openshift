package com.mutualidad.beneficio.query.handler;

import com.mutualidad.beneficio.query.api.BuscarBeneficiosQuery;
import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import com.mutualidad.beneficio.query.repository.BeneficioReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BuscarBeneficiosHandler {

    private final BeneficioReadRepository repository;

    public Page<BeneficioReadModel> handle(BuscarBeneficiosQuery query) {
        log.debug("Buscando beneficios para afiliado: {}", query.getAfiliadoId());

        PageRequest pageRequest = PageRequest.of(
            query.getPage(),
            query.getSize(),
            Sort.by(Sort.Direction.DESC, "fechaInicio")
        );

        if (query.getEstado() != null) {
            return repository.findByAfiliadoIdAndEstado(query.getAfiliadoId(), query.getEstado(), pageRequest);
        }

        if (query.getTipo() != null) {
            return repository.findByAfiliadoIdAndTipoBeneficio(query.getAfiliadoId(), query.getTipo(), pageRequest);
        }

        return repository.findByAfiliadoId(query.getAfiliadoId(), pageRequest);
    }
}
