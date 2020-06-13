@echo off
pushd .
SET scripts=%~dp0
cd %scripts%
pushd ..\..\..\..\..\..\
SET cur_repo=%cd%
cd ..
SET root=%cd%
popd
SET java_advanced_2020=%root%\java-advanced-2020
SET java_solutions=%cur_repo%\java-solutions
SET lib=%cur_repo%\lib
SET bank=%java_solutions%\ru\ifmo\rain\lemeshkova\bank
SET out=%cur_repo%\compiled
SET mod_path=%java_advanced_2020%\lib;%java_advanced_2020%\artifacts;%lib%

:::::::::::::::::::::::::::::::::::::::::::::::::::::

SET classes_to_compile=to_compile.txt
pushd %java_solutions%
dir *.java /s /b > %scripts%\%classes_to_compile%
popd
RD /S /Q %out% 2> nul
echo Compiling...
javac -encoding UTF8 --module-path %mod_path% %java_solutions%\module-info.java @%classes_to_compile% -d %out%
del %classes_to_compile%

:::::::::::::::::::::::::::::::::::::::::::::::::::::
@echo on