Since json on Python 2.6 is extra slow (however 2.7 is fine). Ambari struggles much using it.
This is ~40-100 times faster implementation of json module -- simplejson, with the same function set.
For speedup it uses C library _speedups.so, without this library it works the same as Python 2.6 json module. 