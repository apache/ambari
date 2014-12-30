# !/usr/bin/env python

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
import os
import getpass
import shlex
import subprocess
import sys
import time
import win32api
import win32event
import win32service
import win32con
import win32serviceutil
import wmi
import random
import string

import ctypes

from win32security import *
from win32api import *
from winerror import ERROR_INVALID_HANDLE
from win32process import GetExitCodeProcess, STARTF_USESTDHANDLES, STARTUPINFO, CreateProcessAsUser
from win32event import WaitForSingleObject, INFINITE
import msvcrt
import tempfile
from win32event import *
from win32api import CloseHandle

from ambari_commons.exceptions import *
from logging_utils import *

from win32security import LsaOpenPolicy, POLICY_CREATE_ACCOUNT, POLICY_LOOKUP_NAMES, LookupAccountName, \
  LsaAddAccountRights, LsaRemoveAccountRights, SE_SERVICE_LOGON_NAME
from win32net import NetUserAdd
from win32netcon import USER_PRIV_USER, UF_NORMAL_ACCOUNT, UF_SCRIPT
import pywintypes

SERVICE_STATUS_UNKNOWN = "unknown"
SERVICE_STATUS_STARTING = "starting"
SERVICE_STATUS_RUNNING = "running"
SERVICE_STATUS_STOPPING = "stopping"
SERVICE_STATUS_STOPPED = "stopped"
SERVICE_STATUS_NOT_INSTALLED = "not installed"

WHOAMI_GROUPS = "whoami /groups"
ADMIN_ACCOUNT = "BUILTIN\\Administrators"

class OSVERSIONINFOEXW(ctypes.Structure):
    _fields_ = [('dwOSVersionInfoSize', ctypes.c_ulong),
                ('dwMajorVersion', ctypes.c_ulong),
                ('dwMinorVersion', ctypes.c_ulong),
                ('dwBuildNumber', ctypes.c_ulong),
                ('dwPlatformId', ctypes.c_ulong),
                ('szCSDVersion', ctypes.c_wchar*128),
                ('wServicePackMajor', ctypes.c_ushort),
                ('wServicePackMinor', ctypes.c_ushort),
                ('wSuiteMask', ctypes.c_ushort),
                ('wProductType', ctypes.c_byte),
                ('wReserved', ctypes.c_byte)]

def get_windows_version():
    """
    Get's the OS major and minor versions.  Returns a tuple of
    (OS_MAJOR, OS_MINOR).
    """
    os_version = OSVERSIONINFOEXW()
    os_version.dwOSVersionInfoSize = ctypes.sizeof(os_version)
    retcode = ctypes.windll.Ntdll.RtlGetVersion(ctypes.byref(os_version))
    if retcode != 0:
        raise Exception("Failed to get OS version")

    return os_version.dwMajorVersion, os_version.dwMinorVersion, os_version.dwBuildNumber

CHECK_FIREWALL_SCRIPT = """[string]$CName = $env:computername
$reg = [Microsoft.Win32.RegistryKey]::OpenRemoteBaseKey("LocalMachine",$computer)
$domain = $reg.OpenSubKey("System\CurrentControlSet\Services\SharedAccess\Parameters\FirewallPolicy\DomainProfile").GetValue("EnableFirewall")
$standart = $reg.OpenSubKey("System\CurrentControlSet\Services\SharedAccess\Parameters\FirewallPolicy\StandardProfile").GetValue("EnableFirewall")
$public = $reg.OpenSubKey("System\CurrentControlSet\Services\SharedAccess\Parameters\FirewallPolicy\PublicProfile").GetValue("EnableFirewall")
Write-Host $domain
Write-Host $standart
Write-Host $public
"""

def _create_tmp_files():
  out_file = tempfile.TemporaryFile(mode="r+b")
  err_file = tempfile.TemporaryFile(mode="r+b")
  return (msvcrt.get_osfhandle(out_file.fileno()),
          msvcrt.get_osfhandle(err_file.fileno()),
          out_file,
          err_file)


