from resource_management import *
from resource_management.core.system import System


class MonitorWebserverProvider(Provider):
  def action_start(self):
    self.get_serivice_params()
    self.enable_keep_alive()
    service_name = self.service_name
    Execute(format("/etc/init.d/{service_name} start"))

  def action_stop(self):
    self.get_serivice_params()
    service_name = self.service_name
    Execute(format("/etc/init.d/{service_name} stop"))

  def action_restart(self):
    self.action_stop()
    self.action_start()

  def get_serivice_params(self):
    self.system = System.get_instance()
    if self.system.platform == "suse":
      self.service_name = "apache2"
      self.httpd_conf_dir = '/etc/apache2'
    else:
      self.service_name = "httpd"
      self.httpd_conf_dir = '/etc/httpd/conf'

  def enable_keep_alive(self):
    httpd_conf_dir = self.httpd_conf_dir
    Execute(format(
      "grep -E 'KeepAlive (On|Off)' {httpd_conf_dir}/httpd.conf && sed -i 's/KeepAlive Off/KeepAlive On/' {httpd_conf_dir}/httpd.conf || echo 'KeepAlive On' >> {httpd_conf_dir}/httpd.conf"))
