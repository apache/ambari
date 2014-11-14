#!/usr/bin/env python

'''
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
'''
import datetime
import fileinput
import random
import socket
import stat
import sys
import urllib2

from ambari_commons.exceptions import *
from serverConfiguration import *
from setupActions import *
from userInput import *


SSL_PASSWORD_FILE = "pass.txt"
SSL_PASSIN_FILE = "passin.txt"

# openssl command
VALIDATE_KEYSTORE_CMD = "openssl pkcs12 -info -in '{0}' -password file:'{1}' -passout file:'{2}'"
EXPRT_KSTR_CMD = "openssl pkcs12 -export -in '{0}' -inkey '{1}' -certfile '{0}' -out '{4}' -password file:'{2}' -passin file:'{3}'"
CHANGE_KEY_PWD_CND = 'openssl rsa -in {0} -des3 -out {0}.secured -passout pass:{1}'
GET_CRT_INFO_CMD = 'openssl x509 -dates -subject -in {0}'

#keytool commands
keytool_bin = "keytool"
if OSCheck.is_windows_os():
  keytool_bin = "keytool.exe"

KEYTOOL_IMPORT_CERT_CMD = "{0}" + os.sep + "bin" + os.sep + keytool_bin + " -import -alias '{1}' -storetype '{2}' -file '{3}' -storepass '{4}' -noprompt"
KEYTOOL_DELETE_CERT_CMD = "{0}" + os.sep + "bin" + os.sep + keytool_bin + " -delete -alias '{1}' -storepass '{2}' -noprompt"
KEYTOOL_KEYSTORE = " -keystore '{0}'"

java_bin = "java"
if OSCheck.is_windows_os():
  java_bin = "java.exe"

SECURITY_PROVIDER_GET_CMD = "{0}" + os.sep + "bin" + os.sep + java_bin + " -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.security.encryption" +\
                          ".CredentialProvider GET {3} {4} {5} " +\
                          "> " + SERVER_OUT_FILE + " 2>&1"

SECURITY_PROVIDER_PUT_CMD = "{0}" + os.sep + "bin" + os.sep + java_bin + " -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.security.encryption" +\
                          ".CredentialProvider PUT {3} {4} {5} " +\
                          "> " + SERVER_OUT_FILE + " 2>&1"

SECURITY_PROVIDER_KEY_CMD = "{0}" + os.sep + "bin" + os.sep + java_bin + " -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.security.encryption" +\
                          ".MasterKeyServiceImpl {3} {4} {5} " +\
                          "> " + SERVER_OUT_FILE + " 2>&1"

SSL_KEY_DIR = 'security.server.keys_dir'
SSL_API_PORT = 'client.api.ssl.port'
SSL_API = 'api.ssl'
SSL_SERVER_CERT_NAME = 'client.api.ssl.cert_name'
SSL_SERVER_KEY_NAME = 'client.api.ssl.key_name'
SSL_CERT_FILE_NAME = "https.crt"
SSL_KEY_FILE_NAME = "https.key"
SSL_KEYSTORE_FILE_NAME = "https.keystore.p12"
SSL_KEY_PASSWORD_FILE_NAME = "https.pass.txt"
SSL_KEY_PASSWORD_LENGTH = 50
DEFAULT_SSL_API_PORT = 8443
SSL_DATE_FORMAT = '%b  %d %H:%M:%S %Y GMT'

#SSL certificate metainfo
COMMON_NAME_ATTR = 'CN'
NOT_BEFORE_ATTR = 'notBefore'
NOT_AFTER_ATTR = 'notAfter'

SRVR_TWO_WAY_SSL_PORT_PROPERTY = "security.server.two_way_ssl.port"
SRVR_TWO_WAY_SSL_PORT = "8441"

SRVR_ONE_WAY_SSL_PORT_PROPERTY = "security.server.one_way_ssl.port"
SRVR_ONE_WAY_SSL_PORT = "8440"

SECURITY_KEYS_DIR = "security.server.keys_dir"
SECURITY_MASTER_KEY_LOCATION = "security.master.key.location"
SECURITY_KEY_IS_PERSISTED = "security.master.key.ispersisted"
SECURITY_KEY_ENV_VAR_NAME = "AMBARI_SECURITY_MASTER_KEY"
SECURITY_MASTER_KEY_FILENAME = "master"
SECURITY_IS_ENCRYPTION_ENABLED = "security.passwords.encryption.enabled"
SECURITY_KERBEROS_JASS_FILENAME = "krb5JAASLogin.conf"

GANGLIA_HTTPS = 'ganglia.https'
NAGIOS_HTTPS = 'nagios.https'

SSL_TRUSTSTORE_PASSWORD_ALIAS = "ambari.ssl.trustStore.password"
SSL_TRUSTSTORE_PATH_PROPERTY = "ssl.trustStore.path"
SSL_TRUSTSTORE_PASSWORD_PROPERTY = "ssl.trustStore.password"
SSL_TRUSTSTORE_TYPE_PROPERTY = "ssl.trustStore.type"

DEFAULT_PASSWORD = "bigdata"
PASSWORD_PATTERN = "^[a-zA-Z0-9_-]*$"

LDAP_MGR_PASSWORD_ALIAS = "ambari.ldap.manager.password"
LDAP_MGR_PASSWORD_PROPERTY = "authentication.ldap.managerPassword"
LDAP_MGR_USERNAME_PROPERTY = "authentication.ldap.managerDn"

REGEX_IP_ADDRESS = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
REGEX_HOSTNAME = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"
REGEX_HOSTNAME_PORT = "^(.*:[0-9]{1,5}$)"
REGEX_TRUE_FALSE = "^(true|false)?$"
REGEX_ANYTHING = ".*"

CLIENT_SECURITY_KEY = "client.security"

