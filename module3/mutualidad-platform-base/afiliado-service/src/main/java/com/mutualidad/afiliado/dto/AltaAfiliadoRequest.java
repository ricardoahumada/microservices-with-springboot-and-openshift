package com.mutualidad.afiliado.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AltaAfiliadoRequest {

    @NotBlank(message = "DNI es requerido")
    @Pattern(regexp = "\\d{8}[A-Za-z]", message = "DNI debe tener 8 digitos y una letra")
    private String dni;

    @NotBlank(message = "Nombre es requerido")
    private String nombre;

    @NotBlank(message = "Apellido es requerido")
    private String apellido;

    @Email(message = "Email debe ser valido")
    private String email;

    private String telefono;

    private LocalDate fechaNacimiento;

    @NotBlank(message = "ID de empresa es requerido")
    private String empresaId;
}
