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

# WARNING. Please keep the script POSIX compliant and don't use bash extensions

ROOT_DIR_PATH="${RPM_INSTALL_PREFIX:-}"
ROOT=$(printf '%s' "${ROOT_DIR_PATH}" | sed 's|/*$||')
AMBARI_UNIT="ambari-agent"
ACTION=$1
AMBARI_AGENT_ROOT_DIR="${ROOT}/usr/lib/${AMBARI_UNIT}"
AMBARI_SERVER_ROOT_DIR="${ROOT}/usr/lib/ambari-server"
COMMON_DIR="${AMBARI_AGENT_ROOT_DIR}/lib/ambari_commons"
RESOURCE_MANAGEMENT_DIR="${AMBARI_AGENT_ROOT_DIR}/lib/resource_management"
OLD_OLD_COMMON_DIR="${AMBARI_AGENT_ROOT_DIR}/lib/common_functions"
AMBARI_AGENT="${AMBARI_AGENT_ROOT_DIR}/lib/ambari_agent"
PYTHON_WRAPER_TARGET="${ROOT}/usr/bin/ambari-python-wrap"
AMBARI_AGENT_VAR="${ROOT}/var/lib/${AMBARI_UNIT}"
AMBARI_AGENT_BINARY="${ROOT}/etc/init.d/${AMBARI_UNIT}"
AMBARI_AGENT_BINARY_SYMLINK="${ROOT}/usr/sbin/${AMBARI_UNIT}"
AMBARI_ENV_RPMSAVE="${AMBARI_AGENT_VAR}/ambari-env.sh.rpmsave"
AMBARI_HELPER="${AMBARI_AGENT_VAR}/install-helper.sh.orig"
AMBARI_CONFIG_DIR="${ROOT}/etc/ambari-agent/conf"

LOG_FILE=/dev/null

OLD_COMMON_DIR="${ROOT}/usr/lib/python2.6/site-packages/ambari_commons"
OLD_RESOURCE_MANAGEMENT_DIR="${ROOT}/usr/lib/python2.6/site-packages/resource_management"
OLD_JINJA_DIR="${ROOT}/usr/lib/python2.6/site-packages/ambari_jinja2"
OLD_SIMPLEJSON_DIR="${ROOT}/usr/lib/python2.6/site-packages/ambari_simplejson"
OLD_AMBARI_AGENT_DIR="${ROOT}/usr/lib/python2.6/site-packages/ambari_agent"
OBSOLETE_PYTHON_PATHS="ambari_jinja2;ambari_simplejson;ambari_stomp;ambari_ws4py;ambari_pbkdf2;ambari_pyaes;ambari_agent/apscheduler;ambari_agent/RemoteDebugUtils.py;ambari_agent/debug.py"


resolve_log_file(){
 local log_dir="${ROOT}/var/log/${AMBARI_UNIT}"
 local log_file="${log_dir}/${AMBARI_UNIT}-pkgmgr.log"

 if [ ! -d "${log_dir}" ]; then
   mkdir -p "${log_dir}" 1>/dev/null 2>&1
 fi

 if [ -d "${log_dir}" ]; then
   touch ${log_file} 1>/dev/null 2>&1
   if [ -f "${log_file}" ]; then
    LOG_FILE="${log_file}"
   fi
 fi

 echo "--> Install-helper custom action log started at $(date '+%d/%m/%y %H:%M') for '${ACTION}'" 1>>${LOG_FILE} 2>&1
}

clean_pyc_files(){
  local lib_dir="${AMBARI_AGENT_ROOT_DIR}/lib"

  echo "Cleaning generated Python bytecode from ${lib_dir}..."
  if [ -d "${lib_dir}" ]; then
    find "${lib_dir:?}" -type f \( -name '*.pyc' -o -name '*.pyo' \) \
      -exec rm -f -- {} + 1>>"${LOG_FILE}" 2>&1
  fi
}

clean_obsolete_python_sources(){
  printf '%s\n' "${OBSOLETE_PYTHON_PATHS}" | tr ';' '\n' | while IFS= read -r item; do
    obsolete_path="${AMBARI_AGENT_ROOT_DIR}/lib/${item}"
    if [ -e "${obsolete_path}" ] || [ -L "${obsolete_path}" ]; then
      echo "Removing obsolete Python source ${obsolete_path}..." 1>>"${LOG_FILE}" 2>&1
      rm -rf -- "${obsolete_path}"
    fi
  done
}

