package com.mutualidad.afiliado.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoAfiliado {

    PENDIENTE {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.of(ACTIVO, RECHAZADO);
        }
    },
    ACTIVO {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.of(BAJA, SUSPENDIDO);
        }
    },
    SUSPENDIDO {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.of(ACTIVO, BAJA);
        }
    },
    BAJA {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.of(ACTIVO);
        }
    },
    RECHAZADO {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.noneOf(EstadoAfiliado.class);
        }
    };

    public abstract Set<EstadoAfiliado> transicionesValidas();

    public boolean puedeTransicionarA(EstadoAfiliado nuevoEstado) {
        return transicionesValidas().contains(nuevoEstado);
    }
}
