# DDD en Monolito Bien Diseñado - Mutualidad Platform

> **Implementación que muestra DDD correctamente aplicado en arquitectura monolítica**
> 
> Los **Bounded Contexts están separados lógicamente** pero se ejecutan en el **mismo deployment**

## Principios DDD Aplicados

- ✅ **Bounded Contexts separados** por dominio de negocio
- ✅ **Aggregate Roots** con responsabilidades claras
- ✅ **Value Objects** inmutables y validados
- ✅ **Domain Events** para comunicación entre contextos
- ✅ **Ubiquitous Language** consistente en cada contexto

---

# 🏗️ BOUNDED CONTEXT: GESTIÓN DE AFILIADOS

> **Responsabilidad**: Gestionar el ciclo de vida de afiliados y su información personal

## Value Objects del Contexto

```typescript
/**
 * DNI - Value Object específico para el contexto de Afiliados
 * Encapsula las reglas de validación de identidad
 */
class DNIAfiliado {
    private readonly valor: string;
    private static readonly PATRON = /^[0-9]{8}[A-Z]$/;

    constructor(valor: string) {
        if (!valor?.trim()) {
            throw new Error('DNI es requerido');
        }
        
        const dniLimpio = valor.trim().toUpperCase();
        if (!DNIAfiliado.PATRON.test(dniLimpio)) {
            throw new Error(`Formato DNI inválido: ${valor}`);
        }
        
        // Validar letra de control
        if (!this.validarLetraControl(dniLimpio)) {
            throw new Error(`Letra de control DNI incorrecta: ${valor}`);
        }
        
        this.valor = dniLimpio;
    }

    private validarLetraControl(dni: string): boolean {
        const letras = 'TRWAGMYFPDXBNJZSQVHLCKE';
        const numero = parseInt(dni.substring(0, 8), 10);
        const letraCalculada = letras[numero % 23];
        return dni.charAt(8) === letraCalculada;
    }

    getValue(): string { return this.valor; }
    equals(other: DNIAfiliado): boolean { 
        return this.valor === other.valor; 
    }
}

/**
 * Dirección - Value Object para ubicación de afiliados
 */
class DireccionAfiliado {
    constructor(
        private readonly via: string,
        private readonly numero: string,
        private readonly ciudad: string,
        private readonly provincia: string,
        private readonly codigoPostal: string
    ) {
        if (!via?.trim() || !ciudad?.trim() || !provincia?.trim()) {
            throw new Error('Via, ciudad y provincia son obligatorios');
        }
        
        if (!/^[0-9]{5}$/.test(codigoPostal)) {
            throw new Error('Código postal debe ser 5 dígitos');
        }
    }

    toString(): string {
        return `${this.via} ${this.numero}, ${this.ciudad}, ${this.provincia} - ${this.codigoPostal}`;
    }

    getCiudad(): string { return this.ciudad; }
    getProvincia(): string { return this.provincia; }
}

/**
 * Estado de Afiliación - Value Object que encapsula reglas de estado
 */
class EstadoAfiliacion {
    private static readonly ESTADOS_VALIDOS = ['ACTIVO', 'INACTIVO', 'SUSPENDIDO'] as const;
    private readonly estado: typeof EstadoAfiliacion.ESTADOS_VALIDOS[number];

    constructor(estado: string) {
        if (!EstadoAfiliacion.ESTADOS_VALIDOS.includes(estado as any)) {
            throw new Error(`Estado inválido: ${estado}`);
        }
        this.estado = estado as typeof EstadoAfiliacion.ESTADOS_VALIDOS[number];
    }

    isActivo(): boolean { return this.estado === 'ACTIVO'; }
    isSuspendido(): boolean { return this.estado === 'SUSPENDIDO'; }
    getValue(): string { return this.estado; }
    
    puedeTransicionarA(nuevoEstado: EstadoAfiliacion): boolean {
        // Reglas de negocio para transiciones de estado
        const transicionesPermitidas = {
            'ACTIVO': ['INACTIVO', 'SUSPENDIDO'],
            'SUSPENDIDO': ['ACTIVO', 'INACTIVO'],
            'INACTIVO': ['ACTIVO']
        };
        
        return transicionesPermitidas[this.estado]?.includes(nuevoEstado.estado) || false;
    }
}
```

## Aggregate Root: Afiliado