remove_ambari_unit_dir(){
  # removing empty dirs, which left after cleaning pyc files

  find "${AMBARI_AGENT_ROOT_DIR}" -type d | tac | while read item; do
    echo "Removing empty dir ${item}..."
    rmdir --ignore-fail-on-non-empty ${item} 1>/dev/null 2>&1
  done

  rm -rf ${AMBARI_HELPER}
  find "${AMBARI_AGENT_VAR}" -type d | tac | while read item; do
    echo "Removing empty dir ${item}..."
    rmdir --ignore-fail-on-non-empty ${item} 1>/dev/null 2>&1
  done
}

remove_autostart(){
  if [ -n "${ROOT}" ]; then
    echo "Not removing ambari-agent service from startup for a custom install root."
    return 0
  fi

  which chkconfig
  if [ "$?" -eq 0 ] ; then
    chkconfig --list | grep ambari-server && chkconfig --del ambari-agent
  fi
  which update-rc.d
  if [ "$?" -eq 0 ] ; then
    update-rc.d -f ambari-agent remove
  fi
}

install_autostart(){
  if [ -n "${ROOT}" ]; then
    echo "Not adding ambari-agent service to startup for a custom install root."
    return 0
  fi

  which chkconfig 1>>${LOG_FILE} 2>&1
  if [ "$?" -eq 0 ] ; then
    chkconfig --add ambari-agent
  fi
  which update-rc.d 1>>${LOG_FILE} 2>&1
  if [ "$?" -eq 0 ] ; then
    update-rc.d ambari-agent defaults
  fi
}

required_python_version(){
  local abi_versions
  abi_versions=$(find "${AMBARI_AGENT_ROOT_DIR}/lib" -type f -name '*.cpython-*-*.so' -exec basename {} \; 2>/dev/null \
    | sed -n 's/.*\.cpython-\([0-9][0-9]*\)-.*/\1/p' \
    | sort -u)
  if [ -z "${abi_versions}" ] || [ "$(printf '%s\n' "${abi_versions}" | wc -l)" -ne 1 ]; then
    return 1
  fi
  printf '%s.%s\n' "$(printf '%s' "${abi_versions}" | cut -c1)" "$(printf '%s' "${abi_versions}" | cut -c2-)"
}

locate_python(){
  local python_binary required_version
  if ! required_version=$(required_python_version); then
    >&2 echo "Cannot determine one CPython ABI from ${AMBARI_AGENT_ROOT_DIR}/lib."
    return 1
  fi
  for python_binary in "/usr/bin/python${required_version}" /usr/bin/python3; do
    if [ -x "${python_binary}" ] && "${python_binary}" -c 'import sys; required = tuple(map(int, sys.argv[1].split("."))); sys.exit(sys.version_info < (3, 9, 2) or sys.version_info[:2] != required)' "${required_version}" >>"${LOG_FILE}" 2>/dev/null; then
      echo "${python_binary}"
      return 0
    fi
  done
  return 1
}

install_python_wrapper(){
  local python_binary="$1"
  local temporary_wrapper="${PYTHON_WRAPER_TARGET}.tmp.$$"
  rm -f "${temporary_wrapper}"
  ln -s "${python_binary}" "${temporary_wrapper}" || return 1
  if ! mv -f "${temporary_wrapper}" "${PYTHON_WRAPER_TARGET}"; then
    rm -f "${temporary_wrapper}"
    return 1
  fi
}

