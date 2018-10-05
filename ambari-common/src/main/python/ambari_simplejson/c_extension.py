
"""
The C extension loader for various platforms.

NOTE: this module should be on top level

Example of usage:

def _import_c_scanstring():
    from . import c_extension
    _speedups = c_extension.get()

    if _speedups:
        try:
            return _speedups.scanstring     #  import here required functions
        except AttributeError:
            pass

    return None
"""


def get():
  _path = ".".join(__name__.split(".")[:-1])
  # trying to load extension from available platforms
  _import_paths = ["_speedups.posix.usc2", "_speedups.posix.usc4", "_speedups.ppc", "_speedups.win"]

  for _import_path in _import_paths:
    try:
      return __import__("{}.{}".format(_path, _import_path), fromlist=["_speedups"])._speedups
    except (ImportError, AttributeError):
      pass

  return None


def is_loaded():
  """
  :rtype bool
  """
  return get() is not None
