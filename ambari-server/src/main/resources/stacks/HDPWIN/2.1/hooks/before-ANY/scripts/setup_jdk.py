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

import os
import urllib2

from ambari_commons.inet_utils import download_file
from resource_management import *


_install_cmd = '{0} /s INSTALLDIR={1} ADDLOCAL="ToolsFeature,SourceFeature"'


def _check_installed():
  import params
  return os.path.exists(os.path.join(params.java_home, 'bin', 'java.exe'))


def setup_jdk():
  import params
  if not params.jdk_name:
    return
  if _check_installed():
    return

  if not os.path.exists(params.java_home):
    os.makedirs(params.java_home)
  jdk_setup_savepath = os.path.join(params.java_home, params.jdk_name)
  jdk_download_url = "{0}/{1}".format(params.jdk_location, params.jdk_name)
  download_file(jdk_download_url, jdk_setup_savepath)
  Execute(_install_cmd.format(jdk_setup_savepath, params.java_home))
  if not _check_installed():
    raise Fail("Error when installing jdk")