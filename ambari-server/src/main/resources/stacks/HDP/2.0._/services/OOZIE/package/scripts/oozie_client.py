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
    
def main():
  command_type = sys.argv[1] if len(sys.argv)>1 else "install"
  print "Running "+command_type
  command_data_file = '/root/workspace/Oozie/input.json'
  basedir = '/root/workspace/Oozie/main'
  sys.argv = ["", command_type, command_data_file, basedir]
  
  OozieClient().execute()
  
if __name__ == "__main__":
  #main()
  OozieClient().execute()
