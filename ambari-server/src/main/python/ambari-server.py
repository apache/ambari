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

import optparse
import sys
import os
import signal
import subprocess
import re
import urllib
import string
import glob

SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
SETUP_DB_CMD = "sudo -u postgres psql -f {1} -d {0}"
GET_SE_LINUX_ST_CMD = "sestatus"
SE_STATUS_DISABLED="disabled"
SE_STATUS_ENABLED="enabled"
IP_TBLS_ST_CMD = "service iptables status"
IP_TBLS_STOP_CMD = "service iptables stop"
IP_TBLS_ENABLED="Firewall is running"
IP_TBLS_DISABLED="Firewall is not running"
IP_TBLS_SRVC_NT_FND="iptables: unrecognized service"
SERVER_START_CMD="{0}" + os.sep + "bin" + os.sep + "java -cp {1}"+ os.pathsep + ".." + os.sep + "lib" + os.sep + "ambari-server" + os.sep + "* org.apache.ambari.server.controller.AmbariServer"
# uncomment for debug
# SERVER_START_CMD="{0}" + os.sep + "bin" + os.sep + "java -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -cp {1}"+ os.pathsep + ".." + os.sep + "lib" + os.sep + "ambari-server" + os.sep + "* org.apache.ambari.server.controller.AmbariServer"
AMBARI_CONF_VAR="AMBARI_CONF_DIR"
PG_ST_CMD = "service postgresql status"
PG_START_CMD = "service postgresql start"
PG_STATUS_RUNNING = "running"
PID_DIR="/var/run/ambari-server"
PID_NAME="ambari-server.pid"
AMBARI_PROPERTIES_FILE="ambari.properties"
JDK_LOCAL_FILENAME="jdk-distr.bin"
JDK_MIN_FILESIZE=5000
JDK_INSTALL_DIR="/usr/jdk64"
CREATE_JDK_DIR_CMD = "mkdir -p " + JDK_INSTALL_DIR
MAKE_FILE_EXECUTABLE_CMD = "chmod a+x {0}"

def run_os_command(cmd):
  print 'about to run command: ' + cmd
  process = subprocess.Popen(cmd.split(' '),
                             stdout=subprocess.PIPE,
                             stdin=subprocess.PIPE,
                             stderr=subprocess.PIPE
                            )
  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata
# todo: check if the scheme is already exist
def setup_db(args):
  dbname = args.postgredbname
  file = args.init_script_file
  command = SETUP_DB_CMD.format(dbname, file)
  retcode, outdata, errdata = run_os_command(command)
  if not retcode == 0:
    print errdata
  return retcode

def check_se_down():
  retcode, out, err = run_os_command(GET_SE_LINUX_ST_CMD)
  se_status = re.search('(disabled|enabled)', out).group(0)
  print 'SELinux status:' + se_status
  if se_status == SE_STATUS_DISABLED:
    return 0
  else:
    return 1

def get_conf_dir():
  try:
    conf_dir = os.environ[AMBARI_CONF_VAR]
    return conf_dir
  except KeyError:
    print "Please set value of " + AMBARI_CONF_VAR + "!"
    sys.exit(1)

def search_file(filename, search_path, pathsep=os.pathsep):
  """ Given a search path, find file with requested name """
  for path in string.split(search_path, pathsep):
    candidate = os.path.join(path, filename)
    if os.path.exists(candidate): return os.path.abspath(candidate)
  return None

def ip_tables_down():
  retcode, out, err = run_os_command(IP_TBLS_ST_CMD)
  if out == IP_TBLS_ENABLED:
    print 'iptables is enabled now'
    print 'Stopping iptables service'
    retcode, out, err = run_os_command(IP_TBLS_STOP_CMD)
  print 'iptables is disabled now'
  
  if not retcode == 0:
    print err
  
  if err.strip() == IP_TBLS_SRVC_NT_FND:
    return 0
  else:
   return retcode, out


