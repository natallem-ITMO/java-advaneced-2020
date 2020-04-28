
SET task=concurrent
SET class=IterativeParallelism
SET modification=scalar
SET testTask=mapper

CALL _test_double_args %task% %class% %modification% %testTask%
