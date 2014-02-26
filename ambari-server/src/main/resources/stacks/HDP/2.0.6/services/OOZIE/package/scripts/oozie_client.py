import sys
from resource_management import *

from oozie import oozie
from oozie_service import oozie_service

         
class OozieClient(Script):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)
    
  def configure(self, env):
    import params
    env.set_params(params)

    oozie(is_server=False)

  def status(self, env):
    raise ClientComponentHasNoStatus()
    
if __name__ == "__main__":
  OozieClient().execute()