# A Python replacement for java.util.Properties
# Based on http://code.activestate.com/recipes/496795-a-python-replacement-for-javautilproperties/
class Properties(object):

  def __init__(self, props=None):

    self._props = {}
    self._origprops = {}
    self._keymap = {}

    self.othercharre = re.compile(r'(?<!\\)(\s*\=)|(?<!\\)(\s*\:)')
    self.othercharre2 = re.compile(r'(\s*\=)|(\s*\:)')
    self.bspacere = re.compile(r'\\(?!\s$)')

  def __parse(self, lines):
    lineno=0
    i = iter(lines)
    for line in i:
      lineno += 1
      line = line.strip()
      if not line: continue
      if line[0] == '#': continue
      escaped=False
      sepidx = -1
      flag = 0
      m = self.othercharre.search(line)
      if m:
        first, last = m.span()
        start, end = 0, first
        flag = 1
        wspacere = re.compile(r'(?<![\\\=\:])(\s)')
      else:
        if self.othercharre2.search(line):
          wspacere = re.compile(r'(?<![\\])(\s)')
        start, end = 0, len(line)
      m2 = wspacere.search(line, start, end)
      if m2:
        first, last = m2.span()
        sepidx = first
      elif m:
        first, last = m.span()
        sepidx = last - 1
      while line[-1] == '\\':
        nextline = i.next()
        nextline = nextline.strip()
        lineno += 1
        line = line[:-1] + nextline
      if sepidx != -1:
        key, value = line[:sepidx], line[sepidx+1:]
      else:
        key,value = line,''
      self.processPair(key, value)

  def processPair(self, key, value):
    oldkey = key
    oldvalue = value
    keyparts = self.bspacere.split(key)
    strippable = False
    lastpart = keyparts[-1]
    if lastpart.find('\\ ') != -1:
      keyparts[-1] = lastpart.replace('\\','')
    elif lastpart and lastpart[-1] == ' ':
      strippable = True
    key = ''.join(keyparts)
    if strippable:
      key = key.strip()
      oldkey = oldkey.strip()
    oldvalue = self.unescape(oldvalue)
    value = self.unescape(value)
    self._props[key] = value.strip()
    if self._keymap.has_key(key):
      oldkey = self._keymap.get(key)
      self._origprops[oldkey] = oldvalue.strip()
    else:
      self._origprops[oldkey] = oldvalue.strip()
      self._keymap[key] = oldkey

  def unescape(self, value):
    newvalue = value.replace('\:',':')
    newvalue = newvalue.replace('\=','=')
    return newvalue

  def load(self, stream):
    if type(stream) is not file:
      raise TypeError,'Argument should be a file object!'
    if stream.mode != 'r':
      raise ValueError,'Stream should be opened in read-only mode!'
    try:
      lines = stream.readlines()
      self.__parse(lines)
    except IOError, e:
      raise

  def getProperty(self, key):
    return self._props.get(key,'')

  def propertyNames(self):
    return self._props.keys()

  def getPropertyDict(self):
    return self._props

  def __getitem__(self, name):
    return self.getProperty(name)

  def __getattr__(self, name):
    try:
      return self.__dict__[name]
    except KeyError:
      if hasattr(self._props,name):
        return getattr(self._props, name)


def download_jdk():
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  if conf_file is None:
    print 'File %s not found in search path $%s: %s' % (AMBARI_PROPERTIES_FILE, AMBARI_CONF_VAR, get_conf_dir())
    return -1
  print 'Loading properties from ' + conf_file
  properties = None
  try:
    properties = Properties()
    properties.load(open(conf_file))
  except (Exception), e:
    print 'Could not read "%s": %s' % (conf_file, e)
    return -1
  try:
    jdk_url = properties['jdk.url']
    resources_dir = properties['resources.dir']
  except (KeyError), e:
    print 'Property ' + str(e) + ' is not defined at ' + conf_file
    return -1
  dest_file = resources_dir + os.sep + JDK_LOCAL_FILENAME
  print 'Trying to download JDK from ' + jdk_url + ' to ' + dest_file
  try:
    src_size = int(urllib.urlopen(jdk_url).info()['Content-Length'])
    print 'JDK distribution size is ' + str(src_size) + ' bytes'
    file_exists = os.path.isfile(dest_file)
    file_size = -1
    if file_exists:
      file_size = os.stat(dest_file).st_size
    if file_exists and file_size == src_size:
      print "File already exists"
    else:
      urllib.urlretrieve (jdk_url, dest_file)
      print 'Successfully downloaded JDK distribution to ' + dest_file
  except Exception, e:
    print 'Failed to download JDK: ' + str(e)
    return -1
  downloaded_size = os.stat(dest_file).st_size
  if downloaded_size != src_size or downloaded_size < JDK_MIN_FILESIZE:
    print 'Size of downloaded JDK distribution file is ' + str(downloaded_size) + ' bytes, it is probably \
damaged or incomplete'
    return -1
  print "Installing JDK to {0}".format(JDK_INSTALL_DIR)
  retcode, out, err = run_os_command(CREATE_JDK_DIR_CMD)
  savedPath = os.getcwd()
  os.chdir(JDK_INSTALL_DIR)
  retcode, out, err = run_os_command(MAKE_FILE_EXECUTABLE_CMD.format(dest_file))
  retcode, out, err = run_os_command(dest_file)
  os.chdir(savedPath)
  jdk_version = re.search('Creating (jdk.*)/jre', out).group(1)
  print "Successfully installed JDK to {0}/{1}".format(JDK_INSTALL_DIR, jdk_version)
  return 0

