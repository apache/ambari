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

# Returns package name(s) for the specific OS and architecture
# Params:
#
# - name of the package
# - OS name
# - OS architecture
#
# If there are no approprite OS/architecture, it will search default entries (named as ALL)
module Puppet::Parser::Functions
  newfunction(:pkgName, :type => :rvalue) do |args|
    packageName = args[0]
    os = args[1]
    arch = args[2]
    ALL = 'ALL'

    # First level (packages): packageName => OS hashmap
    # Second level (OS hashmap): Architecture => real package name(s)
    packages = {
      'ganglia-monitor' => {
        'ALL' => {
          64 => 'ganglia-gmond-3.2.0'
        }
      },
      'ganglia-server' => {
        'ALL' => {
          64 => 'ganglia-gmetad-3.2.0'
        }
      },
      'ganglia-gweb' => {
        'ALL' => {
          64 => 'gweb'
        }
      },
      'ganglia-hdp-gweb-addons' => {
        ALL => {
          64 => 'hdp_mon_ganglia_addons'
        }
      },
      'glibc' => {
        'rhel6' => {
          ALL => ['glibc','glibc.i686']
        }
      },
      'nagios-addons' => {
        ALL => {
          64 => 'hdp_mon_nagios_addons'
        }
      },
      'nagios-server' => {
        ALL => {
          64 => 'nagios-3.2.3'
        }
      },
      'nagios-plugins' => {
        ALL => {
          64 => 'nagios-plugins-1.4.9'
        }
      },
      'nagios-fping' => {
        ALL => {
          64 =>'fping'
        }
      },
      'nagios-php-pecl-json' => {
        ALL => {
          64 => 'php-pecl-json.x86_64'
        }
      },
      'snmp' => {
        ALL => {
          64 => ['net-snmp','net-snmp-utils'],
        }
      },
      'dashboard' => {
        ALL => {
          64 => 'hdp_mon_dashboard'
        }
      },
      'templeton' => {
        ALL => {
          ALL => 'templeton'
        }
      },
      'oozie-client' => {
        ALL => {
          64 => 'oozie-client.noarch'
        }
      },
      'oozie-server' => {
        ALL => {
          64 => 'oozie.noarch'
        }
      },
      'lzo' => {
        'rhel5' => {
          ALL => ['lzo','lzo.i386','lzo-devel','lzo-devel.i386']
        },
        'rhel6' => {
          ALL => ['lzo','lzo.i686','lzo-devel','lzo-devel.i686']
        }
      },
      #TODO: make these two consistent on whether case of 64/32 bits
      'snappy' => {
        ALL => {
          32 =>  ['snappy','snappy-devel'],
          64 => ['snappy','snappy-devel']
        }
      },
      'mysql' => {
        ALL => {
          32 =>  ['mysql','mysql-server']
        }
      },
      'mysql-connector' => {
        ALL => {
          64 =>  ['mysql-connector-java']
        }
      },
      'extjs' => {
        ALL => {
          64 =>  ['extjs-2.2-1']
        }
      },
      'templeton-tar-hive' => {
        ALL => {
          64 => ['templeton-tar-hive-0.0.1.14-1']
        }
      },
      'templeton-tar-pig' => {
        ALL => {
          64 => ['templeton-tar-pig-0.0.1.14-1']
        }
      }
    }
    ########################################################
    ########################################################
    # seeking package hashmap
    pkgHash = nil
    
    if has_key(packages, packageName) 
      pkgHash = packages[packageName]
    else 
      print "Wrong package name: " + packageName
      return nil
    end
    
    # seeking os hashmap
    osHash = nil
    
    if has_key(pkgHash, os) 
      osHash = pkgHash[os]
    elsif has_key(pkgHash, ALL) 
      osHash = pkgHash[ALL]
    else 
      print "Wrong package name: " + packageName
      return nil
    end
    
    #seeking arhitecture 
    result = nil
    
    if has_key(osHash, arch) 
      result = osHash[arch]
    elsif has_key(osHash, ALL)
      result = osHash[ALL]
    end
    
    return result
  end
end