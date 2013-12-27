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

Ambari Agent

"""

from __future__ import with_statement
from resource_management.core.environment import Environment
from resource_management.core.utils import checked_unite

__all__ = ["Source", "Template", "InlineTemplate", "StaticFile", "DownloadSource"]

import hashlib
import os
import urllib2
import urlparse


class Source(object):
  def __init__(self, name):
    self.env = Environment.get_instance()
    self.name = name
    
  def get_content(self):
    raise NotImplementedError()

  def get_checksum(self):
    return None

  def __call__(self):
    return self.get_content()


class StaticFile(Source):
  def __init__(self, name):
    super(StaticFile, self).__init__(name)

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
  from jinja2 import Environment as JinjaEnvironment, BaseLoader, TemplateNotFound, FunctionLoader, StrictUndefined
except ImportError:
  class Template(Source):
    def __init__(self, name, variables=None, env=None):
      raise Exception("Jinja2 required for Template/InlineTemplate")
    
  class InlineTemplate(Source):
    def __init__(self, name, variables=None, env=None):
      raise Exception("Jinja2 required for Template/InlineTemplate")
else:
  class TemplateLoader(BaseLoader):
    def __init__(self, env=None):
      self.env = env or Environment.get_instance()

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
    def __init__(self, name, extra_imports=[], **kwargs):
      """
      @param kwargs: Additional variables passed to template
      """
      super(Template, self).__init__(name)
      params = self.env.config.params
      variables = checked_unite(params, kwargs)
      self.imports_dict = dict((module.__name__, module) for module in extra_imports)
      self.context = variables.copy() if variables else {}
      if not hasattr(self, 'template_env'):
        self.template_env = JinjaEnvironment(loader=TemplateLoader(self.env),
                                        autoescape=False, undefined=StrictUndefined, trim_blocks=True)
        
      self.template = self.template_env.get_template(self.name)     
    
    def get_content(self):
      default_variables = { 'env':self.env, 'repr':repr, 'str':str, 'bool':bool }
      variables = checked_unite(default_variables, self.imports_dict)
      self.context.update(variables)
      
      rendered = self.template.render(self.context)
      return rendered + "\n" if not rendered.endswith('\n') else rendered
    
  class InlineTemplate(Template):
    def __init__(self, name, extra_imports=[], **kwargs):
      self.template_env = JinjaEnvironment(loader=FunctionLoader(lambda text: text))
      super(InlineTemplate, self).__init__(name, extra_imports, **kwargs) 


class DownloadSource(Source):
  def __init__(self, name, cache=True, md5sum=None):
    super(DownloadSource, self).__init__(name)
    self.url = self.name
    self.md5sum = md5sum
    self.cache = cache
    if not 'download_path' in self.env.config:
      self.env.config.download_path = '/var/tmp/downloads'
    if not os.path.exists(self.env.config.download_path):
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