def check_postgre_up():
  retcode, out, err = run_os_command(PG_ST_CMD)
  try:
    pg_status = re.search('(stopped|running)', out).group(0)
  except AttributeError:
    pg_status = None
  if pg_status == PG_STATUS_RUNNING:
    print "Postgre is running"
    return 0
  else:
    print "About to start postgre"
    retcode, out, err = run_os_command(PG_START_CMD)
    return retcode
  
def setup(args):
    
  print 'About to check Postgre'
  retcode = check_postgre_up()
  if not retcode == 0:
    print 'Error! Unable to start postgre server'
    sys.exit(retcode)
    
  print 'About to setup database'
  retcode = setup_db(args)
  if not retcode == 0:
    print 'Error! Running database init script was failed'
    sys.exit(retcode)
  
  print 'About to check SELinux down'
  retcode = check_se_down()
  if not retcode == 0:
    print 'Error! SELinux is enabled, unable to proceed'
    sys.exit(retcode)
   
  print 'About to check iptables down'
  retcode, out = ip_tables_down()
  if (not retcode == 0 and out == IP_TBLS_ENABLED):
    print 'Error! Failed to stop iptables'
    sys.exit(retcode)

  print 'About to download JDK and install it'
  retcode = download_jdk()
  if not retcode == 0:
    print 'Error! Downloading or installing JDK failed'
    sys.exit(retcode)

  print 'Setup was finished sucessfully'

def findJDK():
  print "Looking for available JDKs at " + JDK_INSTALL_DIR
  jdks = glob.glob(JDK_INSTALL_DIR + os.sep + "jdk*")
  jdks.sort()
  print "Found: " + str(jdks)
  count = len(jdks)
  if count == 0:
    return
  jdkPath = jdks[count - 1]
  print "Choosed JDK {0}".format(jdkPath)
  return jdkPath

def start(args):
  conf_dir = get_conf_dir()
  jdk_path = findJDK()
  if jdk_path is None:
    print "No any JDK found, please run \"setup\" command to install it automatically or "\
"install any JDK manually to " + JDK_INSTALL_DIR
    return -1
  retcode = check_postgre_up()
  if not retcode == 0:
    print 'Error! Unable to start postgre server'
    sys.exit(retcode)
  command = SERVER_START_CMD.format(jdk_path, conf_dir)
  #print "Running server: " + command
  server_process = subprocess.Popen(command.split(' '))
  f = open(PID_DIR + os.sep + PID_NAME, "w")
  f.write(str(server_process.pid))
  f.close()
  
def stop(args):
  f = open(PID_DIR + os.sep + PID_NAME, "r")
  pid = int(f.readline())
  os.kill(pid, signal.SIGKILL)
  f.close()
  os.remove(f.name)







  
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] action",)
  parser.add_option('-d', '--postgredbname', default='postgres',
                      help="Database name in postgresql")
  parser.add_option('-f', '--init-script-file', default='setup_db.sql',
                      help="File with setup script")

  (options, args) = parser.parse_args()
  
  if not len(args) == 1:
    print parser.print_help()
    parser.error("Invalid number of arguments")
	
  action = args[0]

  if action == SETUP_ACTION:
    setup(options)
  elif action == START_ACTION:
    start(options)
  elif action == STOP_ACTION:
    stop(options)
  else:
    parser.error("Invalid action")

if __name__ == "__main__":
  main()






