# Caso Practico: Sistema de Gestion de Mutualidad

## Contexto

Una mutualidad de profesionales necesita modernizar su sistema de gestion de afiliados y beneficios. Actualmente opera con un sistema monolitico legacy que dificulta la escalabilidad y el mantenimiento.

## Objetivo

Disenar e implementar una plataforma basada en **microservicios** que gestione:

1. **Afiliados**: Registro, alta, baja y consulta de miembros de la mutualidad
2. **Beneficios**: Catalogo de prestaciones disponibles (sanitarias, jubilacion, fallecimiento)
3. **Solicitudes**: Tramitacion de peticiones de beneficios por parte de afiliados
4. **Notificaciones**: Comunicacion multicanal (email, SMS, push) con los afiliados
5. **Validaciones**: Verificacion de reglas de negocio y elegibilidad

## Requisitos Funcionales

- Un afiliado debe tener DNI valido (formato espanol con letra de control)
- Solo los afiliados en estado ACTIVO pueden solicitar beneficios
- Las transiciones de estado deben seguir reglas de negocio (ej: no reactivar un afiliado dado de baja)
- Cada solicitud de beneficio requiere validacion antes de su aprobacion