# ownership/permissions mapping
# path - permissions - user - group - recursive
# Rules are executed in the same order as they are listed
# {0} in user/group will be replaced by customized ambari-server username
NR_ADJUST_OWNERSHIP_LIST = [

  ("/var/log/ambari-server", "644", "{0}", True),
  ("/var/log/ambari-server", "755", "{0}", False),
  ("/var/run/ambari-server", "644", "{0}", True),
  ("/var/run/ambari-server", "755", "{0}", False),
  ("/var/run/ambari-server/bootstrap", "755", "{0}", False),
  ("/var/lib/ambari-server/ambari-env.sh", "700", "{0}", False),
  ("/var/lib/ambari-server/keys", "600", "{0}", True),
  ("/var/lib/ambari-server/keys", "700", "{0}", False),
  ("/var/lib/ambari-server/keys/db", "700", "{0}", False),
  ("/var/lib/ambari-server/keys/db/newcerts", "700", "{0}", False),
  ("/var/lib/ambari-server/keys/.ssh", "700", "{0}", False),
  ("/var/lib/ambari-server/resources/stacks/", "755", "{0}", True),
  ("/var/lib/ambari-server/resources/custom_actions/", "755", "{0}", True),
  ("/etc/ambari-server/conf", "644", "{0}", True),
  ("/etc/ambari-server/conf", "755", "{0}", False),
  ("/etc/ambari-server/conf/password.dat", "640", "{0}", False),
  # Also, /etc/ambari-server/conf/password.dat
  # is generated later at store_password_file
]


def is_valid_https_port(port):
  properties = get_ambari_properties()
  if properties == -1:
    print "Error getting ambari properties"
    return False

  one_way_port = properties[SRVR_ONE_WAY_SSL_PORT_PROPERTY]
  if not one_way_port:
    one_way_port = SRVR_ONE_WAY_SSL_PORT

  two_way_port = properties[SRVR_TWO_WAY_SSL_PORT_PROPERTY]
  if not two_way_port:
    two_way_port = SRVR_TWO_WAY_SSL_PORT

  if port.strip() == one_way_port.strip():
    print "Port for https can't match the port for one way authentication port(" + one_way_port + ")"
    return False

  if port.strip() == two_way_port.strip():
    print "Port for https can't match the port for two way authentication port(" + two_way_port + ")"
    return False

  return True

def run_component_https_cmd(cmd):
  retcode, out, err = run_os_command(cmd)

  if not retcode == 0:
    err = 'Error occured during truststore setup ! :' + out + " : " + err
    raise FatalException(1, err)

def get_cert_info(path):
  retcode, out, err = run_os_command(GET_CRT_INFO_CMD.format(path))

  if retcode != 0:
    print 'Error getting Certificate info'
    print err
    return None

  if out:
    certInfolist = out.split(os.linesep)
  else:
    print 'Empty Certificate info'
    return None

  notBefore = None
  notAfter = None
  subject = None

  for item in range(len(certInfolist)):

    if certInfolist[item].startswith('notAfter='):
      notAfter = certInfolist[item].split('=')[1]

    if certInfolist[item].startswith('notBefore='):
      notBefore = certInfolist[item].split('=')[1]

    if certInfolist[item].startswith('subject='):
      subject = certInfolist[item].split('=', 1)[1]

  #Convert subj to dict
  pattern = re.compile(r"[A-Z]{1,2}=[\w.-]{1,}")
  if subject:
    subjList = pattern.findall(subject)
    keys = [item.split('=')[0] for item in subjList]
    values = [item.split('=')[1] for item in subjList]
    subjDict = dict(zip(keys, values))

    result = subjDict
    result['notBefore'] = notBefore
    result['notAfter'] = notAfter
    result['subject'] = subject

    return result
  else:
    return {}

def is_valid_cert_exp(certInfoDict):
  if certInfoDict.has_key(NOT_BEFORE_ATTR):
    notBefore = certInfoDict[NOT_BEFORE_ATTR]
  else:
    print_warning_msg('There is no Not Before value in Certificate')
    return False

  if certInfoDict.has_key(NOT_AFTER_ATTR):
    notAfter = certInfoDict['notAfter']
  else:
    print_warning_msg('There is no Not After value in Certificate')
    return False

  notBeforeDate = datetime.datetime.strptime(notBefore, SSL_DATE_FORMAT)
  notAfterDate = datetime.datetime.strptime(notAfter, SSL_DATE_FORMAT)

  currentDate = datetime.datetime.now()

  if currentDate > notAfterDate:
    print_warning_msg('Certificate expired on: ' + str(notAfterDate))
    return False

  if currentDate < notBeforeDate:
    print_warning_msg('Certificate will be active from: ' + str(notBeforeDate))
    return False

  return True

def is_valid_cert_host(certInfoDict):
  if certInfoDict.has_key(COMMON_NAME_ATTR):
   commonName = certInfoDict[COMMON_NAME_ATTR]
  else:
    print_warning_msg('There is no Common Name in Certificate')
    return False

  fqdn = get_fqdn()

  if not fqdn:
    print_warning_msg('Failed to get server FQDN')
    return False

  if commonName != fqdn:
    print_warning_msg('Common Name in Certificate: ' + commonName + ' does not match the server FQDN: ' + fqdn)
    return False

  return True

def generate_random_string(length=SSL_KEY_PASSWORD_LENGTH):
  chars = string.digits + string.ascii_letters
  return ''.join(random.choice(chars) for x in range(length))

