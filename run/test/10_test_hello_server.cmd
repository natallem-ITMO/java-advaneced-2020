@echo off

SET task=hello
SET class=HelloUDPServer
SET modification=server
SET modification=server-i18n
SET modification=server-evil

CALL _test %task% %class% %modification%