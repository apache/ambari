__all__ = ["default"]

from resource_management.libraries.script import Script
default_subdict='/configurations/global'

def default(name, default_value=None):
  subdicts = filter(None, name.split('/'))
  
  if not name.startswith('/'):
    subdicts = filter(None, default_subdict.split('/')) + subdicts

  curr_dict = Script.get_config()
  for x in subdicts:
    if x in curr_dict:
      curr_dict = curr_dict[x]
    else:
      return default_value

  return curr_dict