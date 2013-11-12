import time
from resource_management import *

class XmlConfigProvider(Provider):
  def action_create(self):
    filename = self.resource.filename
    conf_dir = self.resource.conf_dir
    
    # |e - for html-like escaping of <,>,',"
    config_content = InlineTemplate('''<!--{{time.asctime(time.localtime())}}-->
    <configuration>
    {% for key, value in configurations_dict.items() %}
    <property>
      <name>{{ key|e }}</name>
      <value>{{ value|e }}</value>
    </property>
    {% endfor %}
  </configuration>''', extra_imports=[time], configurations_dict=self.resource.configurations)
   
  
    self.log.debug(format("Generating config: {conf_dir}/{filename}"))
    
    with Environment.get_instance_copy() as env:
      File (format("{conf_dir}/{filename}"),
        content = config_content,
        owner = self.resource.owner,
        group = self.resource.group,
        mode = self.resource.mode
      )
    env.run()