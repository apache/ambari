import glob
import sys

from resource_management.core.exceptions import ComponentIsNotRunning

reload(sys)
sys.setdefaultencoding('utf8')
config = Script.get_config()

zeppelin_pid_dir = config['configurations']['zeppelin-env']['zeppelin_pid_dir']

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'


def execute(configurations={}, parameters={}, host_name=None):
  try:
    pid_file = glob.glob(zeppelin_pid_dir + '/zeppelin-*.pid')[0]
    check_process_status(pid_file)
  except ComponentIsNotRunning as ex:
    return (RESULT_CODE_CRITICAL, [str(ex)])
  except:
    return (RESULT_CODE_CRITICAL, ["Zeppelin is not running"])

  return (RESULT_CODE_OK, ["Successful connection to Zeppelin"])
