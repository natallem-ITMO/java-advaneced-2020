@echo off

SET scripts=%cd%
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

set classpath=%out%

java ru.ifmo.rain.lemeshkova.bank.server.Server

:::::::::::::::::::::::::::::::::::::::::::::::::::::

@echo on