def import_cert_and_key(security_server_keys_dir):
  import_cert_path = get_validated_filepath_input(\
                    "Enter path to Certificate: ",\
                    "Certificate not found")
  import_key_path  =  get_validated_filepath_input(\
                      "Enter path to Private Key: ", "Private Key not found")
  pem_password = get_validated_string_input("Please enter password for Private Key: ", "", None, None, True)

  certInfoDict = get_cert_info(import_cert_path)

  if not certInfoDict:
    print_warning_msg('Unable to get Certificate information')
  else:
    #Validate common name of certificate
    if not is_valid_cert_host(certInfoDict):
      print_warning_msg('Unable to validate Certificate hostname')

    #Validate issue and expirations dates of certificate
    if not is_valid_cert_exp(certInfoDict):
      print_warning_msg('Unable to validate Certificate issue and expiration dates')

  #jetty requires private key files with non-empty key passwords
  retcode = 0
  err = ''
  if not pem_password:
    print 'Generating random password for HTTPS keystore...done.'
    pem_password = generate_random_string()
    retcode, out, err = run_os_command(CHANGE_KEY_PWD_CND.format(
      import_key_path, pem_password))
    import_key_path += '.secured'

  if retcode == 0:
    keystoreFilePath = os.path.join(security_server_keys_dir,\
                                    SSL_KEYSTORE_FILE_NAME)
    keystoreFilePathTmp = os.path.join(tempfile.gettempdir(),\
                                       SSL_KEYSTORE_FILE_NAME)
    passFilePath = os.path.join(security_server_keys_dir,\
                                SSL_KEY_PASSWORD_FILE_NAME)
    passFilePathTmp = os.path.join(tempfile.gettempdir(),\
      SSL_KEY_PASSWORD_FILE_NAME)
    passinFilePath = os.path.join(tempfile.gettempdir(),\
                                   SSL_PASSIN_FILE)
    passwordFilePath = os.path.join(tempfile.gettempdir(),\
                                   SSL_PASSWORD_FILE)

    with open(passFilePathTmp, 'w+') as passFile:
      passFile.write(pem_password)
      passFile.close
      pass

    set_file_permissions(passFilePath, "660", read_ambari_user(), False)

    copy_file(passFilePathTmp, passinFilePath)
    copy_file(passFilePathTmp, passwordFilePath)

    retcode, out, err = run_os_command(EXPRT_KSTR_CMD.format(import_cert_path,\
    import_key_path, passwordFilePath, passinFilePath, keystoreFilePathTmp))
  if retcode == 0:
   print 'Importing and saving Certificate...done.'
   import_file_to_keystore(keystoreFilePathTmp, keystoreFilePath)
   import_file_to_keystore(passFilePathTmp, passFilePath)

   import_file_to_keystore(import_cert_path, os.path.join(\
                          security_server_keys_dir, SSL_CERT_FILE_NAME))
   import_file_to_keystore(import_key_path, os.path.join(\
                          security_server_keys_dir, SSL_KEY_FILE_NAME))

   #Validate keystore
   retcode, out, err = run_os_command(VALIDATE_KEYSTORE_CMD.format(keystoreFilePath,\
   passwordFilePath, passinFilePath))

   remove_file(passinFilePath)
   remove_file(passwordFilePath)

   if not retcode == 0:
     print 'Error during keystore validation occured!:'
     print err
     return False

   return True
  else:
   print_error_msg('Could not import Certificate and Private Key.')
   print 'SSL error on exporting keystore: ' + err.rstrip() + \
         '.\nPlease ensure that provided Private Key password is correct and ' +\
         're-import Certificate.'

   return False

def import_cert_and_key_action(security_server_keys_dir, properties):
  if import_cert_and_key(security_server_keys_dir):
   properties.process_pair(SSL_SERVER_CERT_NAME, SSL_CERT_FILE_NAME)
   properties.process_pair(SSL_SERVER_KEY_NAME, SSL_KEY_FILE_NAME)
   properties.process_pair(SSL_API, "true")
   return True
  else:
   return False

def get_delete_cert_command(jdk_path, alias, truststore_path, truststore_password):
  cmd = KEYTOOL_DELETE_CERT_CMD.format(jdk_path, alias, truststore_password)
  if truststore_path:
    cmd += KEYTOOL_KEYSTORE.format(truststore_path)
  return cmd

def get_import_cert_command(jdk_path, alias, truststore_type, import_cert_path, truststore_path, truststore_password):
  cmd = KEYTOOL_IMPORT_CERT_CMD.format(jdk_path, alias, truststore_type, import_cert_path, truststore_password)
  if truststore_path:
    cmd += KEYTOOL_KEYSTORE.format(truststore_path)
  return cmd

def import_file_to_keystore(source, destination):
  shutil.copy(source, destination)
  set_file_permissions(destination, "660", read_ambari_user(), False)

def get_truststore_type(properties):

  truststore_type = properties.get_property(SSL_TRUSTSTORE_TYPE_PROPERTY)
  if not truststore_type:
    SSL_TRUSTSTORE_TYPE_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_TYPE_PROPERTY, "jks")

    truststore_type = get_validated_string_input(
      "TrustStore type [jks/jceks/pkcs12] {0}:".format(get_prompt_default(SSL_TRUSTSTORE_TYPE_DEFAULT)),
      SSL_TRUSTSTORE_TYPE_DEFAULT,
      "^(jks|jceks|pkcs12)?$", "Wrong type", False)

    if truststore_type:
      properties.process_pair(SSL_TRUSTSTORE_TYPE_PROPERTY, truststore_type)

  return truststore_type

def get_truststore_path(properties):

  truststore_path = properties.get_property(SSL_TRUSTSTORE_PATH_PROPERTY)
  if not truststore_path:
    SSL_TRUSTSTORE_PATH_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_PATH_PROPERTY)

    while not truststore_path:
      truststore_path = get_validated_string_input(
        "Path to TrustStore file {0}:".format(get_prompt_default(SSL_TRUSTSTORE_PATH_DEFAULT)),
        SSL_TRUSTSTORE_PATH_DEFAULT,
        ".*", False, False)

    if truststore_path:
      properties.process_pair(SSL_TRUSTSTORE_PATH_PROPERTY, truststore_path)

  return truststore_path

def get_truststore_password(properties):
  truststore_password = properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
  isSecure = get_is_secure(properties)
  if truststore_password:
    if isSecure:
      truststore_password = decrypt_password_for_alias(SSL_TRUSTSTORE_PASSWORD_ALIAS)
  else:
    truststore_password = read_password("", ".*", "Password for TrustStore:", "Invalid characters in password")
    if truststore_password:
      encrypted_password = get_encrypted_password(SSL_TRUSTSTORE_PASSWORD_ALIAS, truststore_password, properties)
      properties.process_pair(SSL_TRUSTSTORE_PASSWORD_PROPERTY, encrypted_password)

  return truststore_password

