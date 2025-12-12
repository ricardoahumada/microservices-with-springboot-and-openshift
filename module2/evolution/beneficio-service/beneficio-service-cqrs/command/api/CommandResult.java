package com.mutualidad.beneficio.command.api;

import lombok.Value;
import java.time.Instant;

@Value
public class CommandResult {

    String id;
    boolean success;
    String message;
    Instant timestamp;

    public static CommandResult success(String id) {
        return new CommandResult(id, true, "Operacion completada exitosamente", Instant.now());
    }

    public static CommandResult success(String id, String message) {
        return new CommandResult(id, true, message, Instant.now());
    }

    public static CommandResult failure(String message) {
        return new CommandResult(null, false, message, Instant.now());
    }
}
