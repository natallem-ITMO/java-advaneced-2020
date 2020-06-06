@echo off
set cur=%cd%
cd B:\Projects\IdeaProjects\ITMO\4_semester\JavaAdvanced\java-advanced-2020-solutions\compiled

@java  ru.ifmo.rain.lemeshkova.bank.client.Client %*

cd %cur%
@echo on