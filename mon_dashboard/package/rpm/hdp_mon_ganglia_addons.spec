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
# RPM Spec file for Ganglia Add-ons for HDP Monitoring Dashboard
#

Summary: Ganglia Add-ons for HDP Monitoring Dashboard
Name: hdp_mon_ganglia_addons
Version: 0.0.2.14
URL: http://incubator.apache.org/projects/ambari.html
Release: 1
License: Apache License, Version 2.0
Vendor: Apache Software Foundation (ambari-dev@incubator.apache.org)
Group: System Environment/Base
Source: %{name}-%{version}.tar.gz
Buildroot: %{_tmppath}/%{name}-%{version}-buildroot
Requires: gweb >= 2.2
%define graphd_dir /var/www/html/ganglia/graph.d/
%define gconf_dir /var/lib/ganglia/conf/
BuildArchitectures: noarch

%description
This package provides add-on graphs and configurations for ganglia to provide 
for a better monitoring integration with a Hadoop Cluster

%prep
%setup -q -n %{name}-%{version}
%build

%install
# Flush any old RPM build root
%__rm -rf $RPM_BUILD_ROOT

%__mkdir -p $RPM_BUILD_ROOT/%{graphd_dir}/
%__mkdir -p $RPM_BUILD_ROOT/%{gconf_dir}/

%__cp -rf conf/* $RPM_BUILD_ROOT/%{gconf_dir}/
%__cp -rf graph.d/* $RPM_BUILD_ROOT/%{graphd_dir}/


%files
%defattr(-,root,root)
%{graphd_dir}/*
%{gconf_dir}/*

%clean
%__rm -rf $RPM_BUILD_ROOT

%changelog
* Thu Jun 07 2011 Ambari <ambari-dev@incubator.apache.org>
- Initial version
