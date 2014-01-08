from resource_management import *

def yaml_config(
  filename,
  configurations = None,
  conf_dir = None,
  mode = None,
  owner = None,
  group = None
):
    config_content = InlineTemplate('''{% for key, value in configurations_dict.items() %}{{ key }}: {{ value }}
{% endfor %}''', configurations_dict=configurations)

    File (format("{conf_dir}/{filename}"),
      content = config_content,
      owner = owner,
      group = group,
      mode = mode
    )