```typescript
/**
 * Afiliado - Aggregate Root del contexto de Gestión de Afiliados
 * 
 * Responsabilidades:
 * - Mantener coherencia de datos del afiliado
 * - Aplicar reglas de negocio de afiliación
 * - Generar eventos de dominio relevantes
 */
class Afiliado {
    private readonly id: string;
    private readonly dni: DNIAfiliado;
    private nombre: string;
    private apellidos: string;
    private direccion: DireccionAfiliado;
    private estado: EstadoAfiliacion;
    private readonly fechaAlta: Date;
    private fechaUltimaModificacion: Date;
    
    // Events generados por este agregado
    private eventos: DomainEvent[] = [];

    constructor(
        dni: DNIAfiliado,
        nombre: string,
        apellidos: string,
        direccion: DireccionAfiliado
    ) {
        // Validaciones de dominio
        if (!nombre?.trim() || !apellidos?.trim()) {
            throw new Error('Nombre y apellidos son obligatorios');
        }

        this.id = crypto.randomUUID();
        this.dni = dni;
        this.nombre = nombre.trim();
        this.apellidos = apellidos.trim();
        this.direccion = direccion;
        this.estado = new EstadoAfiliacion('ACTIVO');
        this.fechaAlta = new Date();
        this.fechaUltimaModificacion = new Date();

        // Evento de dominio: nuevo afiliado registrado
        this.eventos.push(new AfiliadoRegistradoEvent(
            this.id,
            this.dni.getValue(),
            this.getFullName(),
            this.fechaAlta
        ));
    }

    /**
     * Cambiar dirección del afiliado
     * Aplica reglas de negocio y genera eventos
     */
    cambiarDireccion(nuevaDireccion: DireccionAfiliado): void {
        if (!this.estado.isActivo()) {
            throw new Error('Solo afiliados activos pueden cambiar dirección');
        }

        const direccionAnterior = this.direccion;
        this.direccion = nuevaDireccion;
        this.fechaUltimaModificacion = new Date();

        // Evento de dominio: dirección cambiada
        this.eventos.push(new DireccionCambiadaEvent(
            this.id,
            this.dni.getValue(),
            direccionAnterior.toString(),
            nuevaDireccion.toString()
        ));
    }

    /**
     * Suspender afiliado
     * Aplica reglas de transición de estado
     */
    suspender(motivo: string): void {
        const nuevoEstado = new EstadoAfiliacion('SUSPENDIDO');
        
        if (!this.estado.puedeTransicionarA(nuevoEstado)) {
            throw new Error(`No se puede suspender desde estado ${this.estado.getValue()}`);
        }

        this.estado = nuevoEstado;
        this.fechaUltimaModificacion = new Date();

        // Evento de dominio: afiliado suspendido
        this.eventos.push(new AfiliadoSuspendidoEvent(
            this.id,
            this.dni.getValue(),
            motivo,
            new Date()
        ));
    }

    /**
     * Reactivar afiliado
     */
    reactivar(): void {
        const nuevoEstado = new EstadoAfiliacion('ACTIVO');
        
        if (!this.estado.puedeTransicionarA(nuevoEstado)) {
            throw new Error(`No se puede reactivar desde estado ${this.estado.getValue()}`);
        }

        this.estado = nuevoEstado;
        this.fechaUltimaModificacion = new Date();

        // Evento de dominio: afiliado reactivado
        this.eventos.push(new AfiliadoReactivadoEvent(
            this.id,
            this.dni.getValue(),
            new Date()
        ));
    }

    // Getters
    getId(): string { return this.id; }
    getDNI(): DNIAfiliado { return this.dni; }
    getFullName(): string { return `${this.nombre} ${this.apellidos}`; }
    getEstado(): EstadoAfiliacion { return this.estado; }
    getDireccion(): DireccionAfiliado { return this.direccion; }
    getFechaAlta(): Date { return this.fechaAlta; }

    // Gestión de eventos de dominio
    getEventosNoPublicados(): DomainEvent[] { return [...this.eventos]; }
    marcarEventosComoPublicados(): void { this.eventos = []; }
}
```

## Repository del Contexto

```typescript
/**
 * Repository para Afiliados
 * Define el contrato de persistencia del agregado
 */
interface AfiliadoRepository {
    save(afiliado: Afiliado): Promise<void>;
    findByDNI(dni: DNIAfiliado): Promise<Afiliado | null>;
    findById(id: string): Promise<Afiliado | null>;
    findByEstado(estado: EstadoAfiliacion): Promise<Afiliado[]>;
}

/**
 * Implementación in-memory para el ejemplo
 */
class InMemoryAfiliadoRepository implements AfiliadoRepository {
    private afiliados: Map<string, Afiliado> = new Map();

    async save(afiliado: Afiliado): Promise<void> {
        this.afiliados.set(afiliado.getId(), afiliado);
    }

    async findByDNI(dni: DNIAfiliado): Promise<Afiliado | null> {
        for (const afiliado of this.afiliados.values()) {
            if (afiliado.getDNI().equals(dni)) {
                return afiliado;
            }
        }
        return null;
    }

    async findById(id: string): Promise<Afiliado | null> {
        return this.afiliados.get(id) || null;
    }

    async findByEstado(estado: EstadoAfiliacion): Promise<Afiliado[]> {
        return Array.from(this.afiliados.values())
            .filter(afiliado => afiliado.getEstado().getValue() === estado.getValue());
    }
}
```

---

# 💰 BOUNDED CONTEXT: GESTIÓN DE BENEFICIOS

> **Responsabilidad**: Gestionar beneficios, prestaciones y cálculos financieros

## Value Objects del Contexto

```typescript
/**
 * Tipo de Beneficio - Value Object específico para beneficios
 */
class TipoBeneficio {
    private static readonly TIPOS_VALIDOS = [
        'CONSULTA_MEDICA',
        'HOSPITALIZACION',
        'FARMACIA', 
        'DENTAL',
        'OPTICA',
        'FISIOTERAPIA'
    ] as const;

    private readonly tipo: typeof TipoBeneficio.TIPOS_VALIDOS[number];

    constructor(tipo: string) {
        if (!TipoBeneficio.TIPOS_VALIDOS.includes(tipo as any)) {
            throw new Error(`Tipo de beneficio inválido: ${tipo}`);
        }
        this.tipo = tipo as typeof TipoBeneficio.TIPOS_VALIDOS[number];
    }

    getValue(): string { return this.tipo; }
    equals(other: TipoBeneficio): boolean { return this.tipo === other.tipo; }
    
    getCobertura(): number {
        // Reglas de negocio para cobertura por tipo
        const coberturas = {
            'CONSULTA_MEDICA': 80,
            'HOSPITALIZACION': 100,
            'FARMACIA': 50,
            'DENTAL': 70,
            'OPTICA': 60,
            'FISIOTERAPIA': 75
        };
        return coberturas[this.tipo];
    }
}

/**
 * Importe Monetario - Value Object para cálculos financieros
 */
class ImporteMonetario {
    private readonly valor: number;
    private readonly moneda: string;

    constructor(valor: number, moneda: string = 'EUR') {
        if (valor < 0) {
            throw new Error('El importe no puede ser negativo');
        }
        if (!moneda?.trim()) {
            throw new Error('La moneda es obligatoria');
        }
        
        this.valor = Math.round(valor * 100) / 100; // Redondear a 2 decimales
        this.moneda = moneda.toUpperCase();
    }

    getValor(): number { return this.valor; }
    getMoneda(): string { return this.moneda; }
    
    sumar(otro: ImporteMonetario): ImporteMonetario {
        if (this.moneda !== otro.moneda) {
            throw new Error('No se pueden sumar importes de diferentes monedas');
        }
        return new ImporteMonetario(this.valor + otro.valor, this.moneda);
    }

    aplicarPorcentaje(porcentaje: number): ImporteMonetario {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new Error('Porcentaje debe estar entre 0 y 100');
        }
        return new ImporteMonetario(this.valor * (porcentaje / 100), this.moneda);
    }

    toString(): string { 
        return `${this.valor.toFixed(2)} ${this.moneda}`;
    }
}

/**
 * Período de Vigencia - Value Object para rangos temporales
 */
class PeriodoVigencia {
    private readonly fechaInicio: Date;
    private readonly fechaFin: Date | null;

    constructor(fechaInicio: Date, fechaFin?: Date) {
        if (!fechaInicio) {
            throw new Error('Fecha de inicio es obligatoria');
        }
        
        if (fechaFin && fechaFin <= fechaInicio) {
            throw new Error('Fecha fin debe ser posterior a fecha inicio');
        }

        this.fechaInicio = new Date(fechaInicio);
        this.fechaFin = fechaFin ? new Date(fechaFin) : null;
    }

    getFechaInicio(): Date { return new Date(this.fechaInicio); }
    getFechaFin(): Date | null { 
        return this.fechaFin ? new Date(this.fechaFin) : null; 
    }

    estaVigente(fecha: Date = new Date()): boolean {
        const esPosterioriAlInicio = fecha >= this.fechaInicio;
        const esAnteriorAlFin = !this.fechaFin || fecha <= this.fechaFin;
        return esPosterioriAlInicio && esAnteriorAlFin;
    }

    getDuraccionDias(): number | null {
        if (!this.fechaFin) return null;
        
        const diffTime = this.fechaFin.getTime() - this.fechaInicio.getTime();
        return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    }
}
```

