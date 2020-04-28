@echo off
SET wd=..\..\..\..\..\..\..\java-advanced-2020

SET run=%cd%
SET mod_path=%run%;%wd%\artifacts;%wd%\lib

@echo on
java --module-path=%mod_path% -m java.solutions %1 %2 %3

