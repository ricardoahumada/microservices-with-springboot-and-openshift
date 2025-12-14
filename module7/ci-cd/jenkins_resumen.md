# Jenkins - Resumen Educativo

Jenkins es una plataforma de integración y entrega continua (CI/CD) de código abierto que automatiza el proceso de construcción, prueba y despliegue de aplicaciones. Desarrollado originalmente como Hudson, Jenkins se ha convertido en una de las herramientas más populares y flexibles para la automatización de pipelines de desarrollo.

## **¿Qué es Jenkins?**

Jenkins es un servidor de automatización que permite automatizar las partes repetitivas del desarrollo de software relacionadas con la construcción, prueba y despliegue de aplicaciones. Proporciona una interfaz web para configurar trabajos (jobs), pipelines y gestionar el ciclo de vida completo de las aplicaciones.

### Características Principales

- **Automatización de builds**: Compilación automática de código
- **Integración continua**: Verificación continua del código
- **Pipelines declarativos**: Definición de workflows complejos
- **Extensibilidad**: Más de 1,800 plugins disponibles
- **Distribución de builds**: Ejecución en múltiples agentes
- **Interfaz web**: Gestión visual de configuraciones

## **Conceptos Clave**

### Jobs y Proyectos
Los **jobs** son las unidades básicas de trabajo en Jenkins. Representan tareas específicas como:
- Compilación de código
- Ejecución de pruebas
- Despliegue de aplicaciones
- Ejecución de scripts personalizados

### Pipelines
Los **pipelines** son workflows que definen una serie de pasos para construir, probar y desplegar código. Pueden ser:
- **Declarativos**: Sintaxis más simple y estructurada
- **Scripted**: Mayor flexibilidad con Groovy

### Agentes (Slaves)
Los **agentes** son nodos que ejecutan los jobs. Pueden ser:
- **Agentes permanentes**: Siempre disponibles
- **Agentes bajo demanda**: Se crean dinámicamente
- **Contenedores Docker**: Ejecución en contenedores

### Plugins
Los **plugins** extienden la funcionalidad de Jenkins:
- **SCM plugins**: Integración con Git, SVN, etc.
- **Build plugins**: Herramientas de compilación
- **Deployment plugins**: Plataformas de despliegue
- **Notification plugins**: Alertas y notificaciones

## **Casos de Uso**

### 1. Integración Continua (CI)
- **Compilación automática**: Detección de cambios en repositorios
- **Ejecución de pruebas**: Unit tests, integración y regresión
- **Análisis de código**: Linting, cobertura y calidad
- **Notificaciones**: Alertas de estado a equipos

### 2. Entrega Continua (CD)
- **Pipelines de despliegue**: Automatización de deployments
- **Gestión de entornos**: Dev, staging, producción
- **Rollback automático**: Reversión en caso de fallos
- **Validación de builds**: Verificación antes del despliegue

### 3. Automatización de Infraestructura
- **Infrastructure as Code**: Despliegue de infraestructura
- **Configuración de servidores**: Automatización de setup
- **Gestión de bases de datos**: Migraciones y backups
- **Monitoreo y alertas**: Integración con sistemas de monitoreo

### 4. Procesos de Release
- **Versionado automático**: Incremento de versiones
- **Generación de artifacts**: Paquetes y contenedores
- **Release notes**: Documentación automática
- **Publicación de releases**: Repositorios y registries

## **Configuración**

### Instalación Básica

#### Instalación con Docker
```bash
# Ejecutar Jenkins en contenedor
docker run -d -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  --name jenkins jenkins/jenkins:lts

# Obtener contraseña inicial
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

#### Instalación con WAR
```bash
# Descargar Jenkins WAR
wget http://mirrors.jenkins.io/war-stable/latest/jenkins.war