## Aggregate Root: Beneficio

```typescript
/**
 * Beneficio - Aggregate Root del contexto de Beneficios
 * 
 * Responsabilidades:
 * - Gestionar asignación y uso de beneficios
 * - Calcular importes y coberturas
 * - Aplicar reglas de carencia y vigencia
 */
class Beneficio {
    private readonly id: string;
    private readonly afiliadoId: string;
    private readonly tipo: TipoBeneficio;
    private readonly importeMaximo: ImporteMonetario;
    private importeUtilizado: ImporteMonetario;
    private readonly periodo: PeriodoVigencia;
    private readonly fechaAsignacion: Date;
    private estado: 'ACTIVO' | 'UTILIZADO' | 'VENCIDO' | 'CANCELADO';
    
    // Eventos del agregado
    private eventos: DomainEvent[] = [];

    constructor(
        afiliadoId: string,
        tipo: TipoBeneficio,
        importeMaximo: ImporteMonetario,
        periodo: PeriodoVigencia
    ) {
        if (!afiliadoId?.trim()) {
            throw new Error('ID de afiliado es obligatorio');
        }

        this.id = crypto.randomUUID();
        this.afiliadoId = afiliadoId;
        this.tipo = tipo;
        this.importeMaximo = importeMaximo;
        this.importeUtilizado = new ImporteMonetario(0, importeMaximo.getMoneda());
        this.periodo = periodo;
        this.fechaAsignacion = new Date();
        this.estado = 'ACTIVO';

        // Evento de dominio: beneficio asignado
        this.eventos.push(new BeneficioAsignado(
            this.id,
            this.afiliadoId,
            this.tipo.getValue(),
            this.importeMaximo,
            this.fechaAsignacion
        ));
    }

    /**
     * Utilizar beneficio para una prestación
     */
    utilizar(importe: ImporteMonetario): ImporteMonetario {
        // Verificar estado
        if (this.estado !== 'ACTIVO') {
            throw new Error(`Beneficio no está activo: ${this.estado}`);
        }

        // Verificar vigencia
        if (!this.periodo.estaVigente()) {
            this.estado = 'VENCIDO';
            throw new Error('Beneficio ha vencido');
        }

        // Verificar si queda saldo
        const importeDisponible = this.getImporteDisponible();
        if (importeDisponible.getValor() <= 0) {
            this.estado = 'UTILIZADO';
            throw new Error('Beneficio completamente utilizado');
        }

        // Calcular importe a cubrir (no puede exceder el disponible)
        const porcentajeCobertura = this.tipo.getCobertura();
        let importeCubierto = importe.aplicarPorcentaje(porcentajeCobertura);
        
        if (importeCubierto.getValor() > importeDisponible.getValor()) {
            importeCubierto = importeDisponible;
        }

        // Actualizar importe utilizado
        this.importeUtilizado = this.importeUtilizado.sumar(importeCubierto);
        
        // Verificar si se completó
        if (this.getImporteDisponible().getValor() <= 0) {
            this.estado = 'UTILIZADO';
        }

        // Evento de dominio: beneficio utilizado
        this.eventos.push(new BeneficioUtilizado(
            this.id,
            this.afiliadoId,
            importeCubierto,
            this.importeUtilizado,
            new Date()
        ));

        return importeCubierto;
    }

    /**
     * Cancelar beneficio
     */
    cancelar(motivo: string): void {
        if (this.estado === 'CANCELADO') {
            throw new Error('Beneficio ya está cancelado');
        }

        this.estado = 'CANCELADO';

        // Evento de dominio: beneficio cancelado
        this.eventos.push(new BeneficioCancelado(
            this.id,
            this.afiliadoId,
            motivo,
            new Date()
        ));
    }

    // Consultas
    getImporteDisponible(): ImporteMonetario {
        const disponible = this.importeMaximo.getValor() - this.importeUtilizado.getValor();
        return new ImporteMonetario(Math.max(0, disponible), this.importeMaximo.getMoneda());
    }

    puedeUtilizar(): boolean {
        return this.estado === 'ACTIVO' && 
               this.periodo.estaVigente() && 
               this.getImporteDisponible().getValor() > 0;
    }

    // Getters
    getId(): string { return this.id; }
    getAfiliadoId(): string { return this.afiliadoId; }
    getTipo(): TipoBeneficio { return this.tipo; }
    getEstado(): string { return this.estado; }
    getPeriodo(): PeriodoVigencia { return this.periodo; }
    
    // Gestión de eventos
    getEventosNoPublicados(): DomainEvent[] { return [...this.eventos]; }
    marcarEventosComoPublicados(): void { this.eventos = []; }
}
```