def _get_files_output(out, err):
  out.seek(0)
  err.seek(0)
  return out.read().strip(), err.read().strip()


def _safe_duplicate_handle(h):
  try:
    h = DuplicateHandle(GetCurrentProcess(),
                        h,
                        GetCurrentProcess(),
                        0,
                        True,
                        win32con.DUPLICATE_SAME_ACCESS)
    return True, h
  except Exception as exc:
    if exc.winerror == ERROR_INVALID_HANDLE:
      return True, None
  return False, None


def run_os_command_impersonated(cmd, user, password, domain='.'):
  si = STARTUPINFO()

  out_handle, err_handle, out_file, err_file = _create_tmp_files()

  ok, si.hStdInput = _safe_duplicate_handle(GetStdHandle(STD_INPUT_HANDLE))

  if not ok:
    raise Exception("Unable to create StdInput for child process")
  ok, si.hStdOutput = _safe_duplicate_handle(out_handle)
  if not ok:
    raise Exception("Unable to create StdOut for child process")
  ok, si.hStdError = _safe_duplicate_handle(err_handle)
  if not ok:
    raise Exception("Unable to create StdErr for child process")

  si.dwFlags = STARTF_USESTDHANDLES
  si.lpDesktop = ""

  user_token = LogonUser(user, domain, password, win32con.LOGON32_LOGON_SERVICE, win32con.LOGON32_PROVIDER_DEFAULT)
  primary_token = DuplicateTokenEx(user_token, SecurityImpersonation, 0, TokenPrimary)
  info = CreateProcessAsUser(primary_token, None, cmd, None, None, 1, 0, None, None, si)

  hProcess, hThread, dwProcessId, dwThreadId = info
  hThread.Close()

  try:
    WaitForSingleObject(hProcess, INFINITE)
  except KeyboardInterrupt:
    pass

  out, err = _get_files_output(out_file, err_file)
  exitcode = GetExitCodeProcess(hProcess)

  return exitcode, out, err

def run_os_command(cmd, env=None):
  if isinstance(cmd,basestring):
    cmd = cmd.replace("\\", "\\\\")
    cmd = shlex.split(cmd)
  process = subprocess.Popen(cmd,
                             stdout=subprocess.PIPE,
                             stdin=subprocess.PIPE,
                             stderr=subprocess.PIPE,
                             env=env
  )
  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata

# execute powershell script passed in script_content. Script will be in temporary file to avoid different escape
# and formatting problems.
def run_powershell_script(script_content):
  tmp_dir = tempfile.gettempdir()
  random_filename = ''.join(random.choice(string.lowercase) for i in range(10))
  script_file = open(os.path.join(tmp_dir,random_filename+".ps1"),"w")
  script_file.write(script_content)
  script_file.close()
  result = run_os_command("powershell  -ExecutionPolicy unrestricted -File {0}".format(script_file.name))
  os.remove(script_file.name)
  return result

def os_change_owner(filePath, user):
  cmd = ['icacls', filePath, '/setowner', user]
  retcode, outdata, errdata = run_os_command(cmd)
  return retcode

def os_is_root():
  '''
  Checks whether the current user is a member of the Administrators group
  Returns True if yes, otherwise False
  '''
  retcode, out, err = run_os_command(WHOAMI_GROUPS)
  if retcode != 0:
    err_msg = "Unable to check the current user's group memberships. Command {0} returned exit code {1} with message: {2}".format(WHOAMI_GROUPS, retcode, err)
    print_warning_msg(err_msg)
    raise FatalException(retcode, err_msg)

  #Check for Administrators group membership
  if -1 != out.find('\n' + ADMIN_ACCOUNT):
    return True

  return False

