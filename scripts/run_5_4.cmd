@echo off

SET start=%cd%
echo start %start%

cd ../..
SET root=%cd%
echo root %root%

SET java_advanced_2020=%cd%\java-advanced-2020
echo java_advanced_2020 %java_advanced_2020%

SET lib=%java_advanced_2020%\lib
echo lib %lib%

SET java_advanced_2020_solutions=%root%\java-advanced-2020-solutions
echo java_advanced_2020_solutions %java_advanced_2020_solutions%

SET java_solutions=%java_advanced_2020_solutions%\java-solutions
echo java_solutions %java_solutions%

SET bank=%java_solutions%\ru\ifmo\rain\lemeshkova\bank
echo bank %bank%

SET out=%java_advanced_2020_solutions%/compiled
cd %bank%
echo cur %cd%
javac -cp .;%lib%/hamcrest-core-1.3.jar;%lib%/junit-4.11.jar client/*.java common/*.java server/*.java tests/*.java -d %out%

set junit_standalone_jar=%java_advanced_2020_solutions%\lib\junit-platform-console-standalone-1.6.1.jar
echo junit_standalone_jar %junit_standalone_jar%

java -jar %junit_standalone_jar% --class-path %lib%;%out%;%bank% -c ru.ifmo.rain.lemeshkova.bank.tests.BankTests

echo Tests finished with exit code %errorlevel%

@echo on