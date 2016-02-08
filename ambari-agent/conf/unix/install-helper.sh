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
SIMPLEJSON_DIR="/usr/lib/python2.6/site-packages/ambari_simplejson"
OLD_COMMON_DIR="/usr/lib/python2.6/site-packages/common_functions"
INSTALL_HELPER_SERVER="/var/lib/ambari-server/install-helper.sh"
COMMON_DIR_AGENT="/usr/lib/ambari-agent/lib/ambari_commons"
RESOURCE_MANAGEMENT_DIR_AGENT="/usr/lib/ambari-agent/lib/resource_management"
JINJA_AGENT_DIR="/usr/lib/ambari-agent/lib/ambari_jinja2"
SIMPLEJSON_AGENT_DIR="/usr/lib/ambari-agent/lib/ambari_simplejson"

PYTHON_WRAPER_TARGET="/usr/bin/ambari-python-wrap"

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
  # setting simplejson shared resource
  if [ ! -d "$SIMPLEJSON_DIR" ]; then
    ln -s "$SIMPLEJSON_AGENT_DIR" "$SIMPLEJSON_DIR"
  fi
  
  # on nano Ubuntu, when umask=027 those folders are created without 'x' bit for 'others'.
  # which causes failures when hadoop users try to access tmp_dir
  chmod a+x /var/lib/ambari-agent
  
  chmod 777 /var/lib/ambari-agent/tmp
  chmod 700 /var/lib/ambari-agent/data

  # remove old python wrapper
  rm -f "$PYTHON_WRAPER_TARGET"

  AMBARI_PYTHON=""
  python_binaries=( "/usr/bin/python" "/usr/bin/python2" "/usr/bin/python2.7", "/usr/bin/python2.6" )
  for python_binary in "${python_binaries[@]}"
  do
    $python_binary -c "import sys ; ver = sys.version_info ; sys.exit(not (ver >= (2,6) and ver<(3,0)))" 1>/dev/null 2>/dev/null

    if [ $? -eq 0 ] ; then
      AMBARI_PYTHON="$python_binary"
      break;
    fi
  done

  if [ -z "$AMBARI_PYTHON" ] ; then
    >&2 echo "Cannot detect python for ambari to use. Please manually set $PYTHON_WRAPER link to point to correct python binary"
  else
    ln -s "$AMBARI_PYTHON" "$PYTHON_WRAPER_TARGET"
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