def read_password(passwordDefault=DEFAULT_PASSWORD,
                  passwordPattern=PASSWORD_PATTERN,
                  passwordPrompt=None,
                  passwordDescr=None):
  # setup password
  if passwordPrompt is None:
    passwordPrompt = 'Password (' + passwordDefault + '): '

  if passwordDescr is None:
    passwordDescr = "Invalid characters in password. Use only alphanumeric or " \
                    "_ or - characters"

  password = get_validated_string_input(passwordPrompt, passwordDefault,
                                        passwordPattern, passwordDescr, True)

  if not password:
    print 'Password cannot be blank.'
    return read_password(passwordDefault, passwordPattern, passwordPrompt,
                   passwordDescr)

  if password != passwordDefault:
    password1 = get_validated_string_input("Re-enter password: ",
                                           passwordDefault, passwordPattern, passwordDescr, True)
    if password != password1:
      print "Passwords do not match"
      return read_password(passwordDefault, passwordPattern, passwordPrompt,
                      passwordDescr)

  return password

def get_is_secure(properties):
  isSecure = properties.get_property(SECURITY_IS_ENCRYPTION_ENABLED)
  isSecure = True if isSecure and isSecure.lower() == 'true' else False
  return isSecure

def encrypt_password(alias, password):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, None)
  return get_encrypted_password(alias, password, properties)

def get_encrypted_password(alias, password, properties):
  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)
  if isSecure:
    masterKey = None
    if not masterKeyFile:
      # Encryption enabled but no master key file found
      masterKey = get_original_master_key(properties)

    retCode = save_passwd_for_alias(alias, password, masterKey)
    if retCode != 0:
      print 'Failed to save secure password!'
      return password
    else:
      return get_alias_string(alias)

  return password

def is_alias_string(passwdStr):
  regex = re.compile("\$\{alias=[\w\.]+\}")
  # Match implies string at beginning of word
  r = regex.match(passwdStr)
  if r is not None:
    return True
  else:
    return False

def get_alias_string(alias):
  return "${alias=" + alias + "}"

def get_alias_from_alias_string(aliasStr):
  return aliasStr[8:-1]

def read_passwd_for_alias(alias, masterKey=""):
  if alias:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to " + JDK_INSTALL_DIR)
      return 1

    tempFileName = "ambari.passwd"
    passwd = ""
    tempDir = tempfile.gettempdir()
    #create temporary file for writing
    tempFilePath = tempDir + os.sep + tempFileName
    file = open(tempFilePath, 'w+')
    os.chmod(tempFilePath, stat.S_IREAD | stat.S_IWRITE)
    file.close()

    if masterKey is None or masterKey == "":
      masterKey = "None"

    command = SECURITY_PROVIDER_GET_CMD.format(jdk_path,
      get_conf_dir(), get_ambari_classpath(), alias, tempFilePath, masterKey)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider get passwd: " +
                   str(retcode))
    if retcode != 0:
      print 'ERROR: Unable to read password from store. alias = ' + alias
    else:
      passwd = open(tempFilePath, 'r').read()
      # Remove temporary file
    os.remove(tempFilePath)
    return passwd
  else:
    print_error_msg("Alias is unreadable.")

def decrypt_password_for_alias(alias):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, None)

  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)
  if isSecure:
    masterKey = None
    if not masterKeyFile:
      # Encryption enabled but no master key file found
      masterKey = get_original_master_key(properties)

    return read_passwd_for_alias(alias, masterKey)
  else:
    return alias

def save_passwd_for_alias(alias, passwd, masterKey=""):
  if alias and passwd:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to " + JDK_INSTALL_DIR)
      return 1

    if masterKey is None or masterKey == "":
      masterKey = "None"

    command = SECURITY_PROVIDER_PUT_CMD.format(jdk_path, get_conf_dir(),
      get_ambari_classpath(), alias, passwd, masterKey)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider save passwd: " +
                   str(retcode))
    return retcode
  else:
    print_error_msg("Alias or password is unreadable.")

def get_is_persisted(properties):
  keyLocation = get_master_key_location(properties)
  masterKeyFile = search_file(SECURITY_MASTER_KEY_FILENAME, keyLocation)
  isPersisted = True if masterKeyFile else False

  return (isPersisted, masterKeyFile)

def get_credential_store_location(properties):
  store_loc = properties[SECURITY_KEYS_DIR]
  if store_loc is None or store_loc == "":
    store_loc = "/var/lib/ambari-server/keys/credentials.jceks"
  else:
    store_loc += os.sep + "credentials.jceks"
  return store_loc

def get_master_key_location(properties):
  keyLocation = properties[SECURITY_MASTER_KEY_LOCATION]
  if keyLocation is None or keyLocation == "":
    keyLocation = properties[SECURITY_KEYS_DIR]
  return keyLocation

def get_original_master_key(properties):
  try:
    masterKey = get_validated_string_input('Enter current Master Key: ',
                                             "", ".*", "", True, False)
  except KeyboardInterrupt:
    print 'Exiting...'
    sys.exit(1)

  # Find an alias that exists
  alias = None
  property = properties.get_property(JDBC_PASSWORD_PROPERTY)
  if property and is_alias_string(property):
    alias = JDBC_RCA_PASSWORD_ALIAS

  alias = None
  property = properties.get_property(JDBC_METRICS_PASSWORD_PROPERTY)
  if property and is_alias_string(property):
    alias = JDBC_METRICS_PASSWORD_ALIAS

  if not alias:
    property = properties.get_property(LDAP_MGR_PASSWORD_PROPERTY)
    if property and is_alias_string(property):
      alias = LDAP_MGR_PASSWORD_ALIAS

  if not alias:
    property = properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
    if property and is_alias_string(property):
      alias = SSL_TRUSTSTORE_PASSWORD_ALIAS

  # Decrypt alias with master to validate it, if no master return
  if alias and masterKey:
    password = read_passwd_for_alias(alias, masterKey)
    if not password:
      print "ERROR: Master key does not match."
      return get_original_master_key(properties)

  return masterKey

