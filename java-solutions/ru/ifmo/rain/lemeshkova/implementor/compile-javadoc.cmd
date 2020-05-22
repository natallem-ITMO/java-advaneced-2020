@echo off
SET root=..\..\..\..\..\..\..\
SET impler_name=info.kgeorgiy.java.advanced.implementor
SET impler_relative_path=info\kgeorgiy\java\advanced\implementor
SET java-advanced-2020=%root%\java-advanced-2020
SET lib=%java-advanced-2020%\lib
SET artifacts=%java-advanced-2020%\artifacts
SET modules=%java-advanced-2020%\modules
SET impler_path=%modules%\%impler_name%\%impler_relative_path%

javadoc ^
    -private ^
    -link https://docs.oracle.com/en/java/javase/11/docs/api/ ^
    -d _javadoc ^
    -cp "%artifacts%\*.jar";"%lib%\*.jar"; ^
     *.java ^
     "%impler_path%\Impler.java" ^
     "%impler_path%\JarImpler.java" ^
     "%impler_path%\ImplerException.java" ^
     "%impler_path%\package-info.java"
