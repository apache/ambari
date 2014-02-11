import re
from resource_management import *

def escape_yaml_propetry(value):
  unquouted = False
  unquouted_values = ["null","Null","NULL","true","True","TRUE","false","False","FALSE","YES","Yes","yes","NO","No","no","ON","On","on","OFF","Off","off"]
  
  if value in unquouted_values:
    unquouted = True

  # if is list [a,b,c]
  if re.match('^\w*\[.+\]\w*$', value):
    unquouted = True
    
  try:
    int(value)
    unquouted = True
  except ValueError:
    pass
  
  try:
    float(value)
    unquouted = True
  except ValueError:
    pass
  
  if not unquouted:
    value = value.replace("'","''")
    value = "'"+value+"'"
    
  return value

def yaml_config(
  filename,
  configurations = None,
  conf_dir = None,
  mode = None,
  owner = None,
  group = None
):
    config_content = source.InlineTemplate('''{% for key, value in configurations_dict.items() %}{{ key }}: {{ escape_yaml_propetry(value) }}
{% endfor %}''', configurations_dict=configurations, extra_imports=[escape_yaml_propetry])

    File (format("{conf_dir}/{filename}"),
      content = config_content,
      owner = owner,
      group = group,
      mode = mode
    )
