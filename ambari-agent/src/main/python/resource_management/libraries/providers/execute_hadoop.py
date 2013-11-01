from resource_management import *

class ExecuteHadoopProvider(Provider):
  def action_run(self):
    if self.resource.security_enabled and not self.resource.kinit_override:
      kinit_if_needed = "su - {user} -c '{kinit_path_local} -kt {keytab} {principal}'".format(
            user=self.resource.user, kinit_path_local=self.resource.kinit_path_local, 
            keytab=self.resource.keytab, principal=self.resource.principal)
    else:
      kinit_if_needed = ""

    cmd = "hadoop --config {conf_dir} {command}".format(conf_dir=self.resource.conf_dir, command=self.resource.command)
    
    if kinit_if_needed != "":
      Execute ( kinit_if_needed,
        path = ['/bin']
      )
  
    Execute ( cmd,
      user        = self.resource.user,
      tries       = self.resource.tries,
      try_sleep   = self.resource.try_sleep,
      logoutput   = self.resource.logoutput,
    )