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


from unittest import TestCase
from mock.mock import patch, MagicMock

from resource_management.core import Environment
from resource_management.core.system import System
from resource_management.core.source import StaticFile
from resource_management.core.source import DownloadSource
from resource_management.core.source import Template
from resource_management.core.source import InlineTemplate

from ambari_jinja2 import UndefinedError, TemplateNotFound
import urllib2
import os


@patch.object(System, "os_family", new = 'redhat')
class TestContentSources(TestCase):

  @patch("__builtin__.open")
  @patch.object(os.path, "join")
  def test_static_file_absolute_path(self, join_mock, open_mock):
    """
    Testing StaticFile source with absolute path
    """
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'content'
    open_mock.return_value = file_mock

    with Environment("/base") as env:
      static_file = StaticFile("/absolute/path/file")
      content = static_file.get_content()

    self.assertEqual('content', content)
    self.assertEqual(file_mock.read.call_count, 1)
    self.assertEqual(join_mock.call_count, 0)


  @patch("__builtin__.open")
  @patch.object(os.path, "join")
  def test_static_file_relative_path(self, join_mock, open_mock):
    """
    Testing StaticFile source with relative path
    """
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'content'
    open_mock.return_value = file_mock

    with Environment("/base") as env:
      static_file = StaticFile("relative/path/file")
      content = static_file.get_content()

    self.assertEqual('content', content)
    self.assertEqual(file_mock.read.call_count, 1)
    self.assertEqual(join_mock.call_count, 1)
    join_mock.assert_called_with('/base', 'files', 'relative/path/file')
    self.assertEqual(open_mock.call_count, 1)

  @patch.object(os, "makedirs")
  @patch.object(os.path, "exists")
  def test_download_source_init_existent_download_directory(self, exists_mock, makedirs_mock):
    """
    Testing DownloadSource without cache with existent download directory
    """
    exists_mock.return_value = True

    with Environment("/base") as env:
      static_file = DownloadSource("http://download/source")

    self.assertEqual(makedirs_mock.call_count, 0)
    self.assertEqual(exists_mock.call_count, 1)
    pass


  @patch.object(os, "makedirs")
  @patch.object(os.path, "exists")
  def test_download_source_init_nonexistent_download_directory(self, exists_mock, makedirs_mock):
    """
    Testing DownloadSource without cache with non-existent download directory
    """
    exists_mock.return_value = False

    with Environment("/base") as env:
      static_file = DownloadSource("http://download/source")

    self.assertEqual(makedirs_mock.call_count, 1)
    makedirs_mock.assert_called_with("/var/tmp/downloads")
    self.assertEqual(exists_mock.call_count, 1)
    pass

  @patch.object(urllib2, "urlopen")
  @patch.object(os, "makedirs")
  @patch.object(os.path, "exists")
  def test_download_source_get_content_nocache(self, exists_mock, makedirs_mock, urlopen_mock):
    """
    Testing DownloadSource.get_content without cache
    """
    exists_mock.return_value = True
    web_file_mock = MagicMock()
    web_file_mock.read.return_value = 'web_content'
    urlopen_mock.return_value = web_file_mock

    with Environment("/base") as env:
      download_source = DownloadSource("http://download/source", cache=False)
    content = download_source.get_content()

    self.assertEqual('web_content', content)
    self.assertEqual(urlopen_mock.call_count, 1)
    urlopen_mock.assert_called_with('http://download/source')
    self.assertEqual(web_file_mock.read.call_count, 1)

  @patch.object(urllib2, "urlopen")
  @patch.object(os, "makedirs")
  @patch.object(os.path, "exists")
  def test_download_source_get_content_cache_new(self, exists_mock, makedirs_mock, urlopen_mock):
    """
    Testing DownloadSource.get_content with cache on non-cached resource
    """
    exists_mock.side_effect = [True, False]
    web_file_mock = MagicMock()
    web_file_mock.read.return_value = 'web_content'
    urlopen_mock.return_value = web_file_mock

    with Environment("/base") as env:
      download_source = DownloadSource("http://download/source", cache=True)
    content = download_source.get_content()

    self.assertEqual('web_content', content)
    self.assertEqual(urlopen_mock.call_count, 1)
    urlopen_mock.assert_called_with('http://download/source')
    self.assertEqual(web_file_mock.read.call_count, 1)

  @patch("__builtin__.open")
  @patch.object(os, "makedirs")
  @patch.object(os.path, "exists")
  def test_download_source_get_content_cache_existent(self, exists_mock, makedirs_mock, open_mock):
    """
    Testing DownloadSource.get_content with cache on cached resource
    """
    exists_mock.side_effect = [True, True, False]

    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'cached_content'
    open_mock.return_value = file_mock


    with Environment("/base") as env:
      download_source = DownloadSource("http://download/source", cache=True)
    content = download_source.get_content()

    self.assertEqual('cached_content', content)
    self.assertEqual(open_mock.call_count, 1)
    open_mock.assert_called_with('/var/tmp/downloads/source')
    self.assertEqual(file_mock.read.call_count, 1)

  @patch.object(urllib2, "urlopen")
  @patch("__builtin__.open")
  @patch.object(os, "makedirs")
  @patch.object(os.path, "exists")
  def test_download_source_get_content_cache_existent_md5_match(self, exists_mock, makedirs_mock, open_mock,
                                                                urlopen_mock):
    """
    Testing DownloadSource.get_content with cache on cached resource with md5 check
    """
    exists_mock.side_effect = [True, True, False]

    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'cached_content'
    open_mock.return_value = file_mock


    with Environment("/base") as env:
      download_source = DownloadSource("http://download/source", cache=True)
    content = download_source.get_content()

    self.assertEqual('cached_content', content)
    self.assertEqual(open_mock.call_count, 1)
    open_mock.assert_called_with('/var/tmp/downloads/source')
    self.assertEqual(file_mock.read.call_count, 1)
    self.assertEqual(urlopen_mock.call_count, 0)

  @patch.object(urllib2, "urlopen")
  @patch("__builtin__.open")
  @patch.object(os, "makedirs")
  @patch.object(os.path, "exists")
  def test_download_source_get_content_cache_existent_md5_unmatch(self, exists_mock, makedirs_mock, open_mock,
                                                                  urlopen_mock):
    """
    Testing DownloadSource.get_content with cache on cached resource with md5 check
    """
    exists_mock.side_effect = [True, True, False]
    fake_md5 = "144c9defac04969c7bfad8efaa8ea194"
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'cached_content'
    open_mock.return_value = file_mock
    web_file_mock = MagicMock()
    web_file_mock.read.return_value = 'web_content'
    urlopen_mock.return_value = web_file_mock

    with Environment("/base") as env:
      download_source = DownloadSource("http://download/source", cache=True, md5sum=fake_md5)
    content = download_source.get_content()

    self.assertEqual('web_content', content)
    self.assertEqual(open_mock.call_count, 2)
    open_mock.assert_once_called('/var/tmp/downloads/source', 'w')
    open_mock.assert_once_called('/var/tmp/downloads/source')
    self.assertEqual(file_mock.read.call_count, 1)
    self.assertEqual(urlopen_mock.call_count, 1)
    urlopen_mock.assert_called_with('http://download/source')

  @patch("__builtin__.open")
  @patch.object(os.path, "getmtime")
  @patch.object(os.path, "exists")
  def test_template_loader(self, exists_mock, getmtime_mock, open_mock):
    """
    Testing template loader on existent file
    """
    exists_mock.return_value = True
    getmtime_mock.return_value = 10
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'template content'
    open_mock.return_value = file_mock

    with Environment("/base") as env:
      template = Template("test.j2")

    self.assertEqual(open_mock.call_count, 1)
    open_mock.assert_called_with('/base/templates/test.j2', 'rb')
    self.assertEqual(getmtime_mock.call_count, 1)
    getmtime_mock.assert_called_with('/base/templates/test.j2')

  @patch.object(os.path, "exists")
  def test_template_loader_fail(self, exists_mock):
    """
    Testing template loader on non-existent file
    """
    exists_mock.return_value = False

    try:
      with Environment("/base") as env:
        template = Template("test.j2")
      self.fail("Template should fail with nonexistent template file")
    except TemplateNotFound:
      pass



  @patch("__builtin__.open")
  @patch.object(os.path, "getmtime")
  @patch.object(os.path, "exists")
  def test_template_loader_absolute_path(self, exists_mock, getmtime_mock, open_mock):
    """
    Testing template loader with absolute file-path
    """
    exists_mock.return_value = True
    getmtime_mock.return_value = 10
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'template content'
    open_mock.return_value = file_mock

    with Environment("/base") as env:
      template = Template("/absolute/path/test.j2")

    self.assertEqual(open_mock.call_count, 1)
    open_mock.assert_called_with('/absolute/path/test.j2', 'rb')
    self.assertEqual(getmtime_mock.call_count, 1)
    getmtime_mock.assert_called_with('/absolute/path/test.j2')

  @patch("__builtin__.open")
  @patch.object(os.path, "getmtime")
  @patch.object(os.path, "exists")
  def test_template_loader_arguments(self, exists_mock, getmtime_mock, open_mock):
    """
    Testing template loader additional arguments in template and absolute file-path
    """
    exists_mock.return_value = True
    getmtime_mock.return_value = 10
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = '{{test_arg1}} template content'
    open_mock.return_value = file_mock

    with Environment("/base") as env:
      template = Template("/absolute/path/test.j2", [], test_arg1 = "test")
      content = template.get_content()
    self.assertEqual(open_mock.call_count, 1)

    self.assertEqual(u'test template content\n', content)
    open_mock.assert_called_with('/absolute/path/test.j2', 'rb')
    self.assertEqual(getmtime_mock.call_count, 1)
    getmtime_mock.assert_called_with('/absolute/path/test.j2')

  def test_inline_template(self):
    """
    Testing InlineTemplate
    """
    with Environment("/base") as env:
      template = InlineTemplate("{{test_arg1}} template content", [], test_arg1 = "test")
      content = template.get_content()

    self.assertEqual(u'test template content\n', content)

  def test_template_imports(self):
    """
    Testing Template additional imports
    """
    try:
      with Environment("/base") as env:
        template = InlineTemplate("{{test_arg1}} template content {{os.path.join(path[0],path[1])}}", [], test_arg1 = "test", path = ["/one","two"])
        content = template.get_content()
        self.fail("Template.get_content should fail when evaluating unknown import")
    except UndefinedError:
      pass
    with Environment("/base") as env:
      template = InlineTemplate("{{test_arg1}} template content {{os.path.join(path[0],path[1])}}", [os], test_arg1 = "test", path = ["/one","two"])
      content = template.get_content()
    self.assertEqual(u'test template content /one/two\n', content)
