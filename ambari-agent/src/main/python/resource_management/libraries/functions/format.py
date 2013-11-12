__all__ = ["format"]
import sys
from string import Formatter
from resource_management.core.exceptions import Fail
from resource_management.core.utils import checked_unite
from resource_management.core.environment import Environment


class ConfigurationFormatter(Formatter):
  def format(self, format_string, *args, **kwargs):
    env = Environment.get_instance()
    variables = kwargs
    params = env.config.params
    
    result = checked_unite(variables, params)
    return self.vformat(format_string, args, result)
      
  
def format(format_string, *args, **kwargs):
  variables = sys._getframe(1).f_locals
  
  result = checked_unite(kwargs, variables)
  result.pop("self", None) # self kwarg would result in an error
  return ConfigurationFormatter().format(format_string, args, **result)