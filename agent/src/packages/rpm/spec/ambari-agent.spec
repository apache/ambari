#   Licensed to the Apache Software Foundation (ASF) under one or more
#   contributor license agreements.  See the NOTICE file distributed with
#   this work for additional information regarding copyright ownership.
#   The ASF licenses this file to You under the Apache License, Version 2.0
#   (the "License"); you may not use this file except in compliance with
#   the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

#
# RPM Spec file for HBase version @version@
#

%define name         ambari-agent
%define version      @version@
%define release      @package.release@

# Installation Locations
%define _final_name  @final.name@
%define _prefix      @package.prefix@
%define _bin_dir     %{_prefix}/bin
%define _conf_dir    @package.conf.dir@
%define _include_dir %{_prefix}/include
%define _lib_dir     %{_prefix}/lib
%define _lib64_dir   %{_prefix}/lib64
%define _libexec_dir %{_prefix}/libexec
%define _log_dir     @package.log.dir@
%define _man_dir     %{_prefix}/man
%define _pid_dir     @package.pid.dir@
%define _sbin_dir    %{_prefix}/sbin
%define _share_dir   %{_prefix}/share/ambari
%define _src_dir     %{_prefix}/src
%define _var_dir     %{_prefix}/var/lib

# Build time settings
%define _build_dir  @package.build.dir@
%define debug_package %{nil}

Summary: Hadoop Management System Agent
License: Apache License, Version 2.0
URL: http://incubator.apache.org/ambari
Vendor: Apache Software Foundation
Group: Development/Libraries
Name: %{name}
Version: %{version}
Release: %{release} 
Source0: %{_final_name}.tar.gz
Prefix: %{_bin_dir}
Prefix: %{_conf_dir}
Prefix: %{_log_dir}
Prefix: %{_pid_dir}
Buildroot: %{_build_dir}
Requires: sh-utils, textutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service, transmission-cli, zkpython, zookeeper-lib, BitTorrent-bencode, mimerender, simplejson, mimeparse, web.py, python-setuptools, libevent >= 2.0.10, avahi-tools, python-iniparse, /sbin/ethtool
AutoReqProv: no
Provides: ambari-agent

%description
Ambari Agent manage software installation and configuration for Hadoop software stack.

%prep

%setup -D -a 0 -n usr

%build
if [ -d ${RPM_BUILD_DIR}%{_log_dir} ]; then
  rm -rf ${RPM_BUILD_DIR}%{_log_dir}
fi

if [ -d ${RPM_BUILD_DIR}%{_conf_dir} ]; then
  rm -rf ${RPM_BUILD_DIR}%{_conf_dir}
fi

if [ -d ${RPM_BUILD_DIR}%{_pid_dir} ]; then
  rm -rf ${RPM_BUILD_DIR}%{_pid_dir}
fi

mkdir -p ${RPM_BUILD_DIR}%{_conf_dir}
mkdir -p ${RPM_BUILD_DIR}%{_log_dir}
mkdir -p ${RPM_BUILD_DIR}%{_conf_dir}
mkdir -p ${RPM_BUILD_DIR}/etc/init.d

cp ${RPM_BUILD_DIR}/../../../../src/packages/rpm/init.d/ambari-agent ${RPM_BUILD_DIR}/etc/init.d/ambari-agent
chmod 0755 ${RPM_BUILD_DIR}/etc/init.d/ambari-agent

cp -a ${RPM_BUILD_DIR}/* ${RPM_BUILD_DIR}/../BUILDROOT

%preun
rm -rf /etc/default/ambari-agent-env.sh

%pre
getent group hadoop 2>/dev/null >/dev/null || /usr/sbin/groupadd -g 123 -r hadoop
/usr/sbin/useradd --comment "Ambari" -u 210 --shell /bin/bash -M -r --groups hadoop --home /home/ambari ambari 2> /dev/null || :

%post
mkdir -p ${RPM_INSTALL_PREFIX2}
mkdir -p ${RPM_INSTALL_PREFIX3}
echo "AMBARI_LOG_DIR=${RPM_INSTALL_PREFIX2}" > /etc/default/ambari-agent-env.sh
echo "AMBARI_PID_DIR=${RPM_INSTALL_PREFIX3}" >> /etc/default/ambari-agent-env.sh
mkdir -p /home/ambari/var/tmp
mkdir -p /home/ambari/var/cache/downloads
mkdir -p /home/ambari/apps

%files
%defattr(-,root,root)
%{_prefix}
/etc/init.d/ambari-agent
%config %{_conf_dir}