def read_master_key(isReset=False):
  passwordPattern = ".*"
  passwordPrompt = "Please provide master key for locking the credential store: "
  passwordDescr = "Invalid characters in password. Use only alphanumeric or "\
                  "_ or - characters"
  passwordDefault = ""
  if isReset:
    passwordPrompt = "Enter new Master Key: "

  masterKey = get_validated_string_input(passwordPrompt, passwordDefault,
                            passwordPattern, passwordDescr, True, True)

  if not masterKey:
    print "Master Key cannot be empty!"
    return read_master_key()

  masterKey2 = get_validated_string_input("Re-enter master key: ",
      passwordDefault, passwordPattern, passwordDescr, True, True)

  if masterKey != masterKey2:
    print "Master key did not match!"
    return read_master_key()

  return masterKey

def save_master_key(master_key, key_location, persist=True):
  if master_key:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to " + JDK_INSTALL_DIR)
      return 1
    command = SECURITY_PROVIDER_KEY_CMD.format(jdk_path,
      get_ambari_classpath(), get_conf_dir(), master_key, key_location, persist)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider save KEY: " +
                   str(retcode))
  else:
    print_error_msg("Master key cannot be None.")

def store_password_file(password, filename):
  conf_file = find_properties_file()
  passFilePath = os.path.join(os.path.dirname(conf_file),
    filename)

  with open(passFilePath, 'w+') as passFile:
    passFile.write(password)
  print_info_msg("Adjusting filesystem permissions")
  ambari_user = read_ambari_user()
  set_file_permissions(passFilePath, "660", ambari_user, False)

  #Windows paths need double backslashes, otherwise the Ambari server deserializer will think the single \ are escape markers
  return passFilePath.replace('\\', '\\\\')

def remove_password_file(filename):
  conf_file = find_properties_file()
  passFilePath = os.path.join(os.path.dirname(conf_file),
    filename)

  if os.path.exists(passFilePath):
    try:
      os.remove(passFilePath)
    except Exception, e:
      print_warning_msg('Unable to remove password file: ' + str(e))
      return 1
  pass
  return 0

def adjust_directory_permissions(ambari_user):
  properties = get_ambari_properties()
  bootstrap_dir = get_value_from_properties(properties, BOOTSTRAP_DIR_PROPERTY)
  print_info_msg("Cleaning bootstrap directory ({0}) contents...".format(bootstrap_dir))
  shutil.rmtree(bootstrap_dir, True) #Ignore the non-existent dir error
  os.makedirs(bootstrap_dir)
  # Add master key and credential store if exists
  keyLocation = get_master_key_location(properties)
  masterKeyFile = search_file(SECURITY_MASTER_KEY_FILENAME, keyLocation)
  if masterKeyFile:
    NR_ADJUST_OWNERSHIP_LIST.append((masterKeyFile, MASTER_KEY_FILE_PERMISSIONS, "{0}", "{0}", False))
  credStoreFile = get_credential_store_location(properties)
  if os.path.exists(credStoreFile):
    NR_ADJUST_OWNERSHIP_LIST.append((credStoreFile, CREDENTIALS_STORE_FILE_PERMISSIONS, "{0}", "{0}", False))
  trust_store_location = properties[SSL_TRUSTSTORE_PATH_PROPERTY]
  if trust_store_location:
    NR_ADJUST_OWNERSHIP_LIST.append((trust_store_location, TRUST_STORE_LOCATION_PERMISSIONS, "{0}", "{0}", False))
  print "Adjusting ambari-server permissions and ownership..."
  for pack in NR_ADJUST_OWNERSHIP_LIST:
    file = pack[0]
    mod = pack[1]
    user = pack[2].format(ambari_user)
    recursive = pack[3]
    set_file_permissions(file, mod, user, recursive)

def get_fqdn():
  properties = get_ambari_properties()
  if properties == -1:
    print "Error reading ambari properties"
    return None

  get_fqdn_service_url = properties[GET_FQDN_SERVICE_URL]
  try:
    handle = urllib2.urlopen(get_fqdn_service_url, '', 2)
    str = handle.read()
    handle.close()
    return str
  except Exception:
    return socket.getfqdn()

def configure_ldap_password():
  passwordDefault = ""
  passwordPrompt = 'Enter Manager Password* : '
  passwordPattern = ".*"
  passwordDescr = "Invalid characters in password."

  password = read_password(passwordDefault, passwordPattern, passwordPrompt,
    passwordDescr)

  return password

def setup_https(args):
  if not is_root():
    err = 'ambari-server setup-https should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)
  args.exit_message = None
  if not SILENT:
    properties = get_ambari_properties()
    try:
      security_server_keys_dir = properties.get_property(SSL_KEY_DIR)
      client_api_ssl_port = DEFAULT_SSL_API_PORT if properties.get_property(SSL_API_PORT) in ("")\
                            else properties.get_property(SSL_API_PORT)
      api_ssl = properties.get_property(SSL_API) in ['true']
      cert_was_imported = False
      cert_must_import = True
      if api_ssl:
       if get_YN_input("Do you want to disable HTTPS [y/n] (n)? ", False):
        properties.process_pair(SSL_API, "false")
        cert_must_import=False
       else:
        properties.process_pair(SSL_API_PORT, \
                                get_validated_string_input(\
                                "SSL port ["+str(client_api_ssl_port)+"] ? ",\
                                str(client_api_ssl_port),\
                                "^[0-9]{1,5}$", "Invalid port.", False, validatorFunction = is_valid_https_port))
        cert_was_imported = import_cert_and_key_action(security_server_keys_dir, properties)
      else:
       if get_YN_input("Do you want to configure HTTPS [y/n] (y)? ", True):
        properties.process_pair(SSL_API_PORT,\
        get_validated_string_input("SSL port ["+str(client_api_ssl_port)+"] ? ",\
        str(client_api_ssl_port), "^[0-9]{1,5}$", "Invalid port.", False, validatorFunction = is_valid_https_port))
        cert_was_imported = import_cert_and_key_action(security_server_keys_dir, properties)
       else:
        return False

      if cert_must_import and not cert_was_imported:
        print 'Setup of HTTPS failed. Exiting.'
        return False

      conf_file = find_properties_file()
      f = open(conf_file, 'w')
      properties.store(f, "Changed by 'ambari-server setup-https' command")

      ambari_user = read_ambari_user()
      if ambari_user:
        adjust_directory_permissions(ambari_user)
      return True
    except (KeyError), e:
      err = 'Property ' + str(e) + ' is not defined'
      raise FatalException(1, err)
  else:
    warning = "setup-https is not enabled in silent mode."
    raise NonFatalException(warning)