def os_set_file_permissions(file, mod, recursive, user):
  retcode = 0

  #WARN_MSG = "Command {0} returned exit code {1} with message: {2}"
  #if recursive:
  #  params = " -R "
  #else:
  #  params = ""
  #command = NR_CHMOD_CMD.format(params, mod, file)
  #retcode, out, err = run_os_command(command)
  #if retcode != 0:
  #  print_warning_msg(WARN_MSG.format(command, file, err))
  #command = NR_CHOWN_CMD.format(params, user, file)
  #retcode, out, err = run_os_command(command)
  #if retcode != 0:
  #  print_warning_msg(WARN_MSG.format(command, file, err))

  # rights = mod
  # acls_remove_cmd = "icacls {0} /remove {1}".format(file, user)
  # retcode, out, err = run_os_command(acls_remove_cmd)
  # if retcode == 0:
  #   acls_modify_cmd = "icacls {0} /grant {1}:{2}".format(file, user, rights)
  #   retcode, out, err = run_os_command(acls_modify_cmd)
  return retcode


def os_set_open_files_limit(maxOpenFiles):
  # No open files limit in Windows. Not messing around with the System Resource Manager, at least for now.
  pass


def os_getpass(prompt, stream=None):
  """Prompt for password with echo off, using Windows getch()."""
  if sys.stdin is not sys.__stdin__:
    return getpass.fallback_getpass(prompt, stream)

  import msvcrt

  for c in prompt:
    msvcrt.putch(c)

  pw = ""
  while True:
    c = msvcrt.getch()
    if c == '\r' or c == '\n':
      break
    if c == '\003':
      raise KeyboardInterrupt
    if c == '\b':
      if pw == '':
        pass
      else:
        pw = pw[:-1]
        msvcrt.putch('\b')
        msvcrt.putch(" ")
        msvcrt.putch('\b')
    else:
      pw = pw + c
      msvcrt.putch("*")

  msvcrt.putch('\r')
  msvcrt.putch('\n')
  return pw

#[fbarca] Not used for now, keep it around just in case
def wait_for_pid_wmi(processName, parentPid, pattern, timeout):
  """
    Check pid for existence during timeout
  """
  tstart = time.time()
  pid_live = 0

  c = wmi.WMI(find_classes=False)
  qry = "select * from Win32_Process where Name=\"%s\" and ParentProcessId=%d" % (processName, parentPid)

  while int(time.time() - tstart) <= timeout:
    for proc in c.query(qry):
      cmdLine = proc.CommandLine
      if cmdLine is not None and pattern in cmdLine:
        return pid_live
    time.sleep(1)
  return 0


#need this for redirecting output form python process to file
class SyncStreamWriter(object):
  def __init__(self, stream, hMutexWrite):
    self.stream = stream
    self.hMutexWrite = hMutexWrite

  def write(self, data):
    #Ensure that the output is thread-safe when writing from 2 separate streams into the same file
    #  (typical when redirecting both stdout and stderr to the same file).
    win32event.WaitForSingleObject(self.hMutexWrite, win32event.INFINITE)
    try:
      self.stream.write(data)
      self.stream.flush()
    finally:
      win32event.ReleaseMutex(self.hMutexWrite)

  def __getattr__(self, attr):
    return getattr(self.stream, attr)


class SvcStatusCallback(object):
  def __init__(self, svc):
    self.svc = svc

  def reportStartPending(self):
    self.svc.ReportServiceStatus(win32service.SERVICE_START_PENDING)

  def reportStarted(self):
    self.svc.ReportServiceStatus(win32service.SERVICE_RUNNING)

  def reportStopPending(self):
    self.svc.ReportServiceStatus(win32service.SERVICE_STOP_PENDING)

  def reportStopped(self):
    self.svc.ReportServiceStatus(win32service.SERVICE_STOPPED)


