import sys
from resource_management import *

from oozie import oozie
from oozie_service import oozie_service

         
class OozieServer(Script):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)
    
  def configure(self, env):
    import params
    env.set_params(params)

    oozie(is_server=True)
    
  def start(self, env):
    import params
    env.set_params(params)
    #TODO remove this when config command will be implemented
    self.configure(env)
    oozie_service(action='start')
    
  def stop(self, env):
    import params
    env.set_params(params)
    oozie_service(action='stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.pid_file)
    
def main():
  command_type = sys.argv[1] if len(sys.argv)>1 else "start"
  print "Running "+command_type
  command_data_file = '/root/workspace/Oozie/input.json'
  basedir = '/root/workspace/Oozie/main'
  sys.argv = ["", command_type, command_data_file, basedir]
  
  OozieServer().execute()
  
if __name__ == "__main__":
  #main()
  OozieServer().execute()
