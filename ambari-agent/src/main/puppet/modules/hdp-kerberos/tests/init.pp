# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
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

$kerberos_domain = "krb.test.com"
$kerberos_realm = "KRB.TEST.COM"
$kerberos_kdc_server = "localhost"
$kerberos_kdc_port = 88
# the following turns a node into a fully functional KDC 
include kerberos::kdc
# the following opens up KDC principle datbase for remote
# administration (it really should be optional, but it is
# required for now in order to make kerberos::client::host_keytab
# work)
include kerberos::kdc::admin_server

# the following turns a node into a Kerberos client hosts with.. 
include kerberos::client
# ...an optional host_keytab for as many services as you want:
kerberos::client::host_keytab { ["host", "hdfs", "mapred"]: }
