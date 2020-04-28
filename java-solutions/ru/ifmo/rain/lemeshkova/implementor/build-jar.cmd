@echo off
SET wd=..\..\..\..\..\..\..\java-advanced-2020
SET mod_path=%wd%\artifacts;%wd%\lib
SET mod_dir=ru\ifmo\rain\lemeshkova\implementor
SET out=%CD%\_build
SET mod_name=ru.ifmo.rain.lemeshkova.implementor
SET src=..\..\..\..\..
SET current=%CD%
@echo on

javac --module-path %mod_path% %src%\module-info.java %src%\%mod_dir%\*.java -d %out%

cd %out%

jar -c --file=%current%\_implementor.jar --main-class=%mod_name%.JarImplementor --module-path=%mod_path% module-info.class %mod_dir%\*.class

@echo off
cd %current%
@echo on
