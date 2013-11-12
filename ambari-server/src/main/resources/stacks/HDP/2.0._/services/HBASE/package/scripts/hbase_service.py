from resource_management import *

def hbase_service(
  name,
  action = 'start'): # 'start' or 'stop'
    
    import params
  
    role = name
    cmd = format("{daemon_script} --config {conf_dir}")
    pid_file = format("{pid_dir}/hbase-hbase-{role}.pid")
    
    daemon_cmd = None
    no_op_test = None
    
    if action == 'start':
      daemon_cmd = format("{cmd} start {role}")
      no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")
    elif action == 'stop':
      daemon_cmd = format("{cmd} stop {role} && rm -f {pid_file}")
  
    if daemon_cmd != None: 
      Execute ( daemon_cmd,
        not_if = no_op_test,
        user = params.hbase_user
      )