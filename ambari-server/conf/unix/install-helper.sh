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
ROOT_DIR_PATH="${RPM_INSTALL_PREFIX}"
ROOT=`echo "${RPM_INSTALL_PREFIX}" | sed 's|/$||g'` # Customized folder, which ambari-server files are installed into ('/' or '' are default).

COMMON_DIR="${ROOT}/usr/lib/python2.6/site-packages/ambari_commons"
RESOURCE_MANAGEMENT_DIR="${ROOT}/usr/lib/python2.6/site-packages/resource_management"
JINJA_DIR="${ROOT}/usr/lib/python2.6/site-packages/ambari_jinja2"
SIMPLEJSON_DIR="${ROOT}/usr/lib/python2.6/site-packages/ambari_simplejson"
OLD_COMMON_DIR="${ROOT}/usr/lib/python2.6/site-packages/common_functions"
AMBARI_SERVER="${ROOT}/usr/lib/python2.6/site-packages/ambari_server"
INSTALL_HELPER_AGENT="/var/lib/ambari-agent/install-helper.sh"
CA_CONFIG="${ROOT}/var/lib/ambari-server/keys/ca.config"
COMMON_DIR_SERVER="${ROOT}/usr/lib/ambari-server/lib/ambari_commons"
RESOURCE_MANAGEMENT_DIR_SERVER="${ROOT}/usr/lib/ambari-server/lib/resource_management"
JINJA_SERVER_DIR="${ROOT}/usr/lib/ambari-server/lib/ambari_jinja2"
SIMPLEJSON_SERVER_DIR="${ROOT}/usr/lib/ambari-server/lib/ambari_simplejson"
AMBARI_PROPERTIES="${ROOT}/etc/ambari-server/conf/ambari.properties"
AMBARI_ENV_RPMSAVE="${ROOT}/var/lib/ambari-server/ambari-env.sh.rpmsave" # this turns into ambari-env.sh during ambari-server start

PYTHON_WRAPER_DIR="${ROOT}/usr/bin/"
PYTHON_WRAPER_TARGET="${PYTHON_WRAPER_DIR}/ambari-python-wrap"

AMBARI_SERVER_EXECUTABLE_LINK="${ROOT}/usr/sbin/ambari-server"
AMBARI_SERVER_EXECUTABLE="${ROOT}/etc/init.d/ambari-server"

AMBARI_CONFIGS_DIR="${ROOT}/etc/ambari-server/conf"
AMBARI_CONFIGS_DIR_SAVE="${ROOT}/etc/ambari-server/conf.save"
AMBARI_CONFIGS_DIR_SAVE_BACKUP="${ROOT}/etc/ambari-server/conf_$(date '+%d_%m_%y_%H_%M').save"
AMBARI_LOG4J="${AMBARI_CONFIGS_DIR}/log4j.properties"

clean_pyc_files(){
  # cleaning old *.pyc files
  find ${RESOURCE_MANAGEMENT_DIR:?} -name *.pyc -exec rm {} \;
  find ${COMMON_DIR:?} -name *.pyc -exec rm {} \;
  find ${AMBARI_SERVER:?} -name *.pyc -exec rm {} \;
}


do_install(){
  rm -f "$AMBARI_SERVER_EXECUTABLE_LINK"
  ln -s "$AMBARI_SERVER_EXECUTABLE" "$AMBARI_SERVER_EXECUTABLE_LINK"
 
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

  #TODO we need this when upgrading from pre 2.4 versions to 2.4, remove this when upgrade from pre 2.4 versions will be
  #TODO unsupported
  clean_pyc_files

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
	mkdir -p "$PYTHON_WRAPER_DIR"
    ln -s "$AMBARI_PYTHON" "$PYTHON_WRAPER_TARGET"
  fi

  sed -i "s|ambari.root.dir\s*=\s*/|ambari.root.dir=${ROOT_DIR_PATH}|g" "$AMBARI_LOG4J"
  sed -i "s|root_dir\s*=\s*/|root_dir = ${ROOT_DIR_PATH}|g" "$CA_CONFIG"
  sed -i "s|^ROOT=\"/\"$|ROOT=\"${ROOT_DIR_PATH}\"|g" "$AMBARI_SERVER_EXECUTABLE"

  AUTOSTART_SERVER_CMD="" 
  which chkconfig > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    AUTOSTART_SERVER_CMD="chkconfig --add ambari-server"
  fi
  which update-rc.d > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    AUTOSTART_SERVER_CMD="update-rc.d ambari-server defaults"
  fi
    
  # if installed to customized root folder, skip ambari-server service actions,
  # as no file in /etc/init.d/ambari-server is present
  if [ ! "${ROOT}/" -ef "/" ] ; then 
	echo "Not adding ambari-server service to startup, as installed to customized root."
	echo "If you need this functionality run the commands below, which create ambari-server service and configure it to run at startup: "
	echo "sudo ln -s ${AMBARI_SERVER_EXECUTABLE} /etc/init.d/ambari-server"
	echo "sudo $AUTOSTART_SERVER_CMD"
  else
	$AUTOSTART_SERVER_CMD
  fi

  if [ -f "$AMBARI_ENV_RPMSAVE" ] ; then
    PYTHON_PATH_LINE='export PYTHONPATH=$PYTHONPATH:/usr/lib/python2.6/site-packages'
    grep "^$PYTHON_PATH_LINE\$" "$AMBARI_ENV_RPMSAVE" > /dev/null
    if [ $? -ne 0 ] ; then
      echo -e "\n$PYTHON_PATH_LINE" >> $AMBARI_ENV_RPMSAVE
    fi
  fi
}

do_remove(){
  $AMBARI_SERVER_EXECUTABLE stop > /dev/null 2>&1

  clean_pyc_files

  if [ -d "$AMBARI_CONFIGS_DIR_SAVE" ]; then
    mv "$AMBARI_CONFIGS_DIR_SAVE" "$AMBARI_CONFIGS_DIR_SAVE_BACKUP"
  fi
  # Remove link created during install
  rm -f "$AMBARI_SERVER_EXECUTABLE_LINK"
  mv "$AMBARI_CONFIGS_DIR" "$AMBARI_CONFIGS_DIR_SAVE"
    
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