do_install(){
  local config_save="${ROOT}/etc/ambari-agent/conf.save"
  if [ -d "${config_save}" ]; then
    cp -f "${config_save}"/* "${AMBARI_CONFIG_DIR}"
    mv "${config_save}" "${ROOT}/etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save"
  fi

  # these symlinks (or directories) where created in ambari releases prior to ambari-2.6.2. Do clean up.   
  rm -rf "${OLD_COMMON_DIR}" "${OLD_RESOURCE_MANAGEMENT_DIR}" "${OLD_JINJA_DIR}" "${OLD_SIMPLEJSON_DIR}" "${OLD_OLD_COMMON_DIR}" "${OLD_AMBARI_AGENT_DIR}"
  clean_obsolete_python_sources

  # setting up /usr/sbin/ambari-agent symlink
  rm -f "${AMBARI_AGENT_BINARY_SYMLINK}"
  ln -s "${AMBARI_AGENT_BINARY}" "${AMBARI_AGENT_BINARY_SYMLINK}"

  # on nano Ubuntu, when umask=027 those folders are created without 'x' bit for 'others'.
  # which causes failures when hadoop users try to access tmp_dir
  chmod a+x ${AMBARI_AGENT_VAR}

  chmod 1777 ${AMBARI_AGENT_VAR}/tmp
  chmod 700 ${AMBARI_AGENT_VAR}/keys
  chmod 700 ${AMBARI_AGENT_VAR}/data

  install_autostart 1>>${LOG_FILE} 2>&1

  local ambari_python
  local bak="${AMBARI_CONFIG_DIR}/ambari-agent.ini.old"
  local upgrade_agent_configs_script="${AMBARI_AGENT_VAR}/upgrade_agent_configs.py"

  if ! ambari_python=$(locate_python); then
    >&2 echo "Cannot detect the Python 3.9.2+ runtime matching the packaged CPython ABI. Please install the matching supported Python runtime."
    return 1
  fi
  mkdir -p "$(dirname "${PYTHON_WRAPER_TARGET}")" || return 1
  if ! install_python_wrapper "${ambari_python}"; then
    >&2 echo "Cannot install ${PYTHON_WRAPER_TARGET}."
    return 1
  fi

  if [ -f "${bak}" ]; then
    if [ -f "${upgrade_agent_configs_script}" ]; then
      "${ambari_python}" "${upgrade_agent_configs_script}"
    fi
    mv "${bak}" "${bak}_$(date '+%d_%m_%y_%H_%M').save"
  fi

  if [ -f "${AMBARI_ENV_RPMSAVE}" ] ; then
    PYTHON_PATH_LINE="export PYTHONPATH=${AMBARI_AGENT_ROOT_DIR}/lib:\$\{PYTHONPATH\}"
    grep "^${PYTHON_PATH_LINE}\$" "${AMBARI_ENV_RPMSAVE}" >>${LOG_FILE}
    if [ $? -ne 0 ] ; then
      echo -e "\n${PYTHON_PATH_LINE}" 1>>${AMBARI_ENV_RPMSAVE}
    fi
  fi
}

copy_helper(){
  cp -f "${AMBARI_AGENT_VAR}/install-helper.sh" "${AMBARI_HELPER}" 1>/dev/null 2>&1
}

do_remove(){
  if [ -z "${ROOT}" ]; then
    "${AMBARI_AGENT_BINARY_SYMLINK}" stop 1>>${LOG_FILE} 2>&1
  fi

  rm -f "${AMBARI_AGENT_BINARY_SYMLINK}" 1>>${LOG_FILE} 2>&1

  if [ -d "${ROOT}/etc/ambari-agent/conf.save" ]; then
    mv "${ROOT}/etc/ambari-agent/conf.save" "${ROOT}/etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save"
  fi
  # first step / label: config_backup
  cp -rf "${AMBARI_CONFIG_DIR}" "${ROOT}/etc/ambari-agent/conf.save"

  remove_autostart 1>>${LOG_FILE} 2>&1
  copy_helper 1>>${LOG_FILE} 2>&1
}

do_cleanup(){
  # do_cleanup is a function, which called after do_remove stage and is supposed to be save place to
  # remove obsolete files generated by application activity

  clean_pyc_files 1>>${LOG_FILE} 2>&1

  # second step / label: config_backup
  rm -rf "${AMBARI_CONFIG_DIR}"

  if [ ! -d "${AMBARI_SERVER_ROOT_DIR}" ]; then
    echo "Removing ${PYTHON_WRAPER_TARGET} ..." 1>>${LOG_FILE} 2>&1
    rm -f ${PYTHON_WRAPER_TARGET} 1>>${LOG_FILE} 2>&1
  fi

  remove_ambari_unit_dir 1>>${LOG_FILE} 2>&1
}

do_upgrade(){
  do_install
}

do_backup(){
  # ToDo: find a way to move backup logic here from preinstall.sh and preinst scripts
  # ToDo: general problem is that still no files are installed on step, when backup is supposed to be done
  echo ""
}

resolve_log_file

case "${ACTION}" in
  install)
    do_install
    ;;
  remove)
    do_remove
    ;;
  upgrade)
    do_upgrade
    ;;
  cleanup)
    do_cleanup
    ;;
  *)
    echo "Wrong command given"
    ;;
esac
