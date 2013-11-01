import os
from resource_management import *

class ConfigFileProvider(Provider):
  def action_create(self):
    template_tag = self.resource.template_tag
    qualified_file_name = self.resource.name
    file_name = os.path.basename(qualified_file_name)

    if not template_tag:
      template_name = format("{file_name}.j2")
    else:
      template_name = format("{file_name}-{template_tag}.j2")

    File( qualified_file_name,
     owner   = self.resource.owner,
     group   = self.resource.group,
     mode    = self.resource.mode,
     content = Template(template_name)
    )