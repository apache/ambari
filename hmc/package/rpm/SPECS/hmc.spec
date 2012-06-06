##
#
#/*
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

#
# RPM Spec file for HMC
#

Summary: HMC
Name: hmc
Version: 0.0.1
URL: http://hortonworks.com
Release: 2%{?dist}
License: Apache License, Version 2.0
Vendor: Hortonworks <hmc-dev-group@hortonworks.com>
Group: System Environment/Base
Source: %{name}-%{version}.tar.gz
Source1: hmc.init.in
Source2: hmc_hdp.repo
Buildroot: %{_tmppath}/%{name}-%{version}-buildroot
Requires: php >= 5, sqlite >= 3, php-pdo, php-pecl-json, httpd, puppet = 2.7.9, pdsh, httpd-devel, ruby-devel, rubygems, mod_passenger, mod_ssl
%define web_prefixdir %{_prefix}/share/hmc
%define httpd_confdir %{_sysconfdir}/httpd/conf.d
%define puppet_master_dir %{_sysconfdir}/puppet/master
%define hmc_passwd_dir %{_sysconfdir}/hmc
%define hmc_db_dir %{_var}/db/hmc
%define hmc_run_dir %{_var}/run/hmc
%define hmc_log_dir %{_var}/log/hmc
BuildArchitectures: noarch

%description
This package provides a Management Console for Hadoop Cluster.

%prep
%setup -q -n %{name}-%{version}

%build

%pre
# Make a backup of existing database before installing new package
if [ -f /var/db/hmc/data/data.db ]; then
  DATE=`date +%d-%m-%y-%H%M`
  mv /var/db/hmc/data/data.db /var/db/hmc/data/data.db.$DATE
fi

%install
# Flush any old RPM build root
%__rm -rf $RPM_BUILD_ROOT
%__install -D -m0755 "%{SOURCE1}" "$RPM_BUILD_ROOT/etc/init.d/%{name}"
%__mkdir -p $RPM_BUILD_ROOT/usr/lib/ruby/site_ruby/1.8/puppet/reports/
%__mkdir -p $RPM_BUILD_ROOT/%{web_prefixdir}/
%__mkdir -p $RPM_BUILD_ROOT/%{web_prefixdir}/bin/
%__mkdir -p $RPM_BUILD_ROOT/%{web_prefixdir}/yum_repo/
%__mkdir -p $RPM_BUILD_ROOT/%{puppet_master_dir}/
%__mkdir -p $RPM_BUILD_ROOT/%{puppet_master_dir}/manifests
%__mkdir -p $RPM_BUILD_ROOT/%{puppet_master_dir}/modules/catalog/files
%__mkdir -p $RPM_BUILD_ROOT/%{web_prefixdir}/
%__install -d "%{buildroot}%{hmc_db_dir}"
%__install -d "%{buildroot}%{hmc_log_dir}"
%__install -d "%{buildroot}%{hmc_run_dir}"
%__install -d "%{buildroot}%{hmc_run_dir}/downloads"
%__mkdir -p $RPM_BUILD_ROOT/%{hmc_db_dir}/data
%__mkdir -p $RPM_BUILD_ROOT/%{hmc_passwd_dir}
%__mkdir -p $RPM_BUILD_ROOT/%{httpd_confdir}/
%__cp -rf css $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf fonts $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf licenses $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf db $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf html $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf ShellScripts $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf images $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf js $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf puppet $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf php $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf yui-3.5.1 $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -f yuiCombinator.php $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf conf $RPM_BUILD_ROOT/%{web_prefixdir}/
%__cp -rf puppet/manifestloader $RPM_BUILD_ROOT/%{puppet_master_dir}
%__cp -rf puppet/modules/stdlib $RPM_BUILD_ROOT/%{puppet_master_dir}/modules
%__cp -f "%{SOURCE2}" $RPM_BUILD_ROOT/%{web_prefixdir}/yum_repo/
%__install -D -m0755 puppet/reports/get_revision $RPM_BUILD_ROOT/%{web_prefixdir}/bin
%__cp -rf puppet/reports/hmcreport.rb $RPM_BUILD_ROOT/usr/lib/ruby/site_ruby/1.8/puppet/reports/
%__tar czf $RPM_BUILD_ROOT/%{web_prefixdir}/modules.tgz puppet/modules
echo "Alias /hdp %{_prefix}/share/hdp" > $RPM_BUILD_ROOT/%{httpd_confdir}/hdp_mon_dashboard.conf

%post
if test X"$RPM_INSTALL_PREFIX0" = X"" ; then
  RPM_INSTALL_PREFIX0="/usr"
fi
php $RPM_INSTALL_PREFIX0/share/hmc/php/frontend/initializeHMC.php /var/db/hmc/data/data.db $RPM_INSTALL_PREFIX0/share/hmc/db/schema.dump
sed -i 's/User\ apache/User\ puppet/g' /etc/httpd/conf/httpd.conf
chmod 666 /var/db/hmc/data/data.db
chown -R puppet:apache /var/db/hmc/
chown -R puppet:apache /var/run/hmc
chown -R puppet:apache /var/log/hmc
chown -R puppet:apache /etc/puppet
mkdir -p /etc/puppet/rack/public
mkdir -p /etc/puppet/rack/tmp
touch /var/run/hmc/lockfile
chown puppet:apache /var/run/hmc/lockfile
cp /usr/share/puppet/ext/rack/files/config.ru /etc/puppet/rack
chown puppet /etc/puppet/rack/config.ru

cp $RPM_INSTALL_PREFIX0/share/hmc/puppet/conf/puppetmaster.conf.template /etc/httpd/conf.d/puppetmaster.conf
cp $RPM_INSTALL_PREFIX0/share/hmc/conf/hmc.conf /etc/httpd/conf.d/hmc.conf
host=`hostname -f | tr '[:upper:]' '[:lower:]'`
sed -i "s/__TODO_HOSTNAME__/$host/g" /etc/httpd/conf.d/puppetmaster.conf
cp $RPM_INSTALL_PREFIX0/share/hmc/puppet/conf/puppet.conf.template /etc/puppet/puppet.conf
echo 0 > /selinux/enforce
htpasswd -mbc /etc/hmc/htpasswd.users hmcadmin hmcadmin
#chown apache:apache /var/db/hmc/data/data.db
chown -R puppet:apache /etc/hmc

%postun
rm -rf /var/run/hmc/clusters/
rm -rf /var/lib/puppet/reports/*
rm -rf /var/lib/puppet/puppet_kick_version.txt
rm -rf /etc/puppet/master
rm -rf /var/run/hmc/license
rm -rf /var/run/hmc/puppetmaster.boot

%files
%defattr(-,root,root)
%{web_prefixdir}/*
%{httpd_confdir}/hdp_mon_dashboard.conf
/usr/lib/ruby/site_ruby/1.8/puppet/reports/hmcreport.rb
%config /etc/init.d/%{name}
%{hmc_passwd_dir}*
%{puppet_master_dir}/*
%{hmc_db_dir}
%{hmc_log_dir}
%{hmc_run_dir}/*
%clean
%__rm -rf $RPM_BUILD_ROOT

%changelog
* Wed Apr 04 2012 Hortonworks <ambari-group@hortonworks.com>
- Initial version
