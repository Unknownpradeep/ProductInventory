@echo off
echo Starting Spring Boot Backend with Low Memory limit (512MB)...
set MAVEN_OPTS=-Xmx512m -XX:+UseG1GC
.\mvnw.cmd spring-boot:run
pause