class WinServiceController:
  @staticmethod
  def Start(serviceName, waitSecs=30):
    err = 0
    try:
      win32serviceutil.StartService(serviceName)
      if waitSecs:
        win32serviceutil.WaitForServiceStatus(serviceName, win32service.SERVICE_RUNNING, waitSecs)
    except win32service.error, exc:
      print "Error starting service: %s" % exc.strerror
      err = exc.winerror
    return err

  @staticmethod
  def Stop(serviceName, waitSecs=30):
    err = 0
    try:
      if waitSecs:
        win32serviceutil.StopServiceWithDeps(serviceName, waitSecs=waitSecs)
      else:
        win32serviceutil.StopService(serviceName)
        if waitSecs:
          win32serviceutil.WaitForServiceStatus(serviceName, win32service.SERVICE_STOPPED, waitSecs)
    except win32service.error, exc:
      print "Error stopping service: %s (%d)" % (exc.strerror, exc.winerror)
      err = exc.winerror
    return err

  @staticmethod
  def QueryStatus(serviceName):
    statusString = SERVICE_STATUS_UNKNOWN

    try:
      status = win32serviceutil.QueryServiceStatus(serviceName)[1]

      if status == win32service.SERVICE_STOPPED:
        statusString = SERVICE_STATUS_STOPPED
      elif status == win32service.SERVICE_START_PENDING:
        statusString = SERVICE_STATUS_STARTING
      elif status == win32service.SERVICE_RUNNING:
        statusString = SERVICE_STATUS_RUNNING
      elif status == win32service.SERVICE_STOP_PENDING:
        statusString = SERVICE_STATUS_STOPPING
    except win32api.error:
      statusString = SERVICE_STATUS_NOT_INSTALLED
      pass

    return statusString

  @staticmethod
  def EnsureServiceIsStarted(serviceName, waitSecs=30):
    err = 0
    try:
      status = win32serviceutil.QueryServiceStatus(serviceName)[1]
      if win32service.SERVICE_RUNNING != status:
        if win32service.SERVICE_START_PENDING != status:
          win32serviceutil.StartService(serviceName)
        if waitSecs:
          win32serviceutil.WaitForServiceStatus(serviceName, win32service.SERVICE_RUNNING, waitSecs)
    except win32service.error, exc:
      err = exc.winerror
    return err


class WinService(win32serviceutil.ServiceFramework):
  # _svc_name_ = The service name
  # _svc_display_name_ = The service display name
  # _svc_description_ = The service description

  _heventSvcStop = win32event.CreateEvent(None, 0, 0, None)
  _hmtxOut = win32event.CreateMutex(None, False, None)  #[fbarca] Python doesn't support critical sections

  def __init__(self, *args):
    win32serviceutil.ServiceFramework.__init__(self, *args)

  def SvcDoRun(self):
    try:
      self.ReportServiceStatus(win32service.SERVICE_RUNNING)
      self.ServiceMain()
    except Exception, x:
      #TODO: Log exception
      self.SvcStop()

  def SvcStop(self):
    self.ReportServiceStatus(win32service.SERVICE_STOP_PENDING)
    win32event.SetEvent(self._heventSvcStop)

  # Service code entry point. Override it to implement the intended functionality.
  def ServiceMain(self):
    #Default implementation, does nothing.
    win32event.WaitForSingleObject(self._heventSvcStop, win32event.INFINITE)
    pass

  def DefCtrlCHandler(self):
    print_info_msg("Ctrl+C handler invoked. Stopping.")
    win32event.SetEvent(self._heventSvcStop)
    pass

  #username domain\\username : The Username the service is to run under
  #password password : The password for the username
  #startup [manual|auto|disabled|delayed] : How the service starts, default = auto
  #interactive : Allow the service to interact with the desktop.
  #perfmonini file: .ini file to use for registering performance monitor data
  #perfmondll file: .dll file to use when querying the service for performance data, default = perfmondata.dll
  @classmethod
  def Install(cls, startupMode = "auto", username = None, password = None, interactive = False,
              perfMonIni = None, perfMonDll = None):
    installArgs = [sys.argv[0], "--startup=" + startupMode]
    if username is not None and username:
      installArgs.append("--username=" + username)
      if password is not None and password:
        installArgs.append("--password=" + password)
    if interactive:
      installArgs.append("--interactive")
    if perfMonIni is not None and perfMonIni:
      installArgs.append("--perfmonini=" + perfMonIni)
    if perfMonDll is not None and perfMonDll:
      installArgs.append("--perfmondll=" + perfMonDll)
    installArgs.append("install")
    win32serviceutil.HandleCommandLine(cls, None, installArgs)

  @classmethod
  def Start(cls, waitSecs = 30):
    return WinServiceController.Start(cls._svc_name_, waitSecs)

  @classmethod
  def Stop(cls, waitSecs = 30):
    return WinServiceController.Stop(cls._svc_name_, waitSecs)

  @classmethod
  def QueryStatus(cls):
    return WinServiceController.QueryStatus(cls._svc_name_)

  @classmethod
  def set_ctrl_c_handler(cls, ctrlHandler):
    win32api.SetConsoleCtrlHandler(ctrlHandler, True)
    pass

  def _RedirectOutputStreamsToFile(self, outFilePath):
    outFileDir = os.path.dirname(outFilePath)
    if not os.path.exists(outFileDir):
      os.makedirs(outFileDir)

    out_writer = SyncStreamWriter(file(outFilePath, "w"), self._hmtxOut)
    sys.stderr = out_writer
    sys.stdout = out_writer
    pass

  def CheckForStop(self):
    #Check for stop event to be signaled
    return win32event.WAIT_OBJECT_0 == win32event.WaitForSingleObject(self._heventSvcStop, 1)

  def _StopOrWaitForChildProcessToFinish(self, childProcess):
    #Wait for the child process to finish or for the stop event to be signaled
    if(win32event.WAIT_OBJECT_0 == win32event.WaitForMultipleObjects([self._heventSvcStop, childProcess._handle], False, win32event.INFINITE)):
      # The OS only detaches the child process when the master process exits.
      # We must kill it manually.
      try:
        #Sending signal.CTRL_BREAK_EVENT doesn't work. It only detaches the child process from the master.
        #  Must brutally terminate the child process. Sorry Java.
        childProcess.terminate()
      except OSError, e:
        print_info_msg("Unable to stop Ambari Server - " + str(e))
        return False

    return True