def setup_component_https(component, command, property, alias):

  if not SILENT:
    jdk_path = find_jdk()
    if jdk_path is None:
      err = "No JDK found, please run the \"ambari-server setup\" " \
                      "command to install a JDK automatically or install any " \
                      "JDK manually to " + JDK_INSTALL_DIR
      raise FatalException(1, err)

    properties = get_ambari_properties()

    use_https = properties.get_property(property) in ['true']

    if use_https:
      if get_YN_input("Do you want to disable HTTPS for " + component + " [y/n] (n)? ", False):

        truststore_path = get_truststore_path(properties)
        truststore_password = get_truststore_password(properties)

        run_component_https_cmd(get_delete_cert_command(jdk_path, alias, truststore_path, truststore_password))

        properties.process_pair(property, "false")

      else:
        return
    else:
      if get_YN_input("Do you want to configure HTTPS for " + component + " [y/n] (y)? ", True):

        truststore_type = get_truststore_type(properties)
        truststore_path = get_truststore_path(properties)
        truststore_password = get_truststore_password(properties)

        run_os_command(get_delete_cert_command(jdk_path, alias, truststore_path, truststore_password))

        import_cert_path = get_validated_filepath_input(\
                          "Enter path to " + component + " Certificate: ",\
                          "Certificate not found")

        run_component_https_cmd(get_import_cert_command(jdk_path, alias, truststore_type, import_cert_path, truststore_path, truststore_password))

        properties.process_pair(property, "true")

      else:
        return

    conf_file = find_properties_file()
    f = open(conf_file, 'w')
    properties.store(f, "Changed by 'ambari-server " + command + "' command")

  else:
    print command + " is not enabled in silent mode."

