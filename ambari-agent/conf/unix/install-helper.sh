# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information rega4rding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


##################################################################
#                      AGENT INSTALL HELPER                      #
##################################################################

COMMON_DIR="/usr/lib/python2.6/site-packages/ambari_commons"
RESOURCE_MANAGEMENT_DIR="/usr/lib/python2.6/site-packages/resource_management"
JINJA_DIR="/usr/lib/python2.6/site-packages/ambari_jinja2"
OLD_COMMON_DIR="/usr/lib/python2.6/site-packages/common_functions"
INSTALL_HELPER_SERVER="/var/lib/ambari-server/install-helper.sh"
COMMON_DIR_AGENT="/usr/lib/ambari-agent/lib/ambari_commons"
RESOURCE_MANAGEMENT_DIR_AGENT="/usr/lib/ambari-agent/lib/resource_management"
JINJA_AGENT_DIR="/usr/lib/ambari-agent/lib/ambari_jinja2"

PYTHON_WRAPER_TARGET="/usr/bin/ambari-python-wrap"
PYTHON_WRAPER_SOURCE="/var/lib/ambari-agent/ambari-python-wrap"

do_install(){
  # setting ambari_commons shared resource
  rm -rf "$OLD_COMMON_DIR"
  if [ ! -d "$COMMON_DIR" ]; then
    ln -s "$COMMON_DIR_AGENT" "$COMMON_DIR"
  fi
  # setting resource_management shared resource
  if [ ! -d "$RESOURCE_MANAGEMENT_DIR" ]; then
    ln -s "$RESOURCE_MANAGEMENT_DIR_AGENT" "$RESOURCE_MANAGEMENT_DIR"
  fi
  # setting jinja2 shared resource
  if [ ! -d "$JINJA_DIR" ]; then
    ln -s "$JINJA_AGENT_DIR" "$JINJA_DIR"
  fi
  # setting python-wrapper script
  if [ ! -f "$PYTHON_WRAPER_TARGET" ]; then
    ln -s "$PYTHON_WRAPER_SOURCE" "$PYTHON_WRAPER_TARGET"
  fi
}

do_remove(){

  if [ -f "$PYTHON_WRAPER_TARGET" ]; then
    rm -f "$PYTHON_WRAPER_TARGET"
  fi

  # if server package exists, restore their settings
  if [ -f "$INSTALL_HELPER_SERVER" ]; then  #  call server shared files installer
    $INSTALL_HELPER_SERVER install
  fi
}

do_upgrade(){
  do_install
}


case "$1" in
install)
  do_install
  ;;
remove)
  do_remove
  ;;
upgrade)
  do_upgrade
;;
esac
