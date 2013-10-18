import os
import subprocess

from resource_management.base import Fail
from resource_management.providers import Provider


class ServiceProvider(Provider):
  def action_start(self):
    if not self.status():
      self._exec_cmd("start", 0)
      self.resource.updated()

  def action_stop(self):
    if self.status():
      self._exec_cmd("stop", 0)
      self.resource.updated()

  def action_restart(self):
    if not self.status():
      self._exec_cmd("start", 0)
      self.resource.updated()
    else:
      self._exec_cmd("restart", 0)
      self.resource.updated()

  def action_reload(self):
    if not self.status():
      self._exec_cmd("start", 0)
      self.resource.updated()
    else:
      self._exec_cmd("reload", 0)
      self.resource.updated()

  def status(self):
    return self._exec_cmd("status") == 0

  def _exec_cmd(self, command, expect=None):
    if command != "status":
      self.log.info("%s command '%s'" % (self.resource, command))

    custom_cmd = getattr(self.resource, "%s_command" % command, None)
    if custom_cmd:
      self.log.debug("%s executing '%s'" % (self.resource, custom_cmd))
      if hasattr(custom_cmd, "__call__"):
        if custom_cmd():
          ret = 0
        else:
          ret = 1
      else:
        ret = subprocess.call(custom_cmd, shell=True,
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    else:
      ret = self._init_cmd(command)

    if expect is not None and expect != ret:
      raise Fail("%r command %s for service %s failed" % (
      self, command, self.resource.service_name))
    return ret

  def _init_cmd(self, command):
    if self._upstart:
      if command == "status":
        proc = subprocess.Popen(
          ["/sbin/" + command, self.resource.service_name],
          stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        out = proc.communicate()[0]
        _proc, state = out.strip().split(' ', 1)
        ret = 0 if state != "stop/waiting" else 1
      else:
        ret = subprocess.call(["/sbin/" + command, self.resource.service_name],
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    else:
      ret = subprocess.call(
        ["/etc/init.d/%s" % self.resource.service_name, command],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return ret

  @property
  def _upstart(self):
    try:
      return self.__upstart
    except AttributeError:
      self.__upstart = os.path.exists("/sbin/start") \
        and os.path.exists("/etc/init/%s.conf" % self.resource.service_name)
    return self.__upstart
