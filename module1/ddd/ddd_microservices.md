# DDD Microservicios - Mutualidad Platform

> **Implementación simplificada con contextos delimitados separados**

## Estructura Microservicios

```typescript
/**
 * VENTAJA: Cada contexto delimitado es independiente
 * - Modelos claros por contexto
 * - Responsabilidades separadas
 * - Equipos pueden trabajar independientemente
 */

// ========================================
// CONTEXTO: AFILIADO
// ========================================

/**
 * VALUE OBJECTS ESPECÍFICOS DE AFILIADO
 */
class DNIAfiliado {
    private readonly valor: string;
    private static readonly PATRON = /^[0-9]{8}[A-Z]$/;

    constructor(valor: string) {
        if (!valor || !DNIAfiliado.PATRON.test(valor)) {
            throw new Error(`DNI inválido: ${valor}`);
        }
        this.valor = valor.toUpperCase();
    }

    getValue(): string { return this.valor; }
}

class DireccionAfiliado {
    constructor(
        private readonly via: string,
        private readonly numero: string,
        private readonly ciudad: string,
        private readonly codigoPostal: string
    ) {}
}

/**
 * AGGREGATE ROOT DEL CONTEXTO AFILIADO
 * Responsabilidad: Solo gestión de afiliados
 */
class Afiliado {
    private readonly id: string;
    private dni: DNIAfiliado;
    private nombre: string;
    private apellidos: string;
    private direccion: DireccionAfiliado;
    private fechaAlta: Date;
    private estado: 'ACTIVO' | 'INACTIVO' | 'SUSPENDIDO';

    constructor(
        dni: DNIAfiliado,
        nombre: string,
        apellidos: string,
        direccion: DireccionAfiliado
    ) {
        this.id = crypto.randomUUID();
        this.dni = dni;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.direccion = direccion;
        this.fechaAlta = new Date();
        this.estado = 'ACTIVO';
    }

    // Métodos específicos del contexto AFILIADO
    cambiarDireccion(nuevaDireccion: DireccionAfiliado): void {
        this.direccion = nuevaDireccion;
    }

    darBaja(): void {
        this.estado = 'INACTIVO';
    }

    estaActivo(): boolean {
        return this.estado === 'ACTIVO';
    }

    getId(): string { return this.id; }
    getDni(): string { return this.dni.getValue(); }
    getNombreCompleto(): string { return `${this.nombre} ${this.apellidos}`; }
}

/**
 * REPOSITORIO ESPECÍFICO DE AFILIADO
 */
interface AfiliadoRepository {
    guardar(afiliado: Afiliado): Promise<void>;
    buscarPorDni(dni: string): Promise<Afiliado | null>;
    buscarPorId(id: string): Promise<Afiliado | null>;
}

/**
 * SERVICIO DE APLICACIÓN - CONTEXTO AFILIADO
 */
class AfiliadoService {
    constructor(private readonly repository: AfiliadoRepository) {}

    async altaAfiliado(
        dniStr: string,
        nombre: string,
        apellidos: string,
        via: string,
        numero: string,
        ciudad: string,
        cp: string
    ): Promise<any> {
        try {
            // Validar que no existe
            const existente = await this.repository.buscarPorDni(dniStr);
            if (existente) {
                throw new Error('Afiliado ya existe');
            }

            // Crear afiliados
            const dni = new DNIAfiliado(dniStr);
            const direccion = new DireccionAfiliado(via, numero, ciudad, cp);
            const afiliado = new Afiliado(dni, nombre, apellidos, direccion);

            // Guardar
            await this.repository.guardar(afiliado);

            return {
                success: true,
                id: afiliado.getId(),
                dni: afiliado.getDni(),
                nombre: afiliado.getNombreCompleto()
            };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }
}

// ========================================
// CONTEXTO: BENEFICIO
// ========================================

/**
 * VALUE OBJECTS ESPECÍFICOS DE BENEFICIO
 */
class PeriodoVigencia {
    constructor(
        private readonly fechaInicio: Date,
        private readonly fechaFin?: Date
    ) {
        if (fechaFin && fechaFin < fechaInicio) {
            throw new Error('Fecha fin debe ser posterior a inicio');
        }
    }

    estaVigente(): boolean {
        const ahora = new Date();
        return ahora >= this.fechaInicio && 
               (!this.fechaFin || ahora <= this.fechaFin);
    }
}

class ImporteBeneficio {
    constructor(private readonly valor: number) {
        if (valor < 0) {
            throw new Error('Importe no puede ser negativo');
        }
    }

    getValue(): number { return this.valor; }
}

/**
 * ENTIDAD BENEFICIO (no es Aggregate Root)
 * Pertenece al Aggregate de Beneficio
 */
class AsignacionBeneficio {
    constructor(
        private readonly id: string,
        private readonly tipo: string,
        private readonly descripcion: string,
        private readonly periodo: PeriodoVigencia,
        private readonly importe: ImporteBeneficio
    ) {}

    estaVigente(): boolean {
        return this.periodo.estaVigente();
    }

    getTipo(): string { return this.tipo; }
    getImporte(): number { return this.importe.getValue(); }
}

/**
 * AGGREGATE ROOT DEL CONTEXTO BENEFICIO
 */
class Beneficio {
    private readonly id: string;
    private readonly afiliadoId: string;
    private asignaciones: AsignacionBeneficio[];

    constructor(afiliadoId: string) {
        this.id = crypto.randomUUID();
        this.afiliadoId = afiliadoId;
        this.asignaciones = [];
    }

    /**
     * Asignar beneficio
     * Responsabilidad: Solo gestión de beneficios
     */
    asignarBeneficio(
        tipo: string,
        descripcion: string,
        importe: number
    ): void {
        const asignacion = new AsignacionBeneficio(
            crypto.randomUUID(),
            tipo,
            descripcion,
            new PeriodoVigencia(new Date()),
            new ImporteBeneficio(importe)
        );
        
        this.asignaciones.push(asignacion);
    }

    /**
     * Verificar si puede recibir prestación
     * Responsabilidad: Solo lógica de beneficios
     */
    puedeRecibirPrestacion(tipoBeneficio: string): boolean {
        const asignacion = this.asignaciones
            .find(a => a.getTipo() === tipoBeneficio && a.estaVigente());
        
        return asignacion !== undefined;
    }

    getAfiliadoId(): string { return this.afiliadoId; }
    getAsignaciones(): AsignacionBeneficio[] { return this.asignaciones; }
}

/**
 * REPOSITORIO ESPECÍFICO DE BENEFICIO
 */
interface BeneficioRepository {
    guardar(beneficio: Beneficio): Promise<void>;
    buscarPorAfiliado(afiliadoId: string): Promise<Beneficio | null>;
}

/**
 * SERVICIO DE APLICACIÓN - CONTEXTO BENEFICIO
 */
class BeneficioService {
    constructor(
        private readonly repository: BeneficioRepository,
        private readonly afiliadoRepository: AfiliadoRepository
    ) {}

    /**
     * Asignar beneficio a afiliado
     * Responsabilidad: Solo coordinación con AfiliadoService
     */
    async asignarBeneficio(
        afiliadoId: string,
        tipo: string,
        descripcion: string,
        importe: number
    ): Promise<any> {
        try {
            // Verificar que el afiliado existe y está activo
            const afiliado = await this.afiliadoRepository.buscarPorId(afiliadoId);
            if (!afiliado || !afiliado.estaActivo()) {
                throw new Error('Afiliado no válido o inactivo');
            }

            // Buscar o crear beneficio
            let beneficio = await this.repository.buscarPorAfiliado(afiliadoId);
            if (!beneficio) {
                beneficio = new Beneficio(afiliadoId);
            }

            // Asignar beneficio
            beneficio.asignarBeneficio(tipo, descripcion, importe);
            
            // Guardar
            await this.repository.guardar(beneficio);

            return {
                success: true,
                mensaje: `Beneficio ${tipo} asignado correctamente`
            };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }
}

// ========================================
// CONTEXTO: NOTIFICACIÓN
// ========================================

/**
 * VALUE OBJECTS ESPECÍFICOS DE NOTIFICACIÓN
 */
class CanalComunicacion {
    constructor(
        private readonly tipo: 'EMAIL' | 'SMS' | 'POSTAL',
        private readonly valor: string
    ) {
        if (!['EMAIL', 'SMS', 'POSTAL'].includes(tipo)) {
            throw new Error('Canal inválido');
        }
    }

    getTipo(): string { return this.tipo; }
    getValor(): string { return this.valor; }
}

class ContenidoMensaje {
    constructor(private readonly texto: string) {
        if (!texto || texto.trim().length === 0) {
            throw new Error('Mensaje no puede estar vacío');
        }
    }

    getValue(): string { return this.texto; }
}

/**
 * AGGREGATE ROOT DEL CONTEXTO NOTIFICACIÓN
 */
class Notificacion {
    private readonly id: string;
    private readonly afiliadoId: string;
    private canal: CanalComunicacion;
    private contenido: ContenidoMensaje;
    private fechaEnvio: Date;
    private estado: 'PENDIENTE' | 'ENVIADA' | 'FALLIDA';

    constructor(
        afiliadoId: string,
        canal: CanalComunicacion,
        contenido: ContenidoMensaje
    ) {
        this.id = crypto.randomUUID();
        this.afiliadoId = afiliadoId;
        this.canal = canal;
        this.contenido = contenido;
        this.fechaEnvio = new Date();
        this.estado = 'PENDIENTE';
    }

    /**
     * Enviar notificación
     * Responsabilidad: Solo gestión de notificaciones
     */
    enviar(): void {
        // Lógica específica de envío por canal
        switch (this.canal.getTipo()) {
            case 'EMAIL':
                this.enviarEmail();
                break;
            case 'SMS':
                this.enviarSms();
                break;
            case 'POSTAL':
                this.enviarPostal();
                break;
        }
    }

    private enviarEmail(): void {
        // Simulación de envío por email
        console.log(`Enviando email a ${this.canal.getValor()}: ${this.contenido.getValue()}`);
        this.estado = 'ENVIADA';
    }

    private enviarSms(): void {
        // Simulación de envío por SMS
        console.log(`Enviando SMS a ${this.canal.getValor()}: ${this.contenido.getValue()}`);
        this.estado = 'ENVIADA';
    }

    private enviarPostal(): void {
        // Simulación de envío postal
        console.log(`Enviando carta a dirección registrada: ${this.contenido.getValue()}`);
        this.estado = 'ENVIADA';
    }

    getAfiliadoId(): string { return this.afiliadoId; }
    getEstado(): string { return this.estado; }
}

/**
 * REPOSITORIO ESPECÍFICO DE NOTIFICACIÓN
 */
interface NotificacionRepository {
    guardar(notificacion: Notificacion): Promise<void>;
    buscarPorAfiliado(afiliadoId: string): Promise<Notificacion[]>;
}

/**
 * SERVICIO DE APLICACIÓN - CONTEXTO NOTIFICACIÓN
 */
class NotificacionService {
    constructor(
        private readonly repository: NotificacionRepository,
        private readonly afiliadoRepository: AfiliadoRepository
    ) {}

    /**
     * Enviar notificación a afiliado
     * Responsabilidad: Solo coordinación
     */
    async enviarNotificacion(
        afiliadoId: string,
        tipoCanal: 'EMAIL' | 'SMS' | 'POSTAL',
        valorCanal: string,
        mensaje: string
    ): Promise<any> {
        try {
            // Verificar afiliado (solo para verificar que existe)
            const afiliado = await this.afiliadoRepository.buscarPorId(afiliadoId);
            if (!afiliado) {
                throw new Error('Afiliado no encontrado');
            }

            // Crear notificación
            const canal = new CanalComunicacion(tipoCanal, valorCanal);
            const contenido = new ContenidoMensaje(mensaje);
            const notificacion = new Notificacion(afiliadoId, canal, contenido);

            // Enviar
            notificacion.enviar();
            
            // Guardar
            await this.repository.guardar(notificacion);

            return {
                success: true,
                estado: notificacion.getEstado()
            };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }
}

// ========================================
// CONTEXTO: VALIDACIÓN
// ========================================

/**
 * VALUE OBJECTS ESPECÍFICOS DE VALIDACIÓN
 */
class TipoValidacion {
    constructor(private readonly tipo: string) {
        const tiposValidos = ['IDENTIDAD', 'DOMICILIO', 'MEDICA', 'ECONOMICA'];
        if (!tiposValidos.includes(tipo)) {
            throw new Error(`Tipo de validación no válido: ${tipo}`);
        }
    }

    getValue(): string { return this.tipo; }
}

class DocumentoValidacion {
    constructor(
        private readonly tipo: string,
        private readonly url: string,
        private readonly fechaSubida: Date
    ) {}
}

/**
 * AGGREGATE ROOT DEL CONTEXTO VALIDACIÓN
 */
class SolicitudValidacion {
    private readonly id: string;
    private readonly afiliadoId: string;
    private tipo: TipoValidacion;
    private documentos: DocumentoValidacion[];
    private estado: 'PENDIENTE' | 'COMPLETADA' | 'RECHAZADA';
    private fechaSolicitud: Date;
    private fechaProcesamiento?: Date;

    constructor(
        afiliadoId: string,
        tipo: TipoValidacion,
        documentos: Array<{ tipo: string; url: string }>
    ) {
        this.id = crypto.randomUUID();
        this.afiliadoId = afiliadoId;
        this.tipo = tipo;
        this.documentos = documentos.map(doc => 
            new DocumentoValidacion(doc.tipo, doc.url, new Date())
        );
        this.estado = 'PENDIENTE';
        this.fechaSolicitud = new Date();
    }

    /**
     * Procesar validación
     * Responsabilidad: Solo gestión de validaciones
     */
    procesar(aprobada: boolean, observaciones?: string): void {
        if (this.estado !== 'PENDIENTE') {
            throw new Error('Solicitud ya procesada');
        }

        this.estado = aprobada ? 'COMPLETADA' : 'RECHAZADA';
        this.fechaProcesamiento = new Date();

        if (observaciones) {
            console.log(`Validación ${this.tipo.getValue()} procesada: ${observaciones}`);
        }
    }

    getAfiliadoId(): string { return this.afiliadoId; }
    getTipo(): string { return this.tipo.getValue(); }
    getEstado(): string { return this.estado; }
}

/**
 * REPOSITORIO ESPECÍFICO DE VALIDACIÓN
 */
interface ValidacionRepository {
    guardar(solicitud: SolicitudValidacion): Promise<void>;
    buscarPorAfiliado(afiliadoId: string): Promise<SolicitudValidacion[]>;
}

/**
 * SERVICIO DE APLICACIÓN - CONTEXTO VALIDACIÓN
 */
class ValidacionService {
    constructor(
        private readonly repository: ValidacionRepository,
        private readonly afiliadoRepository: AfiliadoRepository
    ) {}

    /**
     * Solicitar validación
     * Responsabilidad: Solo coordinación
     */
    async solicitarValidacion(
        afiliadoId: string,
        tipo: string,
        documentos: Array<{ tipo: string; url: string }>
    ): Promise<any> {
        try {
            // Verificar afiliado
            const afiliado = await this.afiliadoRepository.buscarPorId(afiliadoId);
            if (!afiliado || !afiliado.estaActivo()) {
                throw new Error('Afiliado no válido o inactivo');
            }

            // Crear solicitud
            const tipoValidacion = new TipoValidacion(tipo);
            const solicitud = new SolicitudValidacion(afiliadoId, tipoValidacion, documentos);

            // Guardar
            await this.repository.guardar(solicitud);

            return {
                success: true,
                solicitudId: solicitud.id,
                tipo: solicitud.getTipo()
            };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }
}

// ========================================
// COORDINACIÓN ENTRE CONTEXTOS
// ========================================

/**
 * SERVICE LOCATOR o API GATEWAY
 * Responsabilidad: Coordinar múltiples contextos
 */
class MutualidadOrchestrator {
    private afiliadoService: AfiliadoService;
    private beneficioService: BeneficioService;
    private notificacionService: NotificacionService;
    private validacionService: ValidacionService;

    constructor(
        afiliadoRepo: AfiliadoRepository,
        beneficioRepo: BeneficioRepository,
        notificacionRepo: NotificacionRepository,
        validacionRepo: ValidacionRepository
    ) {
        this.afiliadoService = new AfiliadoService(afiliadoRepo);
        this.beneficioService = new BeneficioService(beneficioRepo, afiliadoRepo);
        this.notificacionService = new NotificacionService(notificacionRepo, afiliadoRepo);
        this.validacionService = new ValidacionService(validacionRepo, afiliadoRepo);
    }

    /**
     * Alta completa de afiliado (coordina todos los contextos)
     */
    async altaCompletaAfiliado(datos: {
        dni: string;
        nombre: string;
        apellidos: string;
        via: string;
        numero: string;
        ciudad: string;
        cp: string;
        email: string;
    }): Promise<any> {
        try {
            // 1. Crear afiliado
            const resultadoAfiliado = await this.afiliadoService.altaAfiliado(
                datos.dni, datos.nombre, datos.apellidos,
                datos.via, datos.numero, datos.ciudad, datos.cp
            );

            if (!resultadoAfiliado.success) {
                return resultadoAfiliado;
            }

            const afiliadoId = resultadoAfiliado.id;

            // 2. Asignar beneficio básico
            await this.beneficioService.asignarBeneficio(
                afiliadoId, 'BASICO', 'Cobertura básica', 0
            );

            // 3. Configurar notificación por email
            await this.notificacionService.enviarNotificacion(
                afiliadoId, 'EMAIL', datos.email,
                `Bienvenido ${datos.nombre}, tu alta ha sido procesada`
            );

            // 4. Solicitar validación de identidad
            await this.validacionService.solicitarValidacion(
                afiliadoId, 'IDENTIDAD', [
                    { tipo: 'DNI', url: `/docs/${datos.dni}.pdf` }
                ]
            );

            return {
                success: true,
                afiliadoId,
                mensaje: 'Alta completada en todos los contextos'
            };

        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Procesar prestación (coordina Beneficio + Notificación)
     */
    async procesarPrestacion(dni: string, tipoBeneficio: string): Promise<any> {
        try {
            // 1. Buscar afiliado
            const afiliado = await this.afiliadoService['repository'].buscarPorDni(dni);
            if (!afiliado) {
                throw new Error('Afiliado no encontrado');
            }

            // 2. Verificar y asignar beneficio
            const puedeRecibir = await this.beneficioService['repository']
                .buscarPorAfiliado(afiliado.getId());

            if (!puedeRecibir || !puedeRecibir.puedeRecibirPrestacion(tipoBeneficio)) {
                await this.notificacionService.enviarNotificacion(
                    afiliado.getId(), 'EMAIL', 'test@email.com',
                    `Prestación ${tipoBeneficio} denegada`
                );
                return { success: false, motivo: 'No cumple requisitos' };
            }

            // 3. Asignar prestación
            await this.beneficioService.asignarBeneficio(
                afiliado.getId(), tipoBeneficio, 'Prestación médica', 100
            );

            // 4. Notificar éxito
            await this.notificacionService.enviarNotificacion(
                afiliado.getId(), 'EMAIL', 'test@email.com',
                `Prestación ${tipoBeneficio} aprobada: 100€`
            );

            return { success: true, importe: 100 };

        } catch (error) {
            return { success: false, error: error.message };
        }
    }
}

// ========================================
// REPOSITORIOS EN MEMORIA (EJEMPLO)
// ========================================

class InMemoryAfiliadoRepository implements AfiliadoRepository {
    private afiliados = new Map<string, Afiliado>();

    async guardar(afiliado: Afiliado): Promise<void> {
        this.afiliados.set(afiliado.getDni(), afiliado);
    }

    async buscarPorDni(dni: string): Promise<Afiliado | null> {
        return this.afiliados.get(dni) || null;
    }

    async buscarPorId(id: string): Promise<Afiliado | null> {
        for (const afiliado of this.afiliados.values()) {
            if (afiliado.getId() === id) {
                return afiliado;
            }
        }
        return null;
    }
}

// Implementaciones similares para otros repositorios...

// ========================================
// USO DE MICROSERVICIOS
// ========================================

/**
 * EJEMPLO DE USO:
 * Muestra cómo cada contexto es independiente
 */
async function ejemploUsoMicroservicios() {
    // Crear repositorios
    const afiliadoRepo = new InMemoryAfiliadoRepository();
    // const beneficioRepo = new InMemoryBeneficioRepository();
    // const notificacionRepo = new InMemoryNotificacionRepository();
    // const validacionRepo = new InMemoryValidacionRepository();

    // Crear orquestador
    const orchestrator = new MutualidadOrchestrator(
        afiliadoRepo,
        null as any, // beneficioRepo
        null as any, // notificacionRepo
        null as any  // validacionRepo
    );

    console.log('=== ALTA COMPLETA ===');
    const alta = await orchestrator.altaCompletaAfiliado({
        dni: '12345678A',
        nombre: 'Juan',
        apellidos: 'Pérez',
        via: 'Calle Mayor',
        numero: '123',
        ciudad: 'Madrid',
        cp: '28001',
        email: 'juan.perez@email.com'
    });
    console.log(alta);

    console.log('\n=== PROCESAR PRESTACIÓN ===');
    const prestacion = await orchestrator.procesarPrestacion('12345678A', 'CONSULTA');
    console.log(prestacion);
}

/**
 * VENTAJAS DE LOS MICROSERVICIOS CON DDD:
 * 
 * 1. SINGLE RESPONSIBILITY PRINCIPLE
 *    - Cada contexto tiene una responsabilidad clara
 *    - AfiliadoService solo gestiona afiliados
 *    - BeneficioService solo gestiona beneficios
 * 
 * 2. BAJO ACOPLAMIENTO
 *    - Cambios en un contexto no afectan otros
 *    - Interfaces claras entre contextos
 * 
 * 3. ESCALADO INDEPENDIENTE
 *    - Si necesitamos más rendimiento para notificaciones,
 *      solo escalamos ese microservicio
 * 
 * 4. EQUIPOS INDEPENDIENTES
 *    - Equipo de Afiliado puede trabajar sin afectar otros
 *    - Git conflicts reducidos
 * 
 * 5. TESTING SIMPLIFICADO
 *    - Tests unitarios fáciles por bajo acoplamiento
 *    - Mocking de dependencias sencillo
 * 
 * 6. LENGUAJE UBICUO CLARO
 *    - "Afiliado" significa lo mismo en todo el contexto Afiliado
 *    - "Beneficio" tiene significado específico en contexto Beneficio
 * 
 * 7. BOUNDED CONTEXTS CLAROS
 *    - Cada microservicio = un Bounded Context
 *    - Límites definidos y respetados
 * 
 * 8. TOLERANCIA A FALLOS
 *    - Fallo en Notificación no afecta Afiliados
 *    - Resiliencia por servicio
 */