---

# 📧 BOUNDED CONTEXT: NOTIFICACIONES

> **Responsabilidad**: Gestionar comunicaciones con afiliados

```typescript
/**
 * Canal de Notificación - Value Object
 */
class CanalNotificacion {
    private readonly tipo: 'EMAIL' | 'SMS' | 'PUSH' | 'POSTAL';
    private readonly direccion: string;

    constructor(tipo: string, direccion: string) {
        if (!['EMAIL', 'SMS', 'PUSH', 'POSTAL'].includes(tipo)) {
            throw new Error(`Tipo de canal inválido: ${tipo}`);
        }
        
        if (!direccion?.trim()) {
            throw new Error('Dirección del canal es obligatoria');
        }

        this.tipo = tipo as 'EMAIL' | 'SMS' | 'PUSH' | 'POSTAL';
        this.direccion = direccion.trim();

        // Validaciones específicas por tipo
        this.validarDireccion();
    }

    private validarDireccion(): void {
        switch (this.tipo) {
            case 'EMAIL':
                const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailRegex.test(this.direccion)) {
                    throw new Error('Email inválido');
                }
                break;
            case 'SMS':
                const phoneRegex = /^[+]?[0-9\s-()]{9,15}$/;
                if (!phoneRegex.test(this.direccion)) {
                    throw new Error('Teléfono inválido');
                }
                break;
        }
    }

    getTipo(): string { return this.tipo; }
    getDireccion(): string { return this.direccion; }
}

/**
 * Notificación - Aggregate Root
 */
class Notificacion {
    private readonly id: string;
    private readonly destinatarioId: string;
    private readonly asunto: string;
    private readonly mensaje: string;
    private readonly canal: CanalNotificacion;
    private readonly fechaCreacion: Date;
    private fechaEnvio: Date | null = null;
    private estado: 'PENDIENTE' | 'ENVIADA' | 'FALLIDA' | 'CANCELADA';
    private intentosEnvio: number = 0;
    
    private eventos: DomainEvent[] = [];

    constructor(
        destinatarioId: string,
        asunto: string,
        mensaje: string,
        canal: CanalNotificacion
    ) {
        if (!destinatarioId?.trim() || !asunto?.trim() || !mensaje?.trim()) {
            throw new Error('Destinatario, asunto y mensaje son obligatorios');
        }

        this.id = crypto.randomUUID();
        this.destinatarioId = destinatarioId;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.canal = canal;
        this.fechaCreacion = new Date();
        this.estado = 'PENDIENTE';

        // Evento: notificación creada
        this.eventos.push(new NotificacionCreada(
            this.id,
            this.destinatarioId,
            this.canal.getTipo(),
            this.fechaCreacion
        ));
    }

    marcarComoEnviada(): void {
        if (this.estado !== 'PENDIENTE') {
            throw new Error(`No se puede marcar como enviada desde estado: ${this.estado}`);
        }

        this.estado = 'ENVIADA';
        this.fechaEnvio = new Date();
        this.intentosEnvio++;

        this.eventos.push(new NotificacionEnviada(
            this.id,
            this.destinatarioId,
            this.fechaEnvio
        ));
    }

    marcarComoFallida(): void {
        this.intentosEnvio++;
        
        if (this.intentosEnvio >= 3) {
            this.estado = 'FALLIDA';
            this.eventos.push(new NotificacionFallida(
                this.id,
                this.destinatarioId,
                this.intentosEnvio
            ));
        }
    }

    // Getters
    getId(): string { return this.id; }
    getEstado(): string { return this.estado; }
    getCanal(): CanalNotificacion { return this.canal; }
    getMensaje(): string { return this.mensaje; }
    
    getEventosNoPublicados(): DomainEvent[] { return [...this.eventos]; }
    marcarEventosComoPublicados(): void { this.eventos = []; }
}
```

---

# 🔐 BOUNDED CONTEXT: VALIDACIONES

> **Responsabilidad**: Gestionar validaciones documentales y de identidad

```typescript
/**
 * Tipo de Validación - Value Object
 */
class TipoValidacion {
    private static readonly TIPOS = [
        'IDENTIDAD',
        'INGRESOS', 
        'RESIDENCIA',
        'ESTADO_CIVIL',
        'BENEFICIARIO_AUTORIZADO'
    ] as const;

    private readonly tipo: typeof TipoValidacion.TIPOS[number];

    constructor(tipo: string) {
        if (!TipoValidacion.TIPOS.includes(tipo as any)) {
            throw new Error(`Tipo de validación inválido: ${tipo}`);
        }
        this.tipo = tipo as typeof TipoValidacion.TIPOS[number];
    }

    getValue(): string { return this.tipo; }
    
    getDocumentosRequeridos(): string[] {
        const documentos = {
            'IDENTIDAD': ['DNI', 'PASAPORTE'],
            'INGRESOS': ['NOMINA', 'DECLARACION_RENTA'],
            'RESIDENCIA': ['EMPADRONAMIENTO', 'FACTURA_SUMINISTRO'],
            'ESTADO_CIVIL': ['LIBRO_FAMILIA', 'CERTIFICADO_MATRIMONIO'],
            'BENEFICIARIO_AUTORIZADO': ['AUTORIZACION_NOTARIAL']
        };
        return documentos[this.tipo] || [];
    }
}

/**
 * Documento - Value Object
 */
class Documento {
    constructor(
        private readonly tipo: string,
        private readonly nombre: string,
        private readonly url: string,
        private readonly fechaSubida: Date
    ) {
        if (!tipo?.trim() || !nombre?.trim() || !url?.trim()) {
            throw new Error('Tipo, nombre y URL del documento son obligatorios');
        }
    }

    getTipo(): string { return this.tipo; }
    getNombre(): string { return this.nombre; }
    getUrl(): string { return this.url; }
    getFechaSubida(): Date { return this.fechaSubida; }
}

/**
 * Validación - Aggregate Root
 */
class Validacion {
    private readonly id: string;
    private readonly afiliadoId: string;
    private readonly tipo: TipoValidacion;
    private readonly fechaSolicitud: Date;
    private documentos: Documento[] = [];
    private estado: 'PENDIENTE' | 'EN_REVISION' | 'APROBADA' | 'RECHAZADA';
    private fechaResolucion: Date | null = null;
    private comentarios: string | null = null;
    
    private eventos: DomainEvent[] = [];

    constructor(afiliadoId: string, tipo: TipoValidacion) {
        if (!afiliadoId?.trim()) {
            throw new Error('ID de afiliado es obligatorio');
        }

        this.id = crypto.randomUUID();
        this.afiliadoId = afiliadoId;
        this.tipo = tipo;
        this.fechaSolicitud = new Date();
        this.estado = 'PENDIENTE';

        this.eventos.push(new ValidacionSolicitada(
            this.id,
            this.afiliadoId,
            this.tipo.getValue(),
            this.fechaSolicitud
        ));
    }

    adjuntarDocumento(documento: Documento): void {
        if (this.estado !== 'PENDIENTE') {
            throw new Error(`No se pueden adjuntar documentos en estado: ${this.estado}`);
        }

        // Verificar que el tipo de documento es requerido
        const tiposRequeridos = this.tipo.getDocumentosRequeridos();
        if (!tiposRequeridos.includes(documento.getTipo())) {
            throw new Error(`Tipo de documento no requerido: ${documento.getTipo()}`);
        }

        this.documentos.push(documento);
        
        // Si tenemos todos los documentos, pasar a revisión
        if (this.tieneDocumentosCompletos()) {
            this.estado = 'EN_REVISION';
            this.eventos.push(new ValidacionListaParaRevision(
                this.id,
                this.afiliadoId
            ));
        }
    }

    private tieneDocumentosCompletos(): boolean {
        const tiposRequeridos = this.tipo.getDocumentosRequeridos();
        const tiposPresentados = this.documentos.map(d => d.getTipo());
        
        return tiposRequeridos.every(tipo => tiposPresentados.includes(tipo));
    }

    aprobar(comentarios?: string): void {
        if (this.estado !== 'EN_REVISION') {
            throw new Error(`No se puede aprobar desde estado: ${this.estado}`);
        }

        this.estado = 'APROBADA';
        this.fechaResolucion = new Date();
        this.comentarios = comentarios || null;

        this.eventos.push(new ValidacionAprobada(
            this.id,
            this.afiliadoId,
            this.tipo.getValue(),
            this.fechaResolucion
        ));
    }

    rechazar(comentarios: string): void {
        if (!comentarios?.trim()) {
            throw new Error('Comentarios son obligatorios para rechazo');
        }

        if (this.estado !== 'EN_REVISION') {
            throw new Error(`No se puede rechazar desde estado: ${this.estado}`);
        }

        this.estado = 'RECHAZADA';
        this.fechaResolucion = new Date();
        this.comentarios = comentarios;

        this.eventos.push(new ValidacionRechazada(
            this.id,
            this.afiliadoId,
            this.tipo.getValue(),
            comentarios,
            this.fechaResolucion
        ));
    }

    // Getters
    getId(): string { return this.id; }
    getEstado(): string { return this.estado; }
    getTipo(): TipoValidacion { return this.tipo; }
    getDocumentos(): Documento[] { return [...this.documentos]; }
    
    getEventosNoPublicados(): DomainEvent[] { return [...this.eventos]; }
    marcarEventosComoPublicados(): void { this.eventos = []; }
}
```

---

# 🎯 DOMAIN EVENTS Y COORDINACIÓN

> **Eventos de dominio para comunicación entre Bounded Contexts**

```typescript
/**
 * Base para eventos de dominio
 */
abstract class DomainEvent {
    public readonly occurredOn: Date;
    public readonly eventId: string;

    constructor() {
        this.occurredOn = new Date();
        this.eventId = crypto.randomUUID();
    }

    abstract getEventName(): string;
}

/**
 * Eventos del contexto de Afiliados
 */
class AfiliadoRegistradoEvent extends DomainEvent {
    constructor(
        public readonly afiliadoId: string,
        public readonly dni: string,
        public readonly nombreCompleto: string,
        public readonly fechaAlta: Date
    ) { super(); }
    
    getEventName(): string { return 'AfiliadoRegistrado'; }
}

class DireccionCambiadaEvent extends DomainEvent {
    constructor(
        public readonly afiliadoId: string,
        public readonly dni: string,
        public readonly direccionAnterior: string,
        public readonly direccionNueva: string
    ) { super(); }
    
    getEventName(): string { return 'DireccionCambiada'; }
}

class AfiliadoSuspendidoEvent extends DomainEvent {
    constructor(
        public readonly afiliadoId: string,
        public readonly dni: string,
        public readonly motivo: string,
        public readonly fechaSuspension: Date
    ) { super(); }
    
    getEventName(): string { return 'AfiliadoSuspendido'; }
}

/**
 * Eventos del contexto de Beneficios
 */
class BeneficioAsignado extends DomainEvent {
    constructor(
        public readonly beneficioId: string,
        public readonly afiliadoId: string,
        public readonly tipo: string,
        public readonly importe: ImporteMonetario,
        public readonly fechaAsignacion: Date
    ) { super(); }
    
    getEventName(): string { return 'BeneficioAsignado'; }
}

class BeneficioUtilizado extends DomainEvent {
    constructor(
        public readonly beneficioId: string,
        public readonly afiliadoId: string,
        public readonly importeUtilizado: ImporteMonetario,
        public readonly importeAcumulado: ImporteMonetario,
        public readonly fechaUtilizacion: Date
    ) { super(); }
    
    getEventName(): string { return 'BeneficioUtilizado'; }
}

/**
 * Event Handler para coordinación entre contextos
 */
class DomainEventHandler {
    constructor(
        private notificacionService: NotificacionApplicationService,
        private validacionService: ValidacionApplicationService
    ) {}

    /**
     * Cuando se registra un afiliado, crear validaciones automáticas
     */
    async handle(event: AfiliadoRegistradoEvent): Promise<void> {
        // Crear validación de identidad automática
        await this.validacionService.solicitarValidacion(
            event.afiliadoId,
            'IDENTIDAD'
        );

        // Enviar notificación de bienvenida
        await this.notificacionService.enviarBienvenida(
            event.afiliadoId,
            event.nombreCompleto
        );
    }

    /**
     * Cuando se asigna un beneficio, notificar al afiliado
     */
    async handle(event: BeneficioAsignado): Promise<void> {
        await this.notificacionService.notificarBeneficioAsignado(
            event.afiliadoId,
            event.tipo,
            event.importe
        );
    }

    /**
     * Cuando se suspende afiliado, cancelar beneficios activos
     */
    async handle(event: AfiliadoSuspendidoEvent): Promise<void> {
        // Aquí se coordinaría con el contexto de beneficios
        // para cancelar beneficios activos del afiliado
    }
}
```

