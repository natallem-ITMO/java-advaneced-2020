@echo off
SET return=%CD%
SET implDir=%~dp0
SET implDir=%implDir:~0,-1%
cd %implDir%
SET root=..\..\..\..\..\..\..\
cd %root%
SET root=%CD%
cd %implDir%
SET mod_name=ru.ifmo.rain.lemeshkova.implementor
SET mod_path=ru\ifmo\rain\lemeshkova\implementor
SET impler_name=info.kgeorgiy.java.advanced.implementor
SET impler_path=info\kgeorgiy\java\advanced\implementor
SET artifacts=%root%\java-advanced-2020\artifacts
SET lib=%root%\java-advanced-2020\lib
SET modules=%root%\java-advanced-2020\modules

javadoc ^
    -private ^
    -link https://docs.oracle.com/en/java/javase/11/docs/api/ ^
    -d _javadoc ^
    -cp "%artifacts%\*.jar";"%lib%\*.jar"; ^
     *.java ^
     "%modules%\%impler_name%\%impler_path%\Impler.java" ^
     "%modules%\%impler_name%\%impler_path%\JarImpler.java" ^
     "%modules%\%impler_name%\%impler_path%\ImplerException.java" ^
     "%modules%\%impler_name%\%impler_path%\package-info.java"
cd "%return%"