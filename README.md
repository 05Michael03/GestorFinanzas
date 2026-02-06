**GestorFinanzas**

Descripción
- Proyecto Java simple para gestionar finanzas personales: usuarios, categorías, tipos y movimientos.

Características
- Gestión de usuarios y autenticación (interfaz de login).
- Registrar y listar movimientos (ingresos/gastos) por categoría y tipo.
- Persistencia con base de datos local (script SQL incluido).

Requisitos
- Java 11+ (JDK instalado)
- Maven

Contenido del repositorio
- `finanzas.sql` — script SQL inicial para crear las tablas y datos de prueba.
- Código fuente en `src/main/java`.
- Clase principal: `com.mycompany.gestorfinanzas.GestorFinanzas`.

Instalación y ejecución

1) Compilar con Maven:

```bash
mvn clean package
```

2) Ejecutar desde Maven (usa el plugin exec explícito):

```bash
mvn org.codehaus.mojo:exec-maven-plugin:3.0.0:java -Dexec.mainClass="com.mycompany.gestorfinanzas.GestorFinanzas"
```

3) O ejecutar desde IDE (IntelliJ/Eclipse): importar el proyecto Maven y correr la clase principal `com.mycompany.gestorfinanzas.GestorFinanzas`.

Base de datos
- El script `finanzas.sql` en la raíz crea las tablas necesarias. Revísalo y ejecútalo en tu gestor SQL local si usas una base externa.
- `DatabaseHelper.java` contiene la lógica de conexión y persistencia (revisa `src/main/java/gestorfinanzas/DatabaseHelper.java`).

Estructura clave de código
- [src/main/java/com/mycompany/gestorfinanzas/GestorFinanzas.java](src/main/java/com/mycompany/gestorfinanzas/GestorFinanzas.java)
- [src/main/java/gestorfinanzas/DatabaseHelper.java](src/main/java/gestorfinanzas/DatabaseHelper.java)
- [src/main/java/gestorfinanzas/LoginFrame.java](src/main/java/gestorfinanzas/LoginFrame.java)
- [src/main/java/gestorfinanzas/MainFrame.java](src/main/java/gestorfinanzas/MainFrame.java)
- Modelos: `Usuario`, `Categoria`, `Movimiento`, `Tipo` en `src/main/java/gestorfinanzas`.

Uso rápido
- Arranca la aplicación y usa la ventana de login para acceder.
- Desde la interfaz principal podrás agregar movimientos, filtrar por categoría/tipo y revisar balances.

Cómo contribuir
- Fork y crea un branch por característica: `feature/mi-cambio`.
- Envía un Pull Request con descripción y pruebas mínimas.

Soporte y próximos pasos sugeridos
- Añadir empaquetado en `pom.xml` para generar un JAR ejecutable.
- Añadir instrucciones para configurar la base de datos (si no es embebida).

Licencia
- Añade aquí la licencia que prefieras (MIT, Apache 2.0, etc.).
