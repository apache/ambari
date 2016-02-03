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

#########################################postinstall.sh#########################
#                      SERVER INSTALL HELPER                     #
##################################################################

COMMON_DIR="/usr/lib/python2.6/site-packages/ambari_commons"
RESOURCE_MANAGEMENT_DIR="/usr/lib/python2.6/site-packages/resource_management"
JINJA_DIR="/usr/lib/python2.6/site-packages/ambari_jinja2"
SIMPLEJSON_DIR="/usr/lib/python2.6/site-packages/ambari_simplejson"
OLD_COMMON_DIR="/usr/lib/python2.6/site-packages/common_functions"
AMBARI_SERVER="/usr/lib/python2.6/site-packages/ambari_server"
INSTALL_HELPER_AGENT="/var/lib/ambari-agent/install-helper.sh"
COMMON_DIR_SERVER="/usr/lib/ambari-server/lib/ambari_commons"
RESOURCE_MANAGEMENT_DIR_SERVER="/usr/lib/ambari-server/lib/resource_management"
JINJA_SERVER_DIR="/usr/lib/ambari-server/lib/ambari_jinja2"
SIMPLEJSON_SERVER_DIR="/usr/lib/ambari-server/lib/ambari_simplejson"

PYTHON_WRAPER_TARGET="/usr/bin/ambari-python-wrap"
PYTHON_WRAPER_SOURCE="/var/lib/ambari-server/ambari-python-wrap"

do_install(){
  rm -f /usr/sbin/ambari-server
  ln -s /etc/init.d/ambari-server /usr/sbin/ambari-server
 
  # setting ambari_commons shared resource
  rm -rf "$OLD_COMMON_DIR"
  if [ ! -d "$COMMON_DIR" ]; then
    ln -s "$COMMON_DIR_SERVER" "$COMMON_DIR"
  fi
  # setting resource_management shared resource
  if [ ! -d "$RESOURCE_MANAGEMENT_DIR" ]; then
    ln -s "$RESOURCE_MANAGEMENT_DIR_SERVER" "$RESOURCE_MANAGEMENT_DIR"
  fi
  # setting jinja2 shared resource
  if [ ! -d "$JINJA_DIR" ]; then
    ln -s "$JINJA_SERVER_DIR" "$JINJA_DIR"
  fi
  # setting simplejson shared resource
  if [ ! -d "$SIMPLEJSON_DIR" ]; then
    ln -s "$SIMPLEJSON_SERVER_DIR" "$SIMPLEJSON_DIR"
  fi
  # setting python-wrapper script
  if [ ! -f "$PYTHON_WRAPER_TARGET" ]; then
    ln -s "$PYTHON_WRAPER_SOURCE" "$PYTHON_WRAPER_TARGET"
  fi

  which chkconfig > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    chkconfig --add ambari-server
  fi
  which update-rc.d > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    update-rc.d ambari-server defaults
  fi
}

do_remove(){
  /usr/sbin/ambari-server stop > /dev/null 2>&1
  if [ -d "/etc/ambari-server/conf.save" ]; then
      mv /etc/ambari-server/conf.save /etc/ambari-server/conf_$(date '+%d_%m_%y_%H_%M').save
  fi
  # Remove link created during install
  rm -f /usr/sbin/ambari-server

  mv /etc/ambari-server/conf /etc/ambari-server/conf.save
    
  if [ -f "$PYTHON_WRAPER_TARGET" ]; then
    rm -f "$PYTHON_WRAPER_TARGET"
  fi

  if [ -d "$COMMON_DIR" ]; then
    rm -f $COMMON_DIR
  fi

  if [ -d "$RESOURCE_MANAGEMENT_DIR" ]; then
    rm -f $RESOURCE_MANAGEMENT_DIR
  fi

  if [ -d "$JINJA_DIR" ]; then
    rm -f $JINJA_DIR
  fi

  if [ -d "$SIMPLEJSON_DIR" ]; then
    rm -f $SIMPLEJSON_DIR
  fi

  if [ -d "$OLD_COMMON_DIR" ]; then
    rm -rf $OLD_COMMON_DIR
  fi

  if [ -d "$AMBARI_SERVER" ]; then
    rm -rf "$AMBARI_SERVER"
  fi

  # if server package exists, restore their settings
  if [ -f "$INSTALL_HELPER_AGENT" ]; then  #  call agent shared files installer
    $INSTALL_HELPER_AGENT install
  fi

  which chkconfig > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    chkconfig --list | grep ambari-server && chkconfig --del ambari-server
  fi
  which update-rc.d > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    update-rc.d -f ambari-server remove
  fi
}

do_upgrade(){
  # this function only gets called for rpm. Deb packages always call do_install directly.
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