# Ejecutar Jenkins
java -jar jenkins.war --httpPort=8080
```

### Configuración Inicial

#### Jenkinsfile Básico
```groovy
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', 
                     url: 'https://github.com/empresa/app.git'
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                sh './deploy.sh'
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            slackSend channel: '#devops',
                      color: 'good',
                      message: "Build ${BUILD_NUMBER} succeeded!"
        }
        failure {
            slackSend channel: '#devops',
                      color: 'danger',
                      message: "Build ${BUILD_NUMBER} failed!"
        }
    }
}
```

### Configuración de Plugins

#### Configuración de Git
```groovy
pipeline {
    agent any
    
    tools {
        git 'Default'
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    // Configuración de credenciales
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'git-credentials',
                            usernameVariable: 'GIT_USERNAME',
                            passwordVariable: 'GIT_PASSWORD'
                        )
                    ]) {
                        sh 'git clone https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/empresa/app.git'
                    }
                }
            }
        }
    }
}
```

## **Ejemplos con Java**

### Pipeline para Aplicación Spring Boot
```groovy
pipeline {
    agent {
        docker {
            image 'maven:3.8.6-openjdk-11'
            args '-v $HOME/.m2:/root/.m2'
        }
    }
    
    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=$HOME/.m2/repository'
        JAVA_HOME = '/usr/lib/jvm/java-11-openjdk-amd64'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Maven Dependencies') {
            steps {
                sh 'mvn dependency:resolve'
            }
        }
        
        stage('Compile') {
            steps {
                sh 'mvn clean compile -DskipTests'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    publishTestResults testResultsPattern: '**/surefire-reports/*.xml'
                    publishCoverage adapters: [jacocoAdapter('**/target/site/jacoco/jacoco.xml')]
                }
            }
        }
        
        stage('Integration Tests') {
            when {
                branch 'develop'
            }
            steps {
                sh 'mvn verify -Dit.test=*IntegrationTest'
            }
        }
        
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', 
                                   fingerprint: true
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Report'
                    ])
                }
            }
        }
        
        stage('Docker Build') {
            when {
                branch 'main'
            }
            steps {
                script {
                    def image = docker.build("empresa/app:${BUILD_NUMBER}")
                    docker.withRegistry('https://registry.empresa.com', 'docker-registry') {
                        image.push()
                        image.push('latest')
                    }
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'main'
            }
            steps {
                sshagent(['ssh-key']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no user@staging-server \
                            "cd /opt/app && git pull && mvn spring-boot:run"
                    '''
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            slackSend channel: '#devops',
                      color: 'good',
                      message: "✅ Build ${BUILD_NUMBER} for ${BRANCH_NAME} completed successfully"
        }
        failure {
            slackSend channel: '#devops',
                      color: 'danger',
                      message: "❌ Build ${BUILD_NUMBER} for ${BRANCH_NAME} failed"
        }
        unstable {
            slackSend channel: '#devops',
                      color: 'warning',
                      message: "⚠️ Build ${BUILD_NUMBER} for ${BRANCH_NAME} was unstable"
        }
    }
}
```

### Configuración de Agents Docker
```groovy
pipeline {
    agent {
        docker {
            image 'maven:3.8.6-openjdk-11'
            args '''
                -v $HOME/.m2:/root/.m2
                -v /var/run/docker.sock:/var/run/docker.sock
                -v $(which docker):/usr/bin/docker
            '''
        }
    }
    
    stages {
        stage('Build') {
            steps {
                sh '''
                    mvn clean package -DskipTests
                    docker build -t app:${BUILD_NUMBER} .
                '''
            }
        }
        
        stage('Test Container') {
            steps {
                sh '''
                    docker run -d -p 8080:8080 --name app-test app:${BUILD_NUMBER}
                    sleep 30
                    curl -f http://localhost:8080/health || exit 1
                    docker stop app-test
                    docker rm app-test
                '''
            }
        }
    }
}
```

### Pipeline con Testing en Paralelo
```groovy
pipeline {
    agent any
    
    stages {
        stage('Parallel Testing') {
            parallel {
                stage('Unit Tests') {
                    agent {
                        docker { image 'maven:3.8.6-openjdk-11' }
                    }
                    steps {
                        sh 'mvn test'
                    }
                    post {
                        always {
                            junit 'target/surefire-reports/*.xml'
                        }
                    }
                }
                
                stage('Integration Tests') {
                    agent {
                        docker { image 'maven:3.8.6-openjdk-11' }
                    }
                    steps {
                        sh 'mvn integration-test'
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: '**/failsafe-reports/*.xml'
                        }
                    }
                }
                
                stage('Code Quality') {
                    agent {
                        docker { image 'sonarqube:community' }
                    }
                    steps {
                        sh '''
                            mvn sonar:sonar \
                                -Dsonar.projectKey=app \
                                -Dsonar.host.url=http://sonarqube:9000 \
                                -Dsonar.login=${SONAR_TOKEN}
                        '''
                    }
                }
                
                stage('Security Scan') {
                    agent {
                        docker { image 'owasp/dependency-check:latest' }
                    }
                    steps {
                        sh '''
                            dependency-check.sh --project app \
                                --scan target \
                                --format XML \
                                --out target/
                        '''
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: 'target/*dependency-check-report.xml'
                        }
                    }
                }
            }
        }
    }
}
```

## **Ventajas y Desventajas**

### Ventajas
- **Gratuito y open source**: Sin costos de licenciamiento
- **Extensible**: Ecosistema masivo de plugins
- **Comunidad activa**: Gran cantidad de recursos y documentación
- **Flexible**: Soporte para múltiples tecnologías y lenguajes
- **Escalable**: Distribución de builds en múltiples nodos
- **Integración**: Compatible con la mayoría de herramientas DevOps
- **Madurez**: Proyecto estable con años de desarrollo

### Desventajas
- **Configuración compleja**: Curva de aprendizaje pronunciada
- **Mantenimiento**: Requiere administración constante
- **Rendimiento**: Puede volverse lento con muchos jobs
- **Seguridad**: Configuración de seguridad puede ser compleja
- **UI obsoleta**: Interfaz de usuario no moderna
- **Actualizaciones**: Gestión de actualizaciones de plugins
- **Escalabilidad**: Requiere configuración manual para alta disponibilidad

## **Buenas Prácticas**

### Seguridad
- **Autenticación**: Usar LDAP, Active Directory o SSO
- **Autorización**: Implementar control de acceso basado en roles
- **Secrets Management**: Usar herramientas como HashiCorp Vault
- **HTTPS**: Configurar certificados SSL/TLS
- **Backup**: Respaldos regulares de configuraciones

```groovy
pipeline {
    agent any
    
    environment {
        // Usar credentials management
        SONAR_TOKEN = credentials('sonar-token')
        DOCKER_REGISTRY = credentials('docker-registry')
    }
    
    stages {
        stage('Secure Deploy') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'ssh-key',
                        keyFileVariable: 'SSH_KEY'
                    )
                ]) {
                    sh 'deploy-script.sh'
                }
            }
        }
    }
}
```

### Performance
- **Agents específicos**: Usar images apropiadas para cada tecnología
- **Paralelización**: Ejecutar tests en paralelo cuando sea posible
- **Caching**: Cache de dependencias y herramientas
- **Cleanup**: Limpiar workspace después de builds

```groovy
pipeline {
    agent {
        docker {
            image 'maven:3.8.6-openjdk-11'
            args '''
                -v $HOME/.m2:/root/.m2
                -v $HOME/.cache:/root/.cache
            '''
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}
```

### Mantenimiento
- **Pipeline as Code**: Versionar Jenkinsfiles
- **Modularización**: Reusar shared libraries
- **Documentación**: Documentar configuraciones complejas
- **Monitoring**: Monitorear rendimiento y recursos

```groovy
// shared-library/vars/gradleBuild.groovy
def call(String task = 'build') {
    docker.image('gradle:7.4').inside {
        sh "gradle ${task}"
    }
}

// Jenkinsfile
@Library('shared-library') _

pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                gradleBuild 'build'
            }
        }
    }
}
```

### Testing
- **Test Stages**: Separar unit, integration y e2e tests
- **Quality Gates**: Implementar gates de calidad
- **Test Reporting**: Reportes detallados de testing

```groovy
pipeline {
    agent any
    
    stages {
        stage('Quality Gates') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh 'mvn test'
                    }
                    post {
                        always {
                            junit 'target/surefire-reports/*.xml'
                        }
                    }
                }
                
                stage('Coverage') {
                    steps {
                        sh 'mvn jacoco:report'
                    }
                    post {
                        always {
                            publishCoverage adapters: [
                                jacocoAdapter('target/site/jacoco/jacoco.xml')
                            ]
                        }
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    def coverage = currentBuild.buildVariables['JACOCO_COVERAGE']
                    if (coverage.toFloat() < 80.0) {
                        currentBuild.result = 'UNSTABLE'
                        error "Coverage ${coverage}% is below 80%"
                    }
                }
            }
        }
    }
}
```

---

## **Referencias Oficiales**

1. **Jenkins Official Documentation**: https://www.jenkins.io/doc/
2. **Jenkins Pipeline Documentation**: https://www.jenkins.io/doc/book/pipeline/
3. **Jenkins Plugins Index**: https://plugins.jenkins.io/
4. **Jenkins GitHub Repository**: https://github.com/jenkinsci/jenkins
5. **Jenkins User Guide**: https://www.jenkins.io/doc/tutorials/