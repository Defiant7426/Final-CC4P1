# Usar una imagen base de OpenJDK
FROM openjdk:17-jdk-slim

# Establecer un directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar los archivos necesarios para construir el proyecto
COPY . /app

# Construir el proyecto con Gradle Wrapper
RUN ./gradlew build

# Exponer el puerto que usa la aplicación
EXPOSE 8082

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "build/libs/AppAlmacen-Java-1.0-SNAPSHOT.jar", "8082", "http://127.0.0.1:8083,http://127.0.0.1:8084"]
