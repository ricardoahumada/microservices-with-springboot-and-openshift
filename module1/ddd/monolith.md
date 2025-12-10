# DDD Monolítico - Mutualidad Platform

> **Implementación simplificada mostrando TODOS los conceptos en un solo modelo**

## Estructura Monolítica

```typescript
/**
 * PROBLEMA: Un solo modelo maneja TODOS los contextos
 * - Mezcla conceptos de Afiliado, Beneficio, Notificación
 * - Difícil de mantener y escalar
 * - Reglas de negocio mezcladas
 */

// ========================================
// VALUE OBJECTS (Todos juntos)
// ========================================

/**
 * DNI - Value Object
 * Problema: Mismo concepto usado en múltiples contextos
 */
class DNI {
    private readonly valor: string;
    private static readonly PATRON = /^[0-9]{8}[A-Z]$/;

    constructor(valor: string) {
        if (!valor || !DNI.PATRON.test(valor)) {
            throw new Error(`DNI inválido: ${valor}`);
        }
        this.valor = valor.toUpperCase();
    }

    getValue(): string { return this.valor; }
    equals(other: DNI): boolean { return this.valor === other.valor; }
}

/**
 * Dirección - Value Object
 * Problema: Usado para múltiples contextos
 */
class Direccion {
    constructor(
        private readonly via: string,
        private readonly numero: string,
        private readonly ciudad: string,
        private readonly codigoPostal: string
    ) {
        if (!via || !ciudad || !codigoPostal) {
            throw new Error('Dirección incompleta');
        }
    }
}

/**
 * Canal de Notificación - Value Object
 * Problema: Mezclado con conceptos de afiliación
 */
class CanalNotificacion {
    constructor(
        private readonly tipo: 'EMAIL' | 'SMS' | 'POSTAL',
        private readonly valor: string
    ) {
        if (!['EMAIL', 'SMS', 'POSTAL'].includes(tipo)) {
            throw new Error('Canal inválido');
        }
    }
}

/**
 * Período de Vigencia - Value Object
 * Problema: Mezclado con datos de afiliado
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
}

// ========================================
// ENTITIES (Mezcladas en una sola clase)
// ========================================

/**
 * PROBLEMA: UNA SOLA CLASE QUE HACE DE TODO
 * - Gestiona afiliación
 * - Gestiona beneficios
 * - Gestiona notificaciones
 * - Gestiona validaciones
 * 
 * Esto viola el principio de Responsabilidad Única
 */
class MutualidadUsuario {
    // === PROPIEDADES DE AFILIADO ===
    private id: string;
    private dni: DNI;
    private nombre: string;
    private apellidos: string;
    private fechaAlta: Date;
    private direccion: Direccion;
    private estado: 'ACTIVO' | 'INACTIVO' | 'SUSPENDIDO';

    // === PROPIEDADES DE BENEFICIO ===
    private beneficios: Array<{
        tipo: string;
        descripcion: string;
        periodo: PeriodoVigencia;
        importe: number;
    }>;

    private tieneCarencia: boolean;
    private diasCarencia: number;

    // === PROPIEDADES DE NOTIFICACIÓN ===
    private canales: CanalNotificacion[];
    private ultimaNotificacion?: Date;
    private notificacionesEnviadas: Array<{
        canal: string;
        fecha: Date;
        mensaje: string;
    }>;

    // === PROPIEDADES DE VALIDACIÓN ===
    private validacionesRequeridas: Array<{
        tipo: string;
        fecha: Date;
        estado: 'PENDIENTE' | 'COMPLETADA' | 'RECHAZADA';
    }>;

    private documentosValidacion: Array<{
        tipo: string;
        url: string;
        fechaSubida: Date;
    }>;

    constructor(
        dni: DNI,
        nombre: string,
        apellidos: string,
        direccion: Direccion
    ) {
        // Inicialización masiva de propiedades
        this.id = crypto.randomUUID();
        this.dni = dni;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.direccion = direccion;
        this.fechaAlta = new Date();
        this.estado = 'ACTIVO';
        
        // Arrays vacíos para diferentes contextos
        this.beneficios = [];
        this.canales = [];
        this.notificacionesEnviadas = [];
        this.validacionesRequeridas = [];
        this.documentosValidacion = [];
        this.tieneCarencia = false;
        this.diasCarencia = 0;
    }

    // ========================================
    // MÉTODOS DE AFILIADO (Contexto 1)
    // ========================================
    
    /**
     * Cambiar dirección
     * Problema: Mezclado con otros contextos
     */
    cambiarDireccion(nuevaDireccion: Direccion): void {
        this.direccion = nuevaDireccion;
        // Mezclado: También disparamos notificación
        this.crearNotificacionCambio('EMAIL', 'Dirección actualizada');
    }

    /**
     * Dar de baja
     * Problema: Lógica de negocio mezclada
     */
    darBaja(): void {
        this.estado = 'INACTIVO';
        
        // Mezclado: También cancelamos beneficios
        this.beneficios.forEach(beneficio => {
            beneficio.periodo = new PeriodoVigencia(
                beneficio.periodo.getFechaInicio(),
                new Date()
            );
        });
        
        // Mezclado: Y enviamos notificación
        this.enviarNotificacion('Baja procesada correctamente');
    }

    // ========================================
    // MÉTODOS DE BENEFICIO (Contexto 2)
    // ========================================
    
    /**
     * Asignar beneficio
     * Problema: Lógica mezclada con afiliación
     */
    asignarBeneficio(
        tipo: string,
        descripcion: string,
        importe: number
    ): void {
        // Validación de afiliación (mezclada)
        if (this.estado !== 'ACTIVO') {
            throw new Error('Solo afiliados activos pueden recibir beneficios');
        }

        // Verificar carencia (lógica mezclada)
        if (this.tieneCarencia) {
            const diasDesdeAlta = Math.floor(
                (new Date().getTime() - this.fechaAlta.getTime()) / 
                (1000 * 60 * 60 * 24)
            );
            
            if (diasDesdeAlta < this.diasCarencia) {
                throw new Error('Afiliado en período de carencia');
            }
        }

        // Asignar beneficio
        this.beneficios.push({
            tipo,
            descripcion,
            periodo: new PeriodoVigencia(new Date()),
            importe
        });

        // Mezclado: Notificar asignación
        this.enviarNotificacion(`Beneficio ${tipo} asignado`);
    }

    /**
     * Verificar prestación
     * Problema: Método mezcla lógica de beneficio con validación
     */
    verificarPrestacion(tipoBeneficio: string): boolean {
        const beneficio = this.beneficios.find(b => b.tipo === tipoBeneficio);
        
        if (!beneficio) return false;

        // Verificar vigencia (mezclado con lógica de afiliado)
        if (this.estado !== 'ACTIVO') return false;
        
        // Verificar carencia (mezclado con afiliación)
        if (this.tieneCarencia) {
            const diasDesdeAlta = Math.floor(
                (new Date().getTime() - this.fechaAlta.getTime()) / 
                (1000 * 60 * 60 * 24)
            );
            if (diasDesdeAlta < this.diasCarencia) return false;
        }

        return true;
    }

    // ========================================
    // MÉTODOS DE NOTIFICACIÓN (Contexto 3)
    // ========================================
    
    /**
     * Añadir canal de notificación
     * Problema: Mezclado con datos de afiliado
     */
    agregarCanalNotificacion(canal: CanalNotificacion): void {
        // Verificar que el afiliado esté activo (mezclado)
        if (this.estado !== 'ACTIVO') {
            throw new Error('Solo afiliados activos pueden recibir notificaciones');
        }
        
        this.canales.push(canal);
    }

    /**
     * Enviar notificación
     * Problema: Lógica mezclada con múltiples contextos
     */
    private enviarNotificacion(mensaje: string): void {
        // Enviar por todos los canales activos (mezclado con afiliación)
        if (this.estado === 'ACTIVO') {
            this.canales.forEach(canal => {
                this.notificacionesEnviadas.push({
                    canal: canal.getTipo(),
                    fecha: new Date(),
                    mensaje
                });
            });
            this.ultimaNotificacion = new Date();
        }
    }

    /**
     * Notificar cambio de dirección
     * Problema: Método específico mezclado
     */
    private crearNotificacionCambio(tipo: string, mensaje: string): void {
        // Lógica mezclada: notificación + validación + afiliado
        this.enviarNotificacion(mensaje);
    }

    // ========================================
    // MÉTODOS DE VALIDACIÓN (Contexto 4)
    // ========================================
    
    /**
     * Solicitar validación
     * Problema: Mezclado con conceptos de otros contextos
     */
    solicitarValidacion(tipo: string, documentos: Array<{ tipo: string; url: string }>): void {
        // Validar que el afiliado esté activo (mezclado)
        if (this.estado !== 'ACTIVO') {
            throw new Error('Solo afiliados activos pueden solicitar validaciones');
        }

        // Almacenar documentos (mezclado con afiliación)
        documentos.forEach(doc => {
            this.documentosValidacion.push({
                ...doc,
                fechaSubida: new Date()
            });
        });

        // Crear solicitud (mezclado con beneficio)
        this.validacionesRequeridas.push({
            tipo,
            fecha: new Date(),
            estado: 'PENDIENTE'
        });

        // Notificar (mezclado con notificación)
        this.enviarNotificacion(`Validación ${tipo} solicitada`);
    }

    /**
     * Procesar validación
     * Problema: Lógica mezclada con múltiples contextos
     */
    procesarValidacion(tipo: string, aprobada: boolean): void {
        const validacion = this.validacionesRequeridas
            .find(v => v.tipo === tipo && v.estado === 'PENDIENTE');
        
        if (!validacion) {
            throw new Error('Validación no encontrada');
        }

        validacion.estado = aprobada ? 'COMPLETADA' : 'RECHAZADA';

        // Mezclado: Si es aprobada, puede afectar beneficios
        if (aprobada) {
            this.enviarNotificacion(`Validación ${tipo} aprobada`);
        } else {
            this.enviarNotificacion(`Validación ${tipo} rechazada`);
        }
    }

    // ========================================
    // MÉTODOS DE REPORTE (Mezclados)
    // ========================================
    
    /**
     * Obtener resumen completo
     * Problema: Un método que debe conocer TODO
     */
    getResumen(): any {
        return {
            afiliado: {
                id: this.id,
                dni: this.dni.getValue(),
                nombre: `${this.nombre} ${this.apellidos}`,
                estado: this.estado,
                fechaAlta: this.fechaAlta
            },
            beneficios: this.beneficios.length,
            notificacionesEnviadas: this.notificacionesEnviadas.length,
            validacionesPendientes: this.validacionesRequeridas
                .filter(v => v.estado === 'PENDIENTE').length,
            documentos: this.documentosValidacion.length
        };
    }
}

// ========================================
// SERVICIO DE APLICACIÓN (Todo en uno)
// ========================================

/**
 * PROBLEMA: Un solo servicio que maneja TODOS los casos de uso
 * - Violación del principio de Responsabilidad Única
 * - Dificultad para testing
 * - Acoplamiento fuerte entre contextos
 */
class MutualidadMonoliticaService {
    /**
     * Dar de alta un nuevo afiliado
     * Problema: Mezcla múltiples responsabilidades
     */
    altaAfiliado(
        dniStr: string,
        nombre: string,
        apellidos: string,
        via: string,
        numero: string,
        ciudad: string,
        cp: string
    ): any {
        try {
            // Crear value objects
            const dni = new DNI(dniStr);
            const direccion = new Direccion(via, numero, ciudad, cp);
            
            // Crear usuario (que incluye TODA la lógica)
            const usuario = new MutualidadUsuario(dni, nombre, apellidos, direccion);
            
            // Mezclado: Configurar beneficios por defecto
            usuario.asignarBeneficio('BASICO', 'Cobertura básica', 0);
            
            // Mezclado: Configurar notificaciones
            usuario.agregarCanalNotificacion(new CanalNotificacion('EMAIL', `${nombre}.${apellidos}@email.com`));
            
            // Mezclado: Solicitar validaciones iniciales
            usuario.solicitarValidacion('IDENTIDAD', [
                { tipo: 'DNI', url: '/docs/dni.pdf' }
            ]);
            
            return {
                success: true,
                usuario: usuario.getResumen()
            };
            
        } catch (error) {
            return {
                success: false,
                error: error.message
            };
        }
    }

    /**
     * Procesar prestación médica
     * Problema: Un método que debe coordinar múltiples contextos
     */
    procesarPrestacion(dniStr: string, tipoBeneficio: string): any {
        // Problema: Necesitamos encontrar al usuario en una "base de datos" monolítica
        const usuario = this.buscarUsuario(dniStr);
        
        if (!usuario) {
            throw new Error('Usuario no encontrado');
        }

        // Problema: Verificar múltiples cosas mezcladas
        const puedeRecibir = usuario.verificarPrestacion(tipoBeneficio);
        
        if (!puedeRecibir) {
            // Problema: Notificación de error mezclada con lógica
            usuario.enviarNotificacion('Prestación denegada');
            return { success: false, motivo: 'No cumple requisitos' };
        }

        // Problema: Asignar prestación (que afecta múltiples contextos)
        usuario.asignarBeneficio(tipoBeneficio, 'Prestación médica', 100);
        
        return { success: true, importe: 100 };
    }

    /**
     * Buscar usuario (simulado)
     * Problema: En monolito real, sería una consulta compleja a BD única
     */
    private buscarUsuario(dniStr: string): MutualidadUsuario | null {
        // En un monolito real, esto sería una consulta a BD
        // Por simplicidad, retornamos null
        return null;
    }
}

// ========================================
// USO DEL MONOLITO
// ========================================

/**
 * EJEMPLO DE USO:
 * Muestra cómo un monolito mezcla todas las responsabilidades
 */
function ejemploUsoMonolito() {
    const service = new MutualidadMonoliticaService();

    console.log('=== ALTA AFILIADO ===');
    const alta = service.altaAfiliado(
        '12345678A',
        'Juan',
        'Pérez',
        'Calle Mayor',
        '123',
        'Madrid',
        '28001'
    );
    console.log(alta);

    console.log('\n=== PROCESAR PRESTACIÓN ===');
    const prestacion = service.procesarPrestacion('12345678A', 'CONSULTA');
    console.log(prestacion);
}

/**
 * PROBLEMAS IDENTIFICADOS EN EL MONOLITO:
 * 
 * 1. VIOLACIÓN DE SINGLE RESPONSIBILITY PRINCIPLE
 *    - MutualidadUsuario hace demasiado
 *    - Mezcla afiliación, beneficios, notificaciones y validaciones
 * 
 * 2. ACOPLAMIENTO FUERTE
 *    - Cambios en un contexto afectan otros
 *    - Difícil modificar un contexto sin tocar otros
 * 
 * 3. DIFICULTAD DE ESCALADO
 *    - Si necesitamos más rendimiento para notificaciones,
 *      tenemos que escalar todo el monolito
 * 
 * 4. EQUIPOS DIFÍCILES DE ORGANIZAR
 *    - Un equipo modificando puede romper código de otros
 *    - Git conflicts frecuentes
 * 
 * 5. TESTING COMPLEJO
 *    - Tests unitarios difíciles por acoplamiento
 *    - Tests de integración obligatorios
 * 
 * 6. LENGUAJE UBICUO CONFUSO
 *    - Mismos términos con diferentes significados
 *    - "Usuario" puede ser afiliado, beneficiario, etc.
 * 
 * SOLUCIÓN: DDD con microservicios separados por Bounded Context
 */
