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

class manifestloader () {
    file { '/etc/puppet/agent/modules.tgz':
      ensure => present,
      source => "puppet:///modules/catalog/modules.tgz",  
      mode => '0755',
    }

    exec { 'untar_modules':
      command => "rm -rf /etc/puppet/agent/modules ; tar zxf /etc/puppet/agent/modules.tgz -C /etc/puppet/agent/ --strip-components 3",
      path    => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    } 

    exec { 'puppet_apply':
      command   => "sh /etc/puppet/agent/modules/puppetApply.sh",
      timeout   => 1800,
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      logoutput => "true"
    }

    File['/etc/puppet/agent/modules.tgz'] -> Exec['untar_modules'] -> Exec['puppet_apply']
}

node default {
 stage{1 :}
 class {'manifestloader': stage => 1}
}

