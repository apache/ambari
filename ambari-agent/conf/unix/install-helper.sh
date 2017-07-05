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
AMBARI_AGENT="/usr/lib/python2.6/site-packages/ambari_agent"
PYTHON_WRAPER_TARGET="/usr/bin/ambari-python-wrap"
AMBARI_AGENT_VAR="/var/lib/ambari-agent"
AMBARI_AGENT_BINARY="/etc/init.d/ambari-agent"
AMBARI_AGENT_BINARY_SYMLINK="/usr/sbin/ambari-agent"

clean_pyc_files(){
  # cleaning old *.pyc files
  find ${RESOURCE_MANAGEMENT_DIR:?} -name *.pyc -exec rm {} \;
  find ${COMMON_DIR:?} -name *.pyc -exec rm {} \;
  find ${AMBARI_AGENT:?} -name *.pyc -exec rm {} \;
  find ${AMBARI_AGENT_VAR:?} -name *.pyc -exec rm {} \;
}


do_install(){
  if [ -d "/etc/ambari-agent/conf.save" ]; then
    cp -f /etc/ambari-agent/conf.save/* /etc/ambari-agent/conf
    mv /etc/ambari-agent/conf.save /etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save
  fi

  # setting up /usr/sbin/ambari-agent symlink
  rm -f "$AMBARI_AGENT_BINARY_SYMLINK"
  ln -s "$AMBARI_AGENT_BINARY" "$AMBARI_AGENT_BINARY_SYMLINK"
    
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
  chmod a+x $AMBARI_AGENT_VAR
  
  chmod 1777 $AMBARI_AGENT_VAR/tmp
  chmod 700 $AMBARI_AGENT_VAR/keys
  chmod 700 $AMBARI_AGENT_VAR/data

  #TODO we need this when upgrading from pre 2.4 versions to 2.4, remove this when upgrade from pre 2.4 versions will be
  #TODO unsupported
  clean_pyc_files

  which chkconfig > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    chkconfig --add ambari-agent
  fi
  which update-rc.d > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    update-rc.d ambari-agent defaults
  fi

  # remove old python wrapper
  rm -f "$PYTHON_WRAPER_TARGET"

  AMBARI_PYTHON=""
  python_binaries=( "/usr/bin/python" "/usr/bin/python2" "/usr/bin/python2.7" "/usr/bin/python2.6" )
  for python_binary in "${python_binaries[@]}"
  do
    $python_binary -c "import sys ; ver = sys.version_info ; sys.exit(not (ver >= (2,6) and ver<(3,0)))" 1>/dev/null 2>/dev/null

    if [ $? -eq 0 ] ; then
      AMBARI_PYTHON="$python_binary"
      break;
    fi
  done

  BAK=/etc/ambari-agent/conf/ambari-agent.ini.old
  ORIG=/etc/ambari-agent/conf/ambari-agent.ini
  UPGRADE_AGENT_CONFIGS_SCRIPT=/var/lib/ambari-agent/upgrade_agent_configs.py

  if [ -z "$AMBARI_PYTHON" ] ; then
    >&2 echo "Cannot detect python for Ambari to use. Please manually set $PYTHON_WRAPER_TARGET link to point to correct python binary"
    >&2 echo "Cannot upgrade agent configs because python for Ambari is not configured. The old config file is saved as $BAK . Execution of $UPGRADE_AGENT_CONFIGS_SCRIPT was skipped."
  else
    ln -s "$AMBARI_PYTHON" "$PYTHON_WRAPER_TARGET"

    if [ -f $BAK ]; then
      if [ -f "$UPGRADE_AGENT_CONFIGS_SCRIPT" ]; then
        $UPGRADE_AGENT_CONFIGS_SCRIPT
      fi
      mv $BAK ${BAK}_$(date '+%d_%m_%y_%H_%M').save
    fi
  fi
}

do_remove(){
  /usr/sbin/ambari-agent stop > /dev/null 2>&1

  clean_pyc_files

  rm -f "$AMBARI_AGENT_BINARY_SYMLINK"

  if [ -d "/etc/ambari-agent/conf.save" ]; then
    mv /etc/ambari-agent/conf.save /etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save
  fi
  mv /etc/ambari-agent/conf /etc/ambari-agent/conf.save
    
  if [ -f "$PYTHON_WRAPER_TARGET" ]; then
    rm -f "$PYTHON_WRAPER_TARGET"
  fi
  
  if [ -d "$COMMON_DIR" ]; then
    rm -f $COMMON_DIR
  fi
  
  if [ -d "$RESOURCE_MANAGEMENT_DIR" ]; then
    rm -rf $RESOURCE_MANAGEMENT_DIR
  fi
  
  if [ -d "$JINJA_DIR" ]; then
    rm -rf $JINJA_DIR
  fi

  if [ -d "$SIMPLEJSON_DIR" ]; then
    rm -f $SIMPLEJSON_DIR
  fi

  if [ -d "$OLD_COMMON_DIR" ]; then
    rm -f $OLD_COMMON_DIR
  fi

  # if server package exists, restore their settings
  if [ -f "$INSTALL_HELPER_SERVER" ]; then  #  call server shared files installer
    $INSTALL_HELPER_SERVER install
  fi

  which chkconfig > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    chkconfig --list | grep ambari-server && chkconfig --del ambari-agent
  fi
  which update-rc.d > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    update-rc.d -f ambari-agent remove
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