---

# 🏢 APPLICATION SERVICES (Casos de Uso)

> **Servicios de aplicación específicos por Bounded Context**

```typescript
/**
 * Application Service para Gestión de Afiliados
 * Coordina casos de uso del contexto de afiliados
 */
class AfiliadoApplicationService {
    constructor(
        private repository: AfiliadoRepository,
        private eventBus: DomainEventBus
    ) {}

    async registrarAfiliado(comando: RegistrarAfiliadoCommand): Promise<string> {
        // Verificar que no existe
        const dniAfiliado = new DNIAfiliado(comando.dni);
        const existente = await this.repository.findByDNI(dniAfiliado);
        
        if (existente) {
            throw new Error('Ya existe un afiliado con este DNI');
        }

        // Crear agregado
        const direccion = new DireccionAfiliado(
            comando.via,
            comando.numero,
            comando.ciudad,
            comando.provincia,
            comando.codigoPostal
        );

        const afiliado = new Afiliado(
            dniAfiliado,
            comando.nombre,
            comando.apellidos,
            direccion
        );

        // Persistir
        await this.repository.save(afiliado);

        // Publicar eventos
        await this.publicarEventos(afiliado);

        return afiliado.getId();
    }

    async cambiarDireccion(comando: CambiarDireccionCommand): Promise<void> {
        const afiliado = await this.repository.findById(comando.afiliadoId);
        if (!afiliado) {
            throw new Error('Afiliado no encontrado');
        }

        const nuevaDireccion = new DireccionAfiliado(
            comando.via,
            comando.numero,
            comando.ciudad,
            comando.provincia,
            comando.codigoPostal
        );

        afiliado.cambiarDireccion(nuevaDireccion);
        await this.repository.save(afiliado);
        await this.publicarEventos(afiliado);
    }

    private async publicarEventos(afiliado: Afiliado): Promise<void> {
        const eventos = afiliado.getEventosNoPublicados();
        for (const evento of eventos) {
            await this.eventBus.publish(evento);
        }
        afiliado.marcarEventosComoPublicados();
    }
}

/**
 * Application Service para Gestión de Beneficios
 */
class BeneficioApplicationService {
    constructor(
        private beneficioRepository: BeneficioRepository,
        private afiliadoRepository: AfiliadoRepository,
        private eventBus: DomainEventBus
    ) {}

    async asignarBeneficio(comando: AsignarBeneficioCommand): Promise<string> {
        // Verificar que afiliado existe y está activo
        const afiliado = await this.afiliadoRepository.findById(comando.afiliadoId);
        if (!afiliado) {
            throw new Error('Afiliado no encontrado');
        }

        if (!afiliado.getEstado().isActivo()) {
            throw new Error('Solo se pueden asignar beneficios a afiliados activos');
        }

        // Crear beneficio
        const tipo = new TipoBeneficio(comando.tipo);
        const importe = new ImporteMonetario(comando.importeMaximo);
        const periodo = new PeriodoVigencia(
            comando.fechaInicio,
            comando.fechaFin
        );

        const beneficio = new Beneficio(
            comando.afiliadoId,
            tipo,
            importe,
            periodo
        );

        // Persistir
        await this.beneficioRepository.save(beneficio);
        await this.publicarEventos(beneficio);

        return beneficio.getId();
    }

    async utilizarBeneficio(comando: UtilizarBeneficioCommand): Promise<ImporteMonetario> {
        const beneficio = await this.beneficioRepository.findById(comando.beneficioId);
        if (!beneficio) {
            throw new Error('Beneficio no encontrado');
        }

        const importeSolicitado = new ImporteMonetario(comando.importe);
        const importeCubierto = beneficio.utilizar(importeSolicitado);

        await this.beneficioRepository.save(beneficio);
        await this.publicarEventos(beneficio);

        return importeCubierto;
    }

    private async publicarEventos(beneficio: Beneficio): Promise<void> {
        const eventos = beneficio.getEventosNoPublicados();
        for (const evento of eventos) {
            await this.eventBus.publish(evento);
        }
        beneficio.marcarEventosComoPublicados();
    }
}

/**
 * Application Service para Notificaciones
 */
class NotificacionApplicationService {
    constructor(
        private repository: NotificacionRepository,
        private afiliadoRepository: AfiliadoRepository,
        private eventBus: DomainEventBus
    ) {}

    async enviarBienvenida(afiliadoId: string, nombreCompleto: string): Promise<void> {
        const afiliado = await this.afiliadoRepository.findById(afiliadoId);
        if (!afiliado) {
            throw new Error('Afiliado no encontrado');
        }

        // Crear notificación de bienvenida
        const canal = new CanalNotificacion('EMAIL', 'afiliado@email.com'); // En prod se obtendría del afiliado
        const notificacion = new Notificacion(
            afiliadoId,
            'Bienvenido a la Mutualidad',
            `Estimado/a ${nombreCompleto}, su afiliación ha sido procesada exitosamente.`,
            canal
        );

        await this.repository.save(notificacion);
        await this.publicarEventos(notificacion);
    }

    private async publicarEventos(notificacion: Notificacion): Promise<void> {
        const eventos = notificacion.getEventosNoPublicados();
        for (const evento of eventos) {
            await this.eventBus.publish(evento);
        }
        notificacion.marcarEventosComoPublicados();
    }
}
```

