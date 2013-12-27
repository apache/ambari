from resource_management import *

def oozie(is_server=False # TODO: see if see can remove this
              ):
  import params

  XmlConfig( "oozie-site.xml",
    conf_dir = params.conf_dir, 
    configurations = params.config['configurations']['oozie-site'],
    owner = params.oozie_user,
    group = params.user_group,
    mode = 0664
  )
  
  Directory( params.conf_dir,
    owner = params.oozie_user,
    group = params.user_group
  )
  
  TemplateConfig( format("{conf_dir}/oozie-env.sh"),
    owner = params.oozie_user
  )
  
  TemplateConfig( format("{conf_dir}/oozie-log4j.properties"),
    owner = params.oozie_user
  )

  if params.jdbc_driver_name == "com.mysql.jdbc.Driver" or params.jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
    Execute(format("/bin/sh -c 'cd /usr/lib/ambari-agent/ &&\
    curl -kf --retry 5 {jdk_location}{check_db_connection_jar_name}\
     -o {check_db_connection_jar_name}'"),
      not_if  = format("[ -f {check_db_connection_jar} ]")
    )
    
  oozie_ownership( )
  
  if is_server:      
    oozie_server_specific( )
  
def oozie_ownership(
):
  import params
  
  File ( format("{conf_dir}/adminusers.txt"),
    owner = params.oozie_user,
    group = params.user_group
  )

  File ( format("{conf_dir}/hadoop-config.xml"),
    owner = params.oozie_user,
    group = params.user_group
  )

  File ( format("{conf_dir}/oozie-default.xml"),
    owner = params.oozie_user,
    group = params.user_group
  )

  Directory ( format("{conf_dir}/action-conf"),
    owner = params.oozie_user,
    group = params.user_group
  )

  File ( format("{conf_dir}/action-conf/hive.xml"),
    owner = params.oozie_user,
    group = params.user_group
  )
  
def oozie_server_specific(
):
  import params
  
  oozie_server_directorties = [params.oozie_pid_dir, params.oozie_log_dir, params.oozie_tmp_dir, params.oozie_data_dir, params.oozie_lib_dir, params.oozie_webapps_dir]            
  Directory( oozie_server_directorties,
    owner = params.oozie_user,
    mode = 0755,
    recursive = True
  )
       
  cmd1 = "cd /usr/lib/oozie && tar -xvf oozie-sharelib.tar.gz"
  cmd2 =  format("cd /usr/lib/oozie && mkdir -p {oozie_tmp_dir}")
  
  # this is different for HDP1
  cmd3 = format("cd /usr/lib/oozie && chown {oozie_user}:{user_group} {oozie_tmp_dir} && mkdir -p {oozie_libext_dir} && cp {ext_js_path} {oozie_libext_dir}")
  if params.jdbc_driver_name=="com.mysql.jdbc.Driver" or params.jdbc_driver_name=="oracle.jdbc.driver.OracleDriver":
    cmd3 += format(" && cp {jdbc_driver_jar} {oozie_libext_dir}")
    
  # this is different for HDP1
  cmd4 = format("cd {oozie_tmp_dir} && /usr/lib/oozie/bin/oozie-setup.sh prepare-war")
  
  no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")
  Execute( [cmd1, cmd2, cmd3],
    not_if  = no_op_test
  )
  Execute( cmd4,
    user = params.oozie_user,
    not_if  = no_op_test
  )
  