def setup_master_key():
  if not is_root():
    err = 'Ambari-server setup should be run with '\
                     'root-level privileges'
    raise FatalException(4, err)

  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, "Failed to read properties file.")

  db_windows_auth_prop = properties.get_property(JDBC_USE_INTEGRATED_AUTH_PROPERTY)
  db_sql_auth = False if db_windows_auth_prop and db_windows_auth_prop.lower() == 'true' else True
  db_password = properties.get_property(JDBC_PASSWORD_PROPERTY)
  # Encrypt passwords cannot be called before setup
  if db_sql_auth and not db_password:
    print 'Please call "setup" before "encrypt-passwords". Exiting...'
    return 1

  # Check configuration for location of master key
  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)

  # Read clear text password from file
  if db_sql_auth and not is_alias_string(db_password) and os.path.isfile(db_password):
    with open(db_password, 'r') as passwdfile:
      db_password = passwdfile.read()

  # Read clear text metrics password from file
  db_metrics_windows_auth_prop = properties.get_property(JDBC_METRICS_USE_INTEGRATED_AUTH_PROPERTY)
  db_metrics_sql_auth = False if db_metrics_windows_auth_prop and db_metrics_windows_auth_prop.lower() == 'true' else True
  metrics_password = properties.get_property(JDBC_METRICS_PASSWORD_PROPERTY)
  if db_metrics_sql_auth and not is_alias_string(metrics_password) and os.path.isfile(metrics_password):
    with open(metrics_password, 'r') as passwdfile:
      metrics_password = passwdfile.read()

  ldap_password = properties.get_property(LDAP_MGR_PASSWORD_PROPERTY)
  ts_password = properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
  resetKey = False
  masterKey = None

  if isSecure:
    print "Password encryption is enabled."
    resetKey = get_YN_input("Do you want to reset Master Key? [y/n] (n): ", False)

  # For encrypting of only unencrypted passwords without resetting the key ask
  # for master key if not persisted.
  if isSecure and not isPersisted and not resetKey:
    print "Master Key not persisted."
    masterKey = get_original_master_key(properties)
  pass

  # Make sure both passwords are clear-text if master key is lost
  if resetKey:
    if not isPersisted:
      print "Master Key not persisted."
      masterKey = get_original_master_key(properties)
      # Unable get the right master key or skipped question <enter>
      if not masterKey:
        print "To disable encryption, do the following:"
        print "- Edit " + find_properties_file() + \
              " and set " + SECURITY_IS_ENCRYPTION_ENABLED + " = " + "false."
        err = "{0} is already encrypted. Please call {1} to store unencrypted" \
              " password and call 'encrypt-passwords' again."
        if db_sql_auth and db_password and is_alias_string(db_password):
          print err.format('- Database password', "'" + SETUP_ACTION + "'")
        if db_metrics_sql_auth and metrics_password and is_alias_string(metrics_password):
            print err.format('- Metrics Database password', "'" + SETUP_ACTION + "'")
        if ldap_password and is_alias_string(ldap_password):
          print err.format('- LDAP manager password', "'" + LDAP_SETUP_ACTION + "'")
        if ts_password and is_alias_string(ts_password):
          print err.format('TrustStore password', "'" + LDAP_SETUP_ACTION + "'")

        return 1
      pass
    pass
  pass

  # Read back any encrypted passwords
  if db_sql_auth  and db_password and is_alias_string(db_password):
    db_password = read_passwd_for_alias(JDBC_RCA_PASSWORD_ALIAS, masterKey)
  if db_metrics_sql_auth and metrics_password and is_alias_string(metrics_password):
      metrics_password = read_passwd_for_alias(JDBC_METRICS_PASSWORD_ALIAS, masterKey)
  if ldap_password and is_alias_string(ldap_password):
    ldap_password = read_passwd_for_alias(LDAP_MGR_PASSWORD_ALIAS, masterKey)
  if ts_password and is_alias_string(ts_password):
    ts_password = read_passwd_for_alias(SSL_TRUSTSTORE_PASSWORD_ALIAS, masterKey)
  # Read master key, if non-secure or reset is true
  if resetKey or not isSecure:
    masterKey = read_master_key(resetKey)
    persist = get_YN_input("Do you want to persist master key. If you choose "\
                           "not to persist, you need to provide the Master "\
                           "Key while starting the ambari server as an env "\
                           "variable named " + SECURITY_KEY_ENV_VAR_NAME +\
                           " or the start will prompt for the master key."
                           " Persist [y/n] (y)? ", True)
    if persist:
      save_master_key(masterKey, get_master_key_location(properties) + os.sep +
                                 SECURITY_MASTER_KEY_FILENAME, persist)
    elif not persist and masterKeyFile:
      try:
        os.remove(masterKeyFile)
        print_info_msg("Deleting master key file at location: " + str(
          masterKeyFile))
      except Exception, e:
        print 'ERROR: Could not remove master key file. %s' % e
    # Blow up the credential store made with previous key, if any
    store_file = get_credential_store_location(properties)
    if os.path.exists(store_file):
      try:
        os.remove(store_file)
      except:
        print_warning_msg("Failed to remove credential store file.")
      pass
    pass
  pass

  propertyMap = {SECURITY_IS_ENCRYPTION_ENABLED: 'true'}
  # Encrypt only un-encrypted passwords
  if db_password and not is_alias_string(db_password):
    retCode = save_passwd_for_alias(JDBC_RCA_PASSWORD_ALIAS, db_password, masterKey)
    if retCode != 0:
      print 'Failed to save secure database password.'
    else:
      propertyMap[JDBC_PASSWORD_PROPERTY] = get_alias_string(JDBC_RCA_PASSWORD_ALIAS)
      remove_password_file(JDBC_PASSWORD_FILENAME)
      if properties.get_property(JDBC_RCA_PASSWORD_FILE_PROPERTY):
        propertyMap[JDBC_RCA_PASSWORD_FILE_PROPERTY] = get_alias_string(JDBC_RCA_PASSWORD_ALIAS)
  pass

  if metrics_password and not is_alias_string(metrics_password):
    retCode = save_passwd_for_alias(JDBC_METRICS_PASSWORD_ALIAS, metrics_password, masterKey)
    if retCode != 0:
      print 'Failed to save secure metrics database password.'
    else:
      propertyMap[JDBC_METRICS_PASSWORD_PROPERTY] = get_alias_string(JDBC_METRICS_PASSWORD_ALIAS)
      remove_password_file(JDBC_METRICS_PASSWORD_FILENAME)
  pass

  if ldap_password and not is_alias_string(ldap_password):
    retCode = save_passwd_for_alias(LDAP_MGR_PASSWORD_ALIAS, ldap_password, masterKey)
    if retCode != 0:
      print 'Failed to save secure LDAP password.'
    else:
      propertyMap[LDAP_MGR_PASSWORD_PROPERTY] = get_alias_string(LDAP_MGR_PASSWORD_ALIAS)
  pass

  if ts_password and not is_alias_string(ts_password):
    retCode = save_passwd_for_alias(SSL_TRUSTSTORE_PASSWORD_ALIAS, ts_password, masterKey)
    if retCode != 0:
      print 'Failed to save secure TrustStore password.'
    else:
      propertyMap[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = get_alias_string(SSL_TRUSTSTORE_PASSWORD_ALIAS)
  pass

  update_properties_2(properties, propertyMap)

  # Since files for store and master are created we need to ensure correct
  # permissions
  ambari_user = read_ambari_user()
  if ambari_user:
    adjust_directory_permissions(ambari_user)

  return 0

def setup_ambari_krb5_jaas():
  jaas_conf_file = search_file(SECURITY_KERBEROS_JASS_FILENAME, get_conf_dir())
  if os.path.exists(jaas_conf_file):
    print 'Setting up Ambari kerberos JAAS configuration to access ' +\
          'secured Hadoop daemons...'
    principal = get_validated_string_input('Enter ambari server\'s kerberos '
                  'principal name (ambari@EXAMPLE.COM): ', 'ambari@EXAMPLE.COM', '.*', '', False,
                  False)
    keytab = get_validated_string_input('Enter keytab path for ambari '
                  'server\'s kerberos principal: ',
                  '/etc/security/keytabs/ambari.keytab', '.*', False, False,
                  validatorFunction=is_valid_filepath)

    for line in fileinput.FileInput(jaas_conf_file, inplace=1):
      line = re.sub('keyTab=.*$', 'keyTab="' + keytab + '"', line)
      line = re.sub('principal=.*$', 'principal="' + principal + '"', line)
      print line,

  else:
    raise NonFatalException('No jaas config file found at location: ' +
                            jaas_conf_file)

def setup_ldap():
  if not is_root():
    err = 'Ambari-server setup-ldap should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)

  properties = get_ambari_properties()
  isSecure = get_is_secure(properties)
  # python2.x dict is not ordered
  ldap_property_list_reqd = ["authentication.ldap.primaryUrl",
                        "authentication.ldap.secondaryUrl",
                        "authentication.ldap.useSSL",
                        "authentication.ldap.usernameAttribute",
                        "authentication.ldap.baseDn",
                        "authentication.ldap.bindAnonymously"]

  ldap_property_list_opt = ["authentication.ldap.managerDn",
                             LDAP_MGR_PASSWORD_PROPERTY,
                             SSL_TRUSTSTORE_TYPE_PROPERTY,
                             SSL_TRUSTSTORE_PATH_PROPERTY,
                             SSL_TRUSTSTORE_PASSWORD_PROPERTY]

  ldap_property_list_truststore=[SSL_TRUSTSTORE_TYPE_PROPERTY,
                                 SSL_TRUSTSTORE_PATH_PROPERTY,
                                 SSL_TRUSTSTORE_PASSWORD_PROPERTY]

  ldap_property_list_passwords=[LDAP_MGR_PASSWORD_PROPERTY,
                                SSL_TRUSTSTORE_PASSWORD_PROPERTY]

  LDAP_PRIMARY_URL_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[0])
  LDAP_SECONDARY_URL_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[1])
  LDAP_USE_SSL_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[2], "false")
  LDAP_USER_ATT_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[3], "uid")
  LDAP_BASE_DN_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[4])
  LDAP_BIND_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[5], "false")
  LDAP_MGR_DN_DEFAULT = get_value_from_properties(properties, ldap_property_list_opt[0])
  SSL_TRUSTSTORE_TYPE_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_TYPE_PROPERTY, "jks")
  SSL_TRUSTSTORE_PATH_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_PATH_PROPERTY)


  ldap_properties_map_reqd =\
  {
    ldap_property_list_reqd[0]:(LDAP_PRIMARY_URL_DEFAULT, "Primary URL* {{host:port}} {0}: ".format(get_prompt_default(LDAP_PRIMARY_URL_DEFAULT)), False),\
    ldap_property_list_reqd[1]:(LDAP_SECONDARY_URL_DEFAULT, "Secondary URL {{host:port}} {0}: ".format(get_prompt_default(LDAP_SECONDARY_URL_DEFAULT)), True),\
    ldap_property_list_reqd[2]:(LDAP_USE_SSL_DEFAULT, "Use SSL* [true/false] {0}: ".format(get_prompt_default(LDAP_USE_SSL_DEFAULT)), False),\
    ldap_property_list_reqd[3]:(LDAP_USER_ATT_DEFAULT, "User name attribute* {0}: ".format(get_prompt_default(LDAP_USER_ATT_DEFAULT)), False),\
    ldap_property_list_reqd[4]:(LDAP_BASE_DN_DEFAULT, "Base DN* {0}: ".format(get_prompt_default(LDAP_BASE_DN_DEFAULT)), False),\
    ldap_property_list_reqd[5]:(LDAP_BIND_DEFAULT, "Bind anonymously* [true/false] {0}: ".format(get_prompt_default(LDAP_BIND_DEFAULT)), False)\
  }

  ldap_property_value_map = {}
  for idx, key in enumerate(ldap_property_list_reqd):
    if idx in [0, 1]:
      pattern = REGEX_HOSTNAME_PORT
    elif idx in [2, 5]:
      pattern = REGEX_TRUE_FALSE
    else:
      pattern = REGEX_ANYTHING
    input = get_validated_string_input(ldap_properties_map_reqd[key][1],
      ldap_properties_map_reqd[key][0], pattern,
      "Invalid characters in the input!", False, ldap_properties_map_reqd[key][2])
    if input is not None and input != "":
      ldap_property_value_map[key] = input

  bindAnonymously = ldap_property_value_map["authentication.ldap.bindAnonymously"]
  anonymous = (bindAnonymously and bindAnonymously.lower() == 'true')
  mgr_password = None
  # Ask for manager credentials only if bindAnonymously is false
  if not anonymous:
    username = get_validated_string_input("Manager DN* {0}: ".format(
      get_prompt_default(LDAP_MGR_DN_DEFAULT)), LDAP_MGR_DN_DEFAULT, ".*",
                "Invalid characters in the input!", False, False)
    ldap_property_value_map[LDAP_MGR_USERNAME_PROPERTY] = username
    mgr_password = configure_ldap_password()
    ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] = mgr_password

  useSSL = ldap_property_value_map["authentication.ldap.useSSL"]
  ldaps = (useSSL and useSSL.lower() == 'true')
  ts_password = None

  if ldaps:
    truststore_default = "n"
    truststore_set = bool(SSL_TRUSTSTORE_PATH_DEFAULT)
    if truststore_set:
      truststore_default = "y"
    custom_trust_store = get_YN_input("Do you want to provide custom TrustStore for Ambari [y/n] ({0})?".
                                      format(truststore_default),
                                      truststore_set)
    if custom_trust_store:
      ts_type = get_validated_string_input(
        "TrustStore type [jks/jceks/pkcs12] {0}:".format(get_prompt_default(SSL_TRUSTSTORE_TYPE_DEFAULT)),
        SSL_TRUSTSTORE_TYPE_DEFAULT,
        "^(jks|jceks|pkcs12)?$", "Wrong type", False)
      ts_path = None
      while True:
        ts_path = get_validated_string_input(
          "Path to TrustStore file {0}:".format(get_prompt_default(SSL_TRUSTSTORE_PATH_DEFAULT)),
          SSL_TRUSTSTORE_PATH_DEFAULT,
          ".*", False, False)
        if os.path.exists(ts_path):
          break
        else:
          print 'File not found.'

      ts_password = read_password("", ".*", "Password for TrustStore:", "Invalid characters in password")

      ldap_property_value_map[SSL_TRUSTSTORE_TYPE_PROPERTY] = ts_type
      ldap_property_value_map[SSL_TRUSTSTORE_PATH_PROPERTY] = ts_path
      ldap_property_value_map[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = ts_password
      pass
    else:
      properties.removeOldProp(SSL_TRUSTSTORE_TYPE_PROPERTY)
      properties.removeOldProp(SSL_TRUSTSTORE_PATH_PROPERTY)
      properties.removeOldProp(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
    pass
  pass

  print '=' * 20
  print 'Review Settings'
  print '=' * 20
  for property in ldap_property_list_reqd:
    if property in ldap_property_value_map:
      print("%s: %s" % (property, ldap_property_value_map[property]))

  for property in ldap_property_list_opt:
    if ldap_property_value_map.has_key(property):
      if property not in ldap_property_list_passwords:
        print("%s: %s" % (property, ldap_property_value_map[property]))
      else:
        print("%s: %s" % (property, BLIND_PASSWORD))

  save_settings = get_YN_input("Save settings [y/n] (y)? ", True)

  if save_settings:
    ldap_property_value_map[CLIENT_SECURITY_KEY] = 'ldap'
    if isSecure:
      if mgr_password:
        encrypted_passwd = encrypt_password(LDAP_MGR_PASSWORD_ALIAS, mgr_password)
        if mgr_password != encrypted_passwd:
          ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] = encrypted_passwd
      pass
      if ts_password:
        encrypted_passwd = encrypt_password(SSL_TRUSTSTORE_PASSWORD_ALIAS, ts_password)
        if ts_password != encrypted_passwd:
          ldap_property_value_map[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = encrypted_passwd
      pass
    pass

    # Persisting values
    update_properties_2(properties, ldap_property_value_map)
    print 'Saving...done'

  return 0
