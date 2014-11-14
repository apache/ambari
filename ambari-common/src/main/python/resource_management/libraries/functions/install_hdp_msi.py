"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Ambari Agent

"""
from ambari_commons import os_utils
from ambari_commons.inet_utils import download_file
from ambari_commons.os_windows import SystemWideLock

from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import File
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.reload_windows_env import reload_windows_env
from resource_management.libraries.functions.windows_service_utils import check_windows_service_exists
import socket
import os
import glob


__all__ = ['install_windows_msi']

msi_save_dir = None
hdp_log_dir = "c:\\hadoop\\logs"
hdp_data_dir = "c:\\hadoopDefaultData"
local_host = socket.getfqdn()
db_flavor = "DERBY"
cluster_properties = """#Log directory
HDP_LOG_DIR={hdp_log_dir}

#Data directory
HDP_DATA_DIR={hdp_data_dir}

#hosts
NAMENODE_HOST={local_host}
SECONDARY_NAMENODE_HOST={local_host}
RESOURCEMANAGER_HOST={local_host}
HIVE_SERVER_HOST={local_host}
OOZIE_SERVER_HOST={local_host}
WEBHCAT_HOST={local_host}
SLAVE_HOSTS={local_host}
ZOOKEEPER_HOSTS={local_host}
CLIENT_HOSTS={local_host}
HBASE_MASTER={local_host}
HBASE_REGIONSERVERS={local_host}
FLUME_HOSTS={local_host}
FALCON_HOST={local_host}
KNOX_HOST={local_host}
STORM_NIMBUS={local_host}
STORM_SUPERVISORS={local_host}

#Database host
DB_FLAVOR={db_flavor}
DB_HOSTNAME={local_host}
DB_PORT=1527

#Hive properties
HIVE_DB_NAME=hive
HIVE_DB_USERNAME=hive
HIVE_DB_PASSWORD=hive

