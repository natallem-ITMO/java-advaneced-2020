@echo off
SET task=%1
SET class=%2
SET modification=%3
CALL _test_double_args %task% %class% %modification% %task%
@echo on