package com.mutualidad.beneficio.infrastructure.rest;

import com.mutualidad.beneficio.query.api.BuscarBeneficiosQuery;
import com.mutualidad.beneficio.query.handler.BuscarBeneficiosHandler;
import com.mutualidad.beneficio.query.handler.ResumenBeneficiosHandler;
import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import com.mutualidad.beneficio.query.model.ResumenBeneficiosReadModel;
import com.mutualidad.beneficio.query.repository.BeneficioReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/beneficios/queries")
@RequiredArgsConstructor
public class BeneficioQueryController {

    private final BuscarBeneficiosHandler buscarHandler;
    private final ResumenBeneficiosHandler resumenHandler;
    private final BeneficioReadRepository readRepository;

    @GetMapping
    public ResponseEntity<Page<BeneficioReadModel>> buscar(
            @RequestParam String afiliadoId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        BuscarBeneficiosQuery query = BuscarBeneficiosQuery.builder()
            .afiliadoId(afiliadoId)
            .estado(estado)
            .tipo(tipo)
            .page(page)
            .size(size)
            .build();

        return ResponseEntity.ok(buscarHandler.handle(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeneficioReadModel> obtenerPorId(@PathVariable String id) {
        return readRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/resumen/{afiliadoId}")
    public ResponseEntity<ResumenBeneficiosReadModel> resumen(@PathVariable String afiliadoId) {
        return ResponseEntity.ok(resumenHandler.handle(afiliadoId));
    }
}
