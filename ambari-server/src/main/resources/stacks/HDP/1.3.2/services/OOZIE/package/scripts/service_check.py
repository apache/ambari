from resource_management import *

class OozieServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)
    
    # on HDP2 this file is different
    smoke_test_file_name = 'oozieSmoke.sh'

    oozie_smoke_shell_file( smoke_test_file_name)
  
def oozie_smoke_shell_file(
  file_name
):
  import params

  File( format("/tmp/{file_name}"),
    content = StaticFile(file_name),
    mode = 0755
  )
  
  if params.security_enabled:
    sh_cmd = format("sh /tmp/{file_name} {conf_dir} {hadoop_conf_dir} {smokeuser} {security_enabled} {smokeuser_keytab} {kinit_path_local}")
  else:
    sh_cmd = format("sh /tmp/{file_name} {conf_dir} {hadoop_conf_dir} {smokeuser} {security_enabled}")

  Execute( format("/tmp/{file_name}"),
    command   = sh_cmd,
    tries     = 3,
    try_sleep = 5,
    logoutput = True
  )
    
def main():
  import sys
  command_type = 'service_check'
  command_data_file = '/root/workspace/Oozie/input.json'
  basedir = '/root/workspace/Oozie/main'
  sys.argv = ["", command_type, command_data_file, basedir]
  
  OozieServiceCheck().execute()
  
if __name__ == "__main__":
  OozieServiceCheck().execute()
  #main()
  