#Oozie properties
OOZIE_DB_NAME=oozie
OOZIE_DB_USERNAME=oozie
OOZIE_DB_PASSWORD=oozie
"""

INSTALL_MSI_CMD = 'cmd /C start /wait msiexec /qn /i  {hdp_msi_path} /lv {hdp_log_path} MSIUSEREALADMINDETECTION=1 ' \
                  'HDP_LAYOUT={hdp_layout_path} DESTROY_DATA=yes HDP_USER_PASSWORD={hadoop_password_arg} HDP=yes ' \
                  'KNOX=yes KNOX_MASTER_SECRET="AmbariHDP2Windows" FALCON=yes STORM=yes HBase=yes STORM=yes FLUME=yes'
CREATE_SERVICE_SCRIPT = os.path.abspath("sbin\createservice.ps1")
CREATE_SERVICE_CMD = 'cmd /C powershell -File "{script}" -username hadoop -password "{password}" -servicename ' \
                     '{servicename} -hdpresourcesdir "{resourcedir}" -servicecmdpath "{servicecmd}"'
INSTALL_MARKER_OK = "msi.installed"
INSTALL_MARKER_FAILED = "msi.failed"
_working_dir = None


def _ensure_services_created(hadoop_password):
  resource_dir_hdfs = os.path.join(os.environ["HADOOP_HDFS_HOME"], "bin")
  service_cmd_hdfs = os.path.join(os.environ["HADOOP_HDFS_HOME"], "bin", "hdfs.cmd")
  if not check_windows_service_exists("journalnode"):
    Execute(CREATE_SERVICE_CMD.format(script=CREATE_SERVICE_SCRIPT, password=hadoop_password, servicename="journalnode",
                                      resourcedir=resource_dir_hdfs, servicecmd=service_cmd_hdfs), logoutput=True)
  if not check_windows_service_exists("zkfc"):
    Execute(CREATE_SERVICE_CMD.format(script=CREATE_SERVICE_SCRIPT, password=hadoop_password, servicename="zkfc",
                                      resourcedir=resource_dir_hdfs, servicecmd=service_cmd_hdfs), logoutput=True)


# creating symlinks to services folders to avoid using stack-dependent paths
def _create_symlinks():
  # folders
  Execute("cmd /c mklink /d %HADOOP_NODE%\\hadoop %HADOOP_HOME%")
  Execute("cmd /c mklink /d %HADOOP_NODE%\\hive %HIVE_HOME%")
  # files pairs (symlink_path, path_template_to_target_file), use * to replace file version
  links_pairs = [
    ("%HADOOP_HOME%\\share\\hadoop\\tools\\lib\\hadoop-streaming.jar",
     "%HADOOP_HOME%\\share\\hadoop\\tools\\lib\\hadoop-streaming-*.jar"),
    ("%HIVE_HOME%\\hcatalog\\share\\webhcat\\svr\\lib\\hive-webhcat.jar",
     "%HIVE_HOME%\\hcatalog\\share\\webhcat\\svr\\lib\\hive-webhcat-*.jar"),
    ("%HIVE_HOME%\\lib\\zookeeper.jar", "%HIVE_HOME%\\lib\\zookeeper-*.jar")
  ]
  for link_pair in links_pairs:
    link, target = link_pair
    target = glob.glob(os.path.expandvars(target))[0].replace("\\\\", "\\")
    Execute('cmd /c mklink "{0}" "{1}"'.format(link, target))


# check if services exists and marker file present
def _is_msi_installed():
  return os.path.exists(os.path.join(_working_dir, INSTALL_MARKER_OK)) and check_windows_service_exists("namenode")


# check if msi was installed correctly and raise Fail in case of broken install
def _validate_msi_install():
  if not _is_msi_installed() and os.path.exists(os.path.join(_working_dir, INSTALL_MARKER_FAILED)):
    Fail("Current or previous hdp.msi install failed. Check hdp.msi install logs")
  return _is_msi_installed()


def _write_marker():
  if check_windows_service_exists("namenode"):
    open(os.path.join(_working_dir, INSTALL_MARKER_OK), "w").close()
  else:
    open(os.path.join(_working_dir, INSTALL_MARKER_FAILED), "w").close()


def install_windows_msi(msi_url, save_dir, save_file, hadoop_password):
  global _working_dir
  _working_dir = save_dir
  save_dir = os.path.abspath(save_dir)
  msi_save_dir = save_dir
  # system wide lock to prevent simultaneous installations(when first task failed on timeout)
  install_lock = SystemWideLock("hdp_msi_lock")
  try:
    # try to acquire lock
    if not install_lock.lock():
      Logger.info("Some other task currently installing hdp.msi, waiting for 10 min for finish")
      if not install_lock.lock(600000):
        raise Fail("Timeout on acquiring lock")
    if _validate_msi_install():
      Logger.info("hdp.msi already installed")
      return

    # install msi
    download_file(msi_url, os.path.join(msi_save_dir, save_file))
    File(os.path.join(msi_save_dir, "properties.txt"), content=cluster_properties.format(hdp_log_dir=hdp_log_dir,
                                                                                         hdp_data_dir=hdp_data_dir,
                                                                                         local_host=local_host,
                                                                                         db_flavor=db_flavor))
    hdp_msi_path = os_utils.quote_path(os.path.join(save_dir, "hdp.msi"))
    hdp_log_path = os_utils.quote_path(os.path.join(save_dir, "hdp.log"))
    hdp_layout_path = os_utils.quote_path(os.path.join(save_dir, "properties.txt"))
    hadoop_password_arg = os_utils.quote_path(hadoop_password)

    Execute(
      INSTALL_MSI_CMD.format(hdp_msi_path=hdp_msi_path, hdp_log_path=hdp_log_path, hdp_layout_path=hdp_layout_path,
                             hadoop_password_arg=hadoop_password_arg))
    reload_windows_env()
    # create additional services manually due to hdp.msi limitaitons
    _ensure_services_created(hadoop_password)
    _create_symlinks()
    # finalizing install
    _write_marker()
    _validate_msi_install()
  finally:
    install_lock.unlock()
