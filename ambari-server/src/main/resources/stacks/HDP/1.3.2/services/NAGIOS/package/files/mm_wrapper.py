#!/usr/bin/env python

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
"""
import sys
import subprocess
import os

N_SGN = 'NAGIOS_SERVICEGROUPNAME'
N_SD = 'NAGIOS__SERVICEHOST_COMPONENT'
N_HOST = 'NAGIOS_HOSTNAME'

LIST_SEPARATOR = "--"
HOSTNAME_PLACEHOLDER = "^^"
IGNORE_DAT_FILE = "/var/nagios/ignore.dat"

# Mode constants
OR = 0
AND = 1
ENV_ONLY = 2
FILTER_MM = 3
LEGACY_CHECK_WRAPPER = 4
MODES = ['or', 'and', 'env_only', 'filter_mm', 'legacy_check_wrapper']


def ignored_host_list(service, component):
  """
  :param service: current service
  :param component: current component
  :return: all hosts where specified host component is in ignored state
  """
  def str_norm(s):
    return s.strip().upper()

  result = []

  try:
    with open(IGNORE_DAT_FILE, 'r') as f:
      lines = filter(None, f.read().split(os.linesep))
  except IOError:
    return result

  if lines:
    for l in lines:
      tokens = l.split(' ')
      if len(tokens) == 3 and str_norm(tokens[1]) == str_norm(service) \
          and str_norm(tokens[2]) == str_norm(component):
        result.append(tokens[0])
  return result


def get_real_service():
  try:
    service = os.environ[N_SGN].strip().upper()  # e.g. 'HBASE'
  except KeyError:
    service = ''
  return service


def get_real_component():
  try:
    comp_name = os.environ[N_SD].strip()
  except KeyError:
    comp_name = ''
  mapping = {
    'HBASEMASTER': 'HBASE_MASTER',
    'REGIONSERVER': 'HBASE_REGIONSERVER',
    'JOBHISTORY': 'MAPREDUCE2',
    'HIVE-METASTORE': 'HIVE_METASTORE',
    'HIVE-SERVER': 'HIVE_SERVER',
    'FLUME': 'FLUME_HANDLER',
    'HUE': 'HUE_SERVER',
    'WEBHCAT': 'WEBHCAT_SERVER',
  }
  if comp_name in mapping:
    comp_name = mapping.get(comp_name)
  return comp_name


def check_output(*popenargs, **kwargs):
  """
  Imitate subprocess.check_output() for python 2.6
  """
  process = subprocess.Popen(stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                             *popenargs, **kwargs)
  output, unused_err = process.communicate()
  retcode = process.poll()
  if retcode:
    cmd = kwargs.get("args")
    if cmd is None:
      cmd = popenargs[0]
    err = subprocess.CalledProcessError(retcode, cmd)
    # Monkey-patching for python 2.6
    err.output = output
    raise err
  return output


def print_usage():
  """
  Prints usage and exits with a non-zero exit code
  """
  print "Usage: mm_wrapper.py MODE HOST1 HOST2 .. HOSTN %s command arg1 arg2 .. argN" % LIST_SEPARATOR
  print "MODE is one of the following: or, and, env_only, filter_mm, legacy_check_wrapper"
  print "%s is a separator between list of hostnames and command with args" % LIST_SEPARATOR
  print "%s is used as a hostname placeholder at command args" % HOSTNAME_PLACEHOLDER
  print "Also script provides $MM_HOSTS shell variable to commands"
  print "NOTE: Script makes use of Nagios-populated env vars %s and %s" % (N_SGN, N_SD)
  print "For more info, please see docstrings at %s" % os.path.realpath(__file__)
  sys.exit(1)


def parse_args(args):
  if not args or not LIST_SEPARATOR in args or args[0] not in MODES:
    print_usage()
  else:
    mode = MODES.index(args[0])  # identify operation mode
    args = args[1:]  # Shift args left
    hostnames = []
    command_line = []
    # Parse command line args
    passed_separator = False  # True if met LIST_SEPARATOR
    for arg in args:
      if not passed_separator:
        if arg != LIST_SEPARATOR:
          #check if was passed list of hosts instead of one
          if ',' in arg:
            hostnames += arg.split(',')
          else:
            hostnames.append(arg)
        else:
          passed_separator = True
      else:
        if arg != LIST_SEPARATOR:
          command_line.append(arg)
        else:  # Something definitely goes wrong
          print "Could not parse arguments: " \
                "There is more than one %s argument." % LIST_SEPARATOR
          print_usage()

    if not command_line:
      print "No command provided."
      print_usage()
    return mode, hostnames, command_line


def do_work(mode, hostnames, command_line):
  # Execute commands
  ignored_hosts = ignored_host_list(get_real_service(), get_real_component())
  empty_check_result = {
    'message': 'No checks have been run (no hostnames provided)',
    'retcode': -1,
    'real_retcode': None
  }
  custom_env = os.environ.copy()
  if ignored_hosts:
    custom_env['MM_HOSTS'] = \
      reduce(lambda a, b: "%s %s" % (a, b), ignored_hosts)
  if mode == OR:
    check_result = work_in_or_mode(hostnames, ignored_hosts, command_line, custom_env, empty_check_result)
  elif mode == AND:
    check_result = work_in_and_mode(hostnames, ignored_hosts, command_line, custom_env, empty_check_result)
  elif mode == ENV_ONLY:
    check_result = work_in_env_only_mode(hostnames, command_line, custom_env)
  elif mode == FILTER_MM:
    check_result = work_in_filter_mm_mode(hostnames, ignored_hosts, command_line, custom_env, empty_check_result)
  else:  # mode == LEGACY_CHECK_WRAPPER:
    check_result = work_in_legacy_check_wrapper_mode(ignored_hosts, command_line, custom_env)
  # Build the final output
  final_output = []
  output = check_result.get('message')
  if output is not None:
    for string in output.splitlines():
      final_output.append(string.strip())
  real_retcode = check_result.get('real_retcode')
  if real_retcode:
    # This string is used at check_aggregate.php when aggregating alerts
    final_output.append("AMBARIPASSIVE=%s" % real_retcode)
  return final_output, check_result.get('retcode')


def work_in_or_mode(hostnames, ignored_hosts, command_line, custom_env, empty_check_result):
  check_result = empty_check_result
  for hostname in hostnames:
    concrete_command_line = map(  # Substitute hostname where needed
                                  lambda x: hostname if x == HOSTNAME_PLACEHOLDER else x,
                                  command_line)
    try:
      returncode = 0
      real_retcode = None
      message = check_output(concrete_command_line, env=custom_env)
    except subprocess.CalledProcessError, e:
      if hostname not in ignored_hosts:
        returncode = e.returncode
      else:  # Host is in MM
        real_retcode = e.returncode
      message = e.output
    really_positive_result = hostname not in ignored_hosts and returncode == 0
    if check_result.get('retcode') <= returncode or really_positive_result:
      check_result = {
        'message': message,
        'retcode': returncode,
        'real_retcode': real_retcode  # Real (not suppressed) program retcode
      }
    if really_positive_result:
      break  # Exit on first real success
  return check_result


def work_in_and_mode(hostnames, ignored_hosts, command_line, custom_env, empty_check_result):
  check_result = empty_check_result
  for hostname in hostnames:
    concrete_command_line = map(  # Substitute hostname where needed
                                  lambda x: hostname if x == HOSTNAME_PLACEHOLDER else x,
                                  command_line)
    try:
      returncode = 0
      real_retcode = None
      message = check_output(concrete_command_line, env=custom_env)
    except subprocess.CalledProcessError, e:
      if hostname not in ignored_hosts:
        returncode = e.returncode
      else:
        real_retcode = e.returncode
      message = e.output
    if check_result.get('retcode') <= returncode:
      check_result = {
        'message': message,
        'retcode': returncode,
        'real_retcode': real_retcode  # Real (not suppressed) program retcode
      }
  return check_result


def work_in_env_only_mode(hostnames, command_line, custom_env):
  concrete_command_line = []
  for item in command_line:
    if item == HOSTNAME_PLACEHOLDER:
      concrete_command_line.extend(hostnames)
    else:
      concrete_command_line.append(item)
  try:
    returncode = 0
    message = check_output(concrete_command_line, env=custom_env)
  except subprocess.CalledProcessError, e:
    returncode = e.returncode
    message = e.output
  check_result = {
    'message': message,
    'retcode': returncode,
    'real_retcode': None  # Real (not suppressed) program retcode
  }
  return check_result


def work_in_filter_mm_mode(hostnames, ignored_hosts, command_line, custom_env, empty_check_result):
  not_mm_hosts = [hostname for hostname in hostnames if hostname not in ignored_hosts]
  if not not_mm_hosts:  # All hosts have been filtered
    return empty_check_result
  else:
    return work_in_env_only_mode(not_mm_hosts, command_line, custom_env)


def work_in_legacy_check_wrapper_mode(ignored_hosts, command_line, custom_env):
  host = os.environ[N_HOST]
  result = work_in_env_only_mode([host], command_line, custom_env)
  real_retcode = result['retcode']
  if host in ignored_hosts and real_retcode != 0:  # Ignore fail
    result['retcode'] = 0
    result['real_retcode'] = real_retcode
  return result


def main():
  """
  This script allows to run nagios service check commands for host components
  located at different hosts.
  Also script passes to every command a $MM_HOSTS shell variable with a list of
  hosts that are in MM

  or mode: return 0 exit code if at least one service check succeeds.
  Command exits on a first success.
  Failures for host components that are in MM are suppressed (return code
  is set to 0).
  If command fails for all provided hostnames, script returns alert with the
  greatest exit code value.

  and mode:
  Perform checks of all host components (effectively ignoring negative results
  for MM components). If service check is successful for all hosts, script
  also returns zero exit code. Otherwise alert with the greatest exit code is
  returned.

  env_only mode:
  Pass list of all hosts to command and run it once. The only role of
  mm_wrapper script in this mode is to provide properly initialized
  $MM_HOSTS env variable to command being run. All duties of ignoring failures
  of MM host components are delegated to a command being run.

  filter_mm
  Similar to env_only mode. The only difference is that hostnames for
  host components that are in MM are filtered (not passed to command at all)

  legacy_check_wrapper
  Designed as a drop-in replacement for check_wrapper.sh . It reads $NAGIOS_HOSTNAME
  env var and ignores check results if host component on this host is in MM.
  When host subtitution symbol is encountered, hostname defined by $NAGIOS_HOSTNAME
  is substituted,
  """
  args = sys.argv[1:]  # Shift args left
  mode, hostnames, command_line = parse_args(args)
  output, ret_code = do_work(mode, hostnames, command_line)
  for line in output:
    print line
  sys.exit(ret_code)


if __name__ == "__main__":
  main()