class SystemWideLock(object):

  def __init__(self, name):
    self._mutex = CreateMutex(None, 0, name)

  def lock(self, timeout=0):
    result = WaitForSingleObject(self._mutex, timeout)
    if result in [WAIT_TIMEOUT, WAIT_ABANDONED, WAIT_FAILED]:
      return False
    elif result == WAIT_OBJECT_0:
      return True

  def unlock(self):
    try:
      ReleaseMutex(self._mutex)
      return True
    except:
      return False

  def __del__(self):
    CloseHandle(self._mutex)

class UserHelper(object):
  ACTION_OK = 0
  USER_EXISTS = 1
  ACTION_FAILED = -1

  def __init__(self):
    self._policy = LsaOpenPolicy(None, POLICY_CREATE_ACCOUNT | POLICY_LOOKUP_NAMES)

  def create_user(self, name, password, comment="Ambari user"):
    user_info = {}
    user_info['name'] = name
    user_info['password'] = password
    user_info['priv'] = USER_PRIV_USER
    user_info['comment'] = comment
    user_info['flags'] = UF_NORMAL_ACCOUNT | UF_SCRIPT
    try:
      NetUserAdd(None, 1, user_info)
    except pywintypes.error as e:
      if e.winerror == 2224:
        return UserHelper.USER_EXISTS, e.strerror
      else:
        return UserHelper.ACTION_FAILED, e.strerror
    return UserHelper.ACTION_OK, "User created."

  def add_user_privilege(self, name, privilege):
    try:
      acc_sid = LookupAccountName(None, name)[0]
      LsaAddAccountRights(self._policy, acc_sid, (privilege,))
    except pywintypes.error as e:
      return UserHelper.ACTION_FAILED, e.strerror
    return UserHelper.ACTION_OK, "Privilege added."

  def remove_user_privilege(self, name, privilege):
    try:
      acc_sid = LookupAccountName(None, name)[0]
      LsaRemoveAccountRights(self._policy, acc_sid, 0, (privilege,))
    except pywintypes.error as e:
      return UserHelper.ACTION_FAILED, e.strerror
    return UserHelper.ACTION_OK, "Privilege removed."
