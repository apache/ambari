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
# RPM Spec file for HDP Monitoring Dashboard version @version@
#

Summary: HDP Monitoring Dashboard Frontend
Name: hdp_mon_dashboard
Version: 0.0.2.14
URL: http://incubator.apache.org/projects/ambari.html
Release: 1
License: Apache License, Version 2.0
Vendor: Apache Software Foundation (ambari-dev@incubator.apache.org)
Group: System Environment/Base
Source: %{name}-%{version}.tar.gz
Buildroot: %{_tmppath}/%{name}-%{version}-buildroot
Requires: php >= 5, httpd
%define web_prefixdir %{_prefix}/share/hdp/dashboard
%define httpd_confdir %{_sysconfdir}/httpd/conf.d
BuildArchitectures: noarch

%description
This package provides a monitoring dashboard for a Hadoop cluster. 

%prep
%setup -q -n %{name}-%{version}
%build

%install
# Flush any old RPM build root
%__rm -rf $RPM_BUILD_ROOT

%__mkdir -p $RPM_BUILD_ROOT/%{web_prefixdir}/dataServices/
%__mkdir -p $RPM_BUILD_ROOT/%{web_prefixdir}/ui/
%__mkdir -p $RPM_BUILD_ROOT/%{httpd_confdir}/
%__mkdir -p $RPM_BUILD_ROOT/%{_libexecdir}/hdp_mon_dashboard/

%__cp -rf dataServices/* $RPM_BUILD_ROOT/%{web_prefixdir}/dataServices/
%__cp -rf ui/* $RPM_BUILD_ROOT/%{web_prefixdir}/ui/
echo "Alias /hdp %{_prefix}/share/hdp" > $RPM_BUILD_ROOT/%{httpd_confdir}/hdp_mon_dashboard.conf

%files
%defattr(-,root,root)
%{web_prefixdir}/*
%{httpd_confdir}/hdp_mon_dashboard.conf

%clean
%__rm -rf $RPM_BUILD_ROOT

%changelog
* Thu Jun 07 2012 Ambari <ambari-dev@incubator.apache.org>
- Initial version
