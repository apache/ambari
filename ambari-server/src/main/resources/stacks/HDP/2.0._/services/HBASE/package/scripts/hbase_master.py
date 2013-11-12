import sys
from resource_management import *

from hbase import hbase
from hbase_service import hbase_service

         
class HbaseMaster(Script):
  def install(self, env):
    Package('hbase')
    self.configure(env)
    
  def configure(self, env):
    import params
    env.set_params(params)

    hbase(type='master')
    
  def start(self, env):
    import params
    env.set_params(params)

    hbase_service( 'master',
      action = 'start'
    )
    
  def stop(self, env):
    import params
    env.set_params(params)

    hbase_service( 'master',
      action = 'stop'
    )
    
def main():
  command_type = sys.argv[1] if len(sys.argv)>1 else "start"
  print "Running "+command_type
  command_data_file = '/root/workspace/HBase/input.json'
  basedir = '/root/workspace/HBase/main'
  sys.argv = ["", command_type, command_data_file, basedir]
  
  HbaseMaster().execute()
  
if __name__ == "__main__":
  HbaseMaster().execute()
