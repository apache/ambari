from __future__ import with_statement
from resource_management.core import environment

__all__ = ["Source", "Template", "StaticFile", "DownloadSource"]

import hashlib
import os
import urllib2
import urlparse


class Source(object):
  def get_content(self):
    raise NotImplementedError()

  def get_checksum(self):
    return None

  def __call__(self):
    return self.get_content()


class StaticFile(Source):
  def __init__(self, name, env=None):
    self.name = name
    self.env = env or environment.Environment.get_instance()

  def get_content(self):
    # absolute path
    if self.name.startswith(os.path.sep):
      path = self.name
    # relative path
    else:
      basedir = self.env.config.basedir
      path = os.path.join(basedir, "files", self.name)
      
    with open(path, "rb") as fp:
      return fp.read()


try:
  from jinja2 import Environment, BaseLoader, TemplateNotFound
except ImportError:
  class Template(Source):
    def __init__(self, name, variables=None, env=None):
      raise Exception("Jinja2 required for Template")
else:
  class TemplateLoader(BaseLoader):
    def __init__(self, env=None):
      self.env = env or environment.Environment.get_instance()

    def get_source(self, environment, template_name):
      # absolute path
      if template_name.startswith(os.path.sep):
        path = template_name
      # relative path
      else:
        basedir = self.env.config.basedir
        path = os.path.join(basedir, "templates", template_name)
      
      if not os.path.exists(path):
        raise TemplateNotFound("%s at %s" % (template_name, path))
      mtime = os.path.getmtime(path)
      with open(path, "rb") as fp:
        source = fp.read().decode('utf-8')
      return source, path, lambda: mtime == os.path.getmtime(path)

  class Template(Source):
    def __init__(self, name, variables=None, env=None):
      self.name = name
      self.env = env or environment.Environment.get_instance()
      params = self.env.config.params
      variables = params if params else variables
      self.context = variables.copy() if variables else {}
      self.template_env = Environment(loader=TemplateLoader(self.env),
                                      autoescape=False)
      self.template = self.template_env.get_template(self.name)

    def get_content(self):
      self.context.update(
        env=self.env,
        repr=repr,
        str=str,
        bool=bool,
      )
      rendered = self.template.render(self.context)
      return rendered + "\n" if not rendered.endswith('\n') else rendered


class DownloadSource(Source):
  def __init__(self, url, cache=True, md5sum=None, env=None):
    self.env = env or environment.Environment.get_instance()
    self.url = url
    self.md5sum = md5sum
    self.cache = cache
    if not 'download_path' in env.config:
      env.config.download_path = '/var/tmp/downloads'
    if not os.path.exists(env.config.download_path):
      os.makedirs(self.env.config.download_path)

  def get_content(self):
    filepath = os.path.basename(urlparse.urlparse(self.url).path)
    content = None
    if not self.cache or not os.path.exists(
      os.path.join(self.env.config.download_path, filepath)):
      web_file = urllib2.urlopen(self.url)
      content = web_file.read()
    else:
      update = False
      with open(os.path.join(self.env.config.download_path, filepath)) as fp:
        content = fp.read()
      if self.md5sum:
        m = hashlib.md5(content)
        md5 = m.hexdigest()
        if md5 != self.md5sum:
          web_file = urllib2.urlopen(self.url)
          content = web_file.read()
          update = True
      if self.cache and update:
        with open(os.path.join(self.env.config.download_path, filepath),
                  'w') as fp:
          fp.write(content)
    return content
