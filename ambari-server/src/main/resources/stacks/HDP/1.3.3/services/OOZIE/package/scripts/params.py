from resource_management import *
import status_params

# server configurations
config = Script.get_config()

oozie_user = config['configurations']['global']['oozie_user']
smokeuser = config['configurations']['global']['smokeuser']
conf_dir = "/etc/oozie/conf"
hadoop_conf_dir = "/etc/hadoop/conf"
user_group = config['configurations']['global']['user_group']
jdk_location = config['hostLevelParams']['jdk_location']
check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
hadoop_prefix = "/usr"
oozie_tmp_dir = "/var/tmp/oozie"
oozie_hdfs_user_dir = format("/user/{oozie_user}")
oozie_pid_dir = status_params.oozie_pid_dir
pid_file = status_params.pid_file
hadoop_jar_location = "/usr/lib/hadoop/"
# for HDP2 it's "/usr/share/HDP-oozie/ext-2.2.zip"
ext_js_path = "/usr/share/HDP-oozie/ext.zip"
oozie_libext_dir = "/usr/lib/oozie/libext"
lzo_enabled = config['configurations']['global']['lzo_enabled']
security_enabled = config['configurations']['global']['security_enabled']

kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
oozie_service_keytab = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.keytab.file']
oozie_principal = config['configurations']['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal']
smokeuser_keytab = config['configurations']['global']['smokeuser_keytab']
oozie_keytab = config['configurations']['global']['oozie_keytab']

oracle_driver_jar_name = "ojdbc6.jar"
java_share_dir = "/usr/share/java"

java_home = config['hostLevelParams']['java_home']
oozie_metastore_user_name = config['configurations']['oozie-site']['oozie.service.JPAService.jdbc.username']
oozie_metastore_user_passwd = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.password","")
oozie_jdbc_connection_url = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.url", "")
oozie_log_dir = config['configurations']['global']['oozie_log_dir']
oozie_data_dir = config['configurations']['global']['oozie_data_dir']
oozie_lib_dir = "/var/lib/oozie/"
oozie_webapps_dir = "/var/lib/oozie/oozie-server/webapps/"

jdbc_driver_name = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.driver", "")

if jdbc_driver_name == "com.mysql.jdbc.Driver":
  jdbc_driver_jar = "/usr/share/java/mysql-connector-java.jar"
elif jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
  jdbc_driver_jar = "/usr/share/java/ojdbc6.jar"
else:
  jdbc_driver_jar = ""
  
if lzo_enabled or jdbc_driver_name:
  jar_option = "-jars"         
else:
  jar_option = ""
  
lzo_jar_suffix = "/usr/lib/hadoop/lib/hadoop-lzo-0.5.0.jar" if lzo_enabled else ""
  
if lzo_enabled and jdbc_driver_name:
    jar_path = format("{lzo_jar_suffix}:{jdbc_driver_jar}")        
else:
    jar_path = "{lzo_jar_suffix}{jdbc_driver_jar}"

#oozie-log4j.properties
if ('oozie-log4j' in config['configurations']):
  log4j_props = config['configurations']['oozie-log4j']
else:
  log4j_props = None