---

# 🎭 FACADE DEL MONOLITO

> **Punto de entrada unificado que coordina todos los Bounded Contexts**

```typescript
/**
 * Facade principal del monolito
 * Proporciona una API unificada integrando todos los contextos
 */
class MutualidadMonolitoFacade {
    constructor(
        private afiliadoService: AfiliadoApplicationService,
        private beneficioService: BeneficioApplicationService,
        private notificacionService: NotificacionApplicationService,
        private validacionService: ValidacionApplicationService
    ) {}

    /**
     * Proceso completo de alta de afiliado
     * Coordina múltiples bounded contexts
     */
    async procesarAltaCompleta(datos: DatosAltaAfiliado): Promise<ResumenAltaAfiliado> {
        try {
            // 1. Registrar afiliado (Contexto: Afiliados)
            const afiliadoId = await this.afiliadoService.registrarAfiliado({
                dni: datos.dni,
                nombre: datos.nombre,
                apellidos: datos.apellidos,
                via: datos.direccion.via,
                numero: datos.direccion.numero,
                ciudad: datos.direccion.ciudad,
                provincia: datos.direccion.provincia,
                codigoPostal: datos.direccion.codigoPostal
            });

            // 2. Asignar beneficio básico (Contexto: Beneficios)
            const beneficioId = await this.beneficioService.asignarBeneficio({
                afiliadoId,
                tipo: 'CONSULTA_MEDICA',
                importeMaximo: 500,
                fechaInicio: new Date(),
                fechaFin: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000) // 1 año
            });

            // 3. Los eventos de dominio se encargan automáticamente de:
            //    - Crear validación de identidad (Contexto: Validaciones)
            //    - Enviar notificación de bienvenida (Contexto: Notificaciones)

            return {
                afiliadoId,
                beneficioId,
                estado: 'REGISTRADO_EXITOSAMENTE',
                mensajeUsuario: `Afiliado ${datos.nombre} ${datos.apellidos} registrado correctamente. Se ha enviado información de bienvenida.`
            };

        } catch (error) {
            return {
                afiliadoId: null,
                beneficioId: null,
                estado: 'ERROR',
                mensajeUsuario: `Error en el registro: ${error.message}`
            };
        }
    }

    /**
     * Proceso de utilización de prestación médica
     * Coordina validaciones, beneficios y notificaciones
     */
    async procesarPrestacionMedica(datos: DatosPrestacion): Promise<ResultadoPrestacion> {
        try {
            // 1. Buscar afiliado activo
            const afiliado = await this.buscarAfiliadoActivo(datos.dni);
            
            // 2. Verificar validaciones requeridas
            const validacionesCompletas = await this.verificarValidaciones(afiliado.getId());
            if (!validacionesCompletas) {
                return {
                    aprobada: false,
                    motivo: 'Validaciones pendientes',
                    importeCubierto: new ImporteMonetario(0)
                };
            }

            // 3. Buscar beneficio aplicable
            const beneficio = await this.buscarBeneficioAplicable(
                afiliado.getId(), 
                datos.tipoPrestacion
            );

            if (!beneficio) {
                return {
                    aprobada: false,
                    motivo: 'No hay beneficios disponibles para este tipo de prestación',
                    importeCubierto: new ImporteMonetario(0)
                };
            }

            // 4. Utilizar beneficio
            const importeCubierto = await this.beneficioService.utilizarBeneficio({
                beneficioId: beneficio.getId(),
                importe: datos.importeTotal
            });

            // 5. Notificación automática vía eventos de dominio

            return {
                aprobada: true,
                motivo: 'Prestación aprobada',
                importeCubierto,
                beneficioUtilizado: beneficio.getId()
            };

        } catch (error) {
            return {
                aprobada: false,
                motivo: `Error: ${error.message}`,
                importeCubierto: new ImporteMonetario(0)
            };
        }
    }

    /**
     * Consulta integrada del estado de afiliado
     * Agrega información de todos los contextos
     */
    async consultarEstadoAfiliado(dni: string): Promise<EstadoCompletoAfiliado> {
        const afiliado = await this.buscarAfiliadoActivo(dni);
        
        // Información de múltiples contextos
        const [beneficios, validaciones, notificaciones] = await Promise.all([
            this.consultarBeneficios(afiliado.getId()),
            this.consultarValidaciones(afiliado.getId()),
            this.consultarHistorialNotificaciones(afiliado.getId())
        ]);

        return {
            afiliado: {
                id: afiliado.getId(),
                dni: afiliado.getDNI().getValue(),
                nombre: afiliado.getFullName(),
                estado: afiliado.getEstado().getValue(),
                direccion: afiliado.getDireccion().toString(),
                fechaAlta: afiliado.getFechaAlta()
            },
            beneficios: beneficios.map(b => ({
                id: b.getId(),
                tipo: b.getTipo().getValue(),
                estado: b.getEstado(),
                disponible: b.getImporteDisponible().toString()
            })),
            validaciones: validaciones.map(v => ({
                tipo: v.getTipo().getValue(),
                estado: v.getEstado()
            })),
            estadisticasNotificaciones: {
                enviadas: notificaciones.filter(n => n.getEstado() === 'ENVIADA').length,
                pendientes: notificaciones.filter(n => n.getEstado() === 'PENDIENTE').length
            }
        };
    }

    // Métodos auxiliares privados...
    private async buscarAfiliadoActivo(dni: string): Promise<Afiliado> {
        const dniObj = new DNIAfiliado(dni);
        const afiliado = await this.afiliadoService.buscarPorDNI(dniObj);
        
        if (!afiliado) {
            throw new Error('Afiliado no encontrado');
        }
        
        if (!afiliado.getEstado().isActivo()) {
            throw new Error('Afiliado no está activo');
        }
        
        return afiliado;
    }

    private async verificarValidaciones(afiliadoId: string): Promise<boolean> {
        // Lógica para verificar validaciones completas
        return true; // Simplificado
    }

    private async buscarBeneficioAplicable(afiliadoId: string, tipo: string): Promise<Beneficio | null> {
        // Lógica para buscar beneficio
        return null; // Simplificado
    }
}
```

