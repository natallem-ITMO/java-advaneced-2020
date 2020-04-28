@echo on

SET task=%1
SET class=%2
SET modification=%3
SET testTask=%4

SET package_name=ru.ifmo.rain.lemeshkova.%task%
SET package_dir=ru\ifmo\rain\lemeshkova\%task%

SET idea_project=B:\Projects\IdeaProjects\ITMO\4_semester\JavaAdvanced
SET java_advanced=%idea_project%\java-advanced-2020
SET out=%idea_project%\out\production\java-solutions\%package_name%
SET mod_path=%java_advanced%\artifacts;%java_advanced%\lib;%out%
SET solutions=%idea_project%\java-advanced-2020-solutions\java-solutions

echo Compiling...
javac --module-path %mod_path% %solutions%\module-info.java %solutions%\%package_dir%\*.java -d %out%

set \P salt="Enter salt: "
@echo on
java --module-path %mod_path% --add-modules java.solutions -m info.kgeorgiy.java.advanced.%testTask%^
 %modification% %package_name%.%class% %salt%
@echo off