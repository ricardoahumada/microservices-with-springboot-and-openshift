package com.mutualidad.beneficio;

import com.mutualidad.beneficio.command.api.AsignarBeneficioCommand;
import com.mutualidad.beneficio.command.api.CommandResult;
import com.mutualidad.beneficio.command.api.TipoBeneficio;
import com.mutualidad.beneficio.command.handler.AsignarBeneficioHandler;
import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import com.mutualidad.beneficio.query.repository.BeneficioReadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BeneficioServiceCqrsApplicationTests {

    @Autowired
    private AsignarBeneficioHandler asignarHandler;

    @Autowired
    private BeneficioReadRepository readRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void asignarBeneficio_deberiaCrearBeneficioYActualizarReadModel() {
        // Given
        AsignarBeneficioCommand command = AsignarBeneficioCommand.builder()
            .afiliadoId("afiliado-001")
            .tipoBeneficio(TipoBeneficio.SALUD)
            .fechaInicio(LocalDate.now().plusDays(1))
            .monto(new BigDecimal("500.00"))
            .descripcion("Cobertura de salud completa")
            .solicitadoPor("admin")
            .build();

        // When
        CommandResult result = asignarHandler.handle(command);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getId());

        // Verificar que el read model se actualizó
        Optional<BeneficioReadModel> readModel = readRepository.findById(result.getId());
        assertTrue(readModel.isPresent());
        assertEquals("ACTIVO", readModel.get().getEstado());
        assertEquals("SALUD", readModel.get().getTipoBeneficio());
    }

    @Test
    void asignarBeneficioDuplicado_deberiaFallar() {
        // Given - primer beneficio
        AsignarBeneficioCommand command1 = AsignarBeneficioCommand.builder()
            .afiliadoId("afiliado-002")
            .tipoBeneficio(TipoBeneficio.FORMACION)
            .fechaInicio(LocalDate.now().plusDays(1))
            .monto(new BigDecimal("1000.00"))
            .solicitadoPor("admin")
            .build();

        asignarHandler.handle(command1);

        // When - segundo beneficio del mismo tipo
        AsignarBeneficioCommand command2 = AsignarBeneficioCommand.builder()
            .afiliadoId("afiliado-002")
            .tipoBeneficio(TipoBeneficio.FORMACION)
            .fechaInicio(LocalDate.now().plusDays(1))
            .monto(new BigDecimal("2000.00"))
            .solicitadoPor("admin")
            .build();

        CommandResult result = asignarHandler.handle(command2);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Ya existe"));
    }
}
