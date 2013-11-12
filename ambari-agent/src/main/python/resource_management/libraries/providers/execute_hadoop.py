import pipes
from resource_management import *

class ExecuteHadoopProvider(Provider):
  def action_run(self):
    kinit__path_local = self.resource.kinit_path_local
    keytab = self.resource.keytab
    conf_dir = self.resource.conf_dir
    command = self.resource.command
    
    if self.resource.principal:
      principal = self.resource.user
    else:
      principal = self.resource.principal
    
    if isinstance(command, (list, tuple)):
      command = ' '.join(pipes.quote(x) for x in command)
    
    with Environment.get_instance_copy() as env:
      if self.resource.security_enabled and not self.resource.kinit_override:
        Execute (format("{kinit__path_local} -kt {keytab} {principal}"),
          path = ['/bin'],
          user = self.resource.user
        )
    
      Execute (format("hadoop --config {conf_dir} {command}"),
        user        = self.resource.user,
        tries       = self.resource.tries,
        try_sleep   = self.resource.try_sleep,
        logoutput   = self.resource.logoutput,
      )
    env.run()