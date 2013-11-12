import sys
from resource_management import *

from hbase import hbase

         
class HbaseClient(Script):
  def install(self, env):
    Package('hbase')
    self.configure(env)
    
  def configure(self, env):
    import params
    env.set_params(params)
    
    hbase(type='client')
    
#for tests
def main():
  command_type = 'install'
  command_data_file = '/root/workspace/HBase/input.json'
  basedir = '/root/workspace/HBase/main'
  sys.argv = ["", command_type, command_data_file, basedir]
  
  HbaseClient().execute()
  
if __name__ == "__main__":
  HbaseClient().execute()
