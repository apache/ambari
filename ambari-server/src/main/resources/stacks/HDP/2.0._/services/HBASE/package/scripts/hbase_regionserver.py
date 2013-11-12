import sys
from resource_management import *

from hbase import hbase
from hbase_service import hbase_service

         
class HbaseRegionServer(Script):
  def install(self, env):
    Package('hbase')
    self.configure(env)
    
  def configure(self, env):
    import params
    env.set_params(params)

    hbase(type='regionserver')
      
  def start(self, env):
    import params
    env.set_params(params)

    hbase_service( 'regionserver',
      action = 'start'
    )
    
  def stop(self, env):
    import params
    env.set_params(params)

    hbase_service( 'regionserver',
      action = 'stop'
    )
    
  def decommission(self):
    print "Decommission not yet implemented!"
    
def main():
  command_type = sys.argv[1] if len(sys.argv)>1 else "stop"
  print "Running "+command_type
  command_data_file = '/root/workspace/HBase/input.json'
  basedir = '/root/workspace/HBase/main'
  sys.argv = ["", command_type, command_data_file, basedir]
  
  HbaseRegionServer().execute()
  
if __name__ == "__main__":
  HbaseRegionServer().execute()
