#!/bin/bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

user=""
secure="false"
keytab=""
kinit_path="/usr/kerberos/bin/kinit"
while getopts ":u:k:s" opt; do
  case $opt in
    u)
      user=$OPTARG;
      ;;
    k)
      keytab=$OPTARG;
      ;;
    s)
      secure="true";
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 3
      ;;
    :)
      echo "UNKNOWNOption -$OPTARG requires an argument." >&2
      exit 3
      ;;
  esac
done

outfile="/tmp/nagios-hbase-check.out"
curtime=`date +"%F-%H-%M-%S"`
fname="nagios-hbase-check-${curtime}"

if [[ "$user" == "" ]]; then
  echo "INVALID: user argument not specified";
  exit 3;
fi
if [[ "$keytab" == "" ]]; then 
  keytab="/homes/$user/$user.headless.keytab"
fi

if [[ ! -f "$kinit_path" ]]; then
  kinit_path="kinit"
fi

if [[ "$secure" == "true" ]]; then
  sudo -u $user -i "$kinit_path -kt $keytab $user" > ${outfile} 2>&1
fi

output=`sudo -u $user -i "echo status | /usr/bin/hbase --config /etc/hbase shell"`
(IFS='')
tmpOutput=$(echo $output | grep -v '0 servers')
if [[ "$?" -ne "0" ]]; then 
  echo "CRITICAL: No region servers are running";
  exit 2; 
fi
sudo -u $user -i "echo disable \'nagios_test_table\' | /usr/bin/hbase --config /etc/hbase shell" > ${outfile} 2>&1
sudo -u $user -i "echo drop \'nagios_test_table\' | /usr/bin/hbase --config /etc/hbase shell" > ${outfile} 2>&1
sudo -u $user -i "echo create \'nagios_test_table\', \'family\' | /usr/bin/hbase --config /etc/hbase shell" > ${outfile} 2>&1
sudo -u $user -i "echo put \'nagios_test_table\', \'row01\', \'family:col01\', \'value1\' | /usr/bin/hbase --config /etc/hbase shell" > ${outfile} 2>&1
output=`sudo -u $user -i "echo scan \'nagios_test_table\' | /usr/bin/hbase --config /etc/hbase shell"`
(IFS='')
tmpOutput=$(echo $output | grep -v '1 row(s) in')
if [[ "$?" -ne "1" ]]; then 
  echo "CRITICAL: Error populating HBase table";
  exit 2; 
fi
sudo -u $user -i "echo disable \'nagios_test_table\' | /usr/bin/hbase --config /etc/hbase shell" > ${outfile} 2>&1
sudo -u $user -i "echo drop \'nagios_test_table\' | /usr/bin/hbase --config /etc/hbase shell" > ${outfile} 2>&1

echo "OK: HBase transaction completed successfully"
exit 0;
