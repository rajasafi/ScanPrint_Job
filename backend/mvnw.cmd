@echo off
setlocal
set "WRAPPER_HOME=%~dp0."
set "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" (echo Maven wrapper jar missing & exit /b 1)
java -Dmaven.multiModuleProjectDirectory="%WRAPPER_HOME%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