---

# ✅ EJEMPLO DE USO COMPLETO

```typescript
/**
 * Demostración del monolito DDD bien diseñado
 */
async function ejemploMonolitoDDD() {
    console.log('=== MONOLITO DDD BIEN DISEÑADO ===\n');

    // Setup (en una aplicación real, esto sería inyección de dependencias)
    const afiliadoRepo = new InMemoryAfiliadoRepository();
    const beneficioRepo = new InMemoryBeneficioRepository();
    const notificacionRepo = new InMemoryNotificacionRepository();
    const validacionRepo = new InMemoryValidacionRepository();
    
    const eventBus = new InMemoryEventBus();
    
    const afiliadoService = new AfiliadoApplicationService(afiliadoRepo, eventBus);
    const beneficioService = new BeneficioApplicationService(beneficioRepo, afiliadoRepo, eventBus);
    const notificacionService = new NotificacionApplicationService(notificacionRepo, afiliadoRepo, eventBus);
    const validacionService = new ValidacionApplicationService(validacionRepo, afiliadoRepo, eventBus);

    // Facade principal
    const mutualidad = new MutualidadMonolitoFacade(
        afiliadoService,
        beneficioService,
        notificacionService,
        validacionService
    );

    try {
        // 1. Proceso de alta completo
        console.log('1. PROCESANDO ALTA COMPLETA...');
        const resultadoAlta = await mutualidad.procesarAltaCompleta({
            dni: '12345678Z',
            nombre: 'Ana',
            apellidos: 'García López',
            direccion: {
                via: 'Calle Alcalá',
                numero: '45',
                ciudad: 'Madrid',
                provincia: 'Madrid',
                codigoPostal: '28014'
            }
        });
        
        console.log('Resultado alta:', resultadoAlta);
        console.log();

        // 2. Consultar estado completo
        console.log('2. CONSULTANDO ESTADO COMPLETO...');
        const estadoCompleto = await mutualidad.consultarEstadoAfiliado('12345678Z');
        console.log('Estado completo:', JSON.stringify(estadoCompleto, null, 2));
        console.log();

        // 3. Procesar prestación médica
        console.log('3. PROCESANDO PRESTACIÓN MÉDICA...');
        const resultadoPrestacion = await mutualidad.procesarPrestacionMedica({
            dni: '12345678Z',
            tipoPrestacion: 'CONSULTA_MEDICA',
            importeTotal: 120
        });
        
        console.log('Resultado prestación:', resultadoPrestacion);
        
    } catch (error) {
        console.error('Error:', error.message);
    }
}

// Ejecutar ejemplo
ejemploMonolitoDDD();
```

---

## 🎯 BENEFICIOS DE ESTA IMPLEMENTACIÓN DDD

### ✅ **Separación Clara de Responsabilidades**
- Cada **Bounded Context** tiene responsabilidades específicas
- Los **Aggregates** mantienen invariantes de negocio específicas
- Los **Value Objects** encapsulan validaciones y reglas

### ✅ **Mantenibilidad**
- Cambios en un contexto no afectan otros
- Fácil agregar nuevos contextos
- Testing independiente por contexto

### ✅ **Escalabilidad Conceptual**
- Equipos pueden trabajar en contextos independientes
- Preparado para eventual migración a microservicios
- Interfaces claras entre contextos

### ✅ **Robustez**
- **Domain Events** para coordinación asíncrona
- Validaciones distribuidas en los Value Objects apropiados
- Manejo de errores específico por dominio

### ✅ **Flexibilidad**
- Fácil modificar reglas de un contexto específico
- Posible evolución independiente de cada bounded context
- Preparado para diferentes estrategias de persistencia

---

## 🔄 COMPARACIÓN: ANTI-PATRÓN vs DDD CORRECTO

| Aspecto | ❌ Anti-Patrón Monolítico | ✅ DDD Monolítico Correcto |
|---------|------------------------|---------------------------|
| **Responsabilidades** | Una clase lo hace todo | Separadas por Bounded Context |
| **Acoplamiento** | Fuerte entre conceptos | Bajo, comunicación vía eventos |
| **Testabilidad** | Difícil por dependencias | Fácil por separación |
| **Mantenimiento** | Cambio afecta múltiples áreas | Cambios localizados |
| **Escalado** | Todo junto | Preparado para separación |
| **Equipos** | Conflictos frecuentes | Trabajo independiente posible |

---

## 🚀 EVOLUCIÓN A MICROSERVICIOS

Esta implementación **está preparada** para evolucionar a microservicios:

1. **Cada Bounded Context** puede convertirse en un microservicio independiente
2. **Los Domain Events** se convertirían en eventos de integración
3. **Los Application Services** se mantendrían como APIs independientes
4. **Los Value Objects y Entities** se preservan en cada servicio

**La arquitectura DDD facilita esta transición manteniendo la lógica de negocio intacta.**