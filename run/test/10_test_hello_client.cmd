@echo off

SET task=hello
SET class=HelloUDPClient
SET modification=client
SET modification=client-i18n
SET modification=client-evil

CALL _test %task% %class% %modification%