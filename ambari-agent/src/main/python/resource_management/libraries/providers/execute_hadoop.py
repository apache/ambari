from resource_management import *

class ExecuteHadoopProvider(Provider):
  def action_run(self):
    kinit_path_local = self.resource.kinit_path_local
    keytab = self.resource.keytab
    principal = self.resource.principal
    conf_dir = self.resource.conf_dir
    command = self.resource.command
    
    if self.resource.security_enabled and not self.resource.kinit_override:
      Execute ((kinit_path_local, '-kt', keytab, principal),
        path = ['/bin'],
        user = self.resource.user
      )

    Execute (('hadoop', '--config', conf_dir, command),
      user        = self.resource.user,
      tries       = self.resource.tries,
      try_sleep   = self.resource.try_sleep,
      logoutput   = self.resource.logoutput,
    )