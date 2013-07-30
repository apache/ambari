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

class hdp-yarn::initialize()
{
  $yarn_user = $hdp-yarn::params::yarn_user
  $mapred_user = $hdp-yarn::params::mapred_user
  
  ##Process package
  hdp-yarn::package{'yarn-common':}
  
  # Create yarn user
  hdp::user { 'yarn_user':
    user_name => $yarn_user
  }
  
  # Create mapred user
  hdp::user { 'mapred_user':
     user_name => $mapred_user
  }
  
  #Generate common configs
  hdp-yarn::generate_common_configs{'yarn-common-configs':}
  
  anchor{ 'hdp-yarn::initialize::begin': } Hdp::Package['yarn-common'] -> Hdp::User['yarn_user'] -> Hdp-yarn::Generate_common_configs['yarn-common-configs'] -> anchor{ 'hdp-yarn::initialize::end': }
}

define hdp-yarn::generate_common_configs() {

  $yarn_config_dir = $hdp-yarn::params::conf_dir

  # Generate configs
  if has_key($::configuration, 'mapred-site') {
    configgenerator::configfile{'mapred-site': 
      modulespath => $yarn_config_dir,
      filename => 'mapred-site.xml',
      module => 'hdp-yarn',
      configuration => $::configuration['mapred-site'],
      owner => $hdp-yarn::params::yarn_user,
      mode => 644
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${yarn_config_dir}/mapred-site.xml":
      owner => $hdp-yarn::params::yarn_user,
      mode => 644
    }
  }
  
  if has_key($::configuration, 'yarn-site') {
    configgenerator::configfile{'yarn-site': 
      modulespath => $yarn_config_dir,
      filename => 'yarn-site.xml',
      module => 'hdp-yarn',
      configuration => $::configuration['yarn-site'],
      owner => $hdp-yarn::params::yarn_user,
      mode => 644
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${yarn_config_dir}/yarn-site.xml":
      owner => $hdp-yarn::params::yarn_user,
      mode => 644
    }
  }

  if has_key($::configuration, 'capacity-scheduler') {
    configgenerator::configfile{'capacity-scheduler': 
      modulespath => $yarn_config_dir,
      filename => 'capacity-scheduler.xml',
      module => 'hdp-yarn',
      configuration => $::configuration['capacity-scheduler'],
      owner => $hdp-yarn::params::yarn_user,
      mode => 644
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${yarn_config_dir}/capacity-scheduler.xml":
      owner => $hdp-yarn::params::yarn_user,
      mode => 644
    }
  }

  hdp::configfile {"${yarn_config_dir}/yarn-env.sh":
    component      => 'yarn',
    owner          => $hdp-yarn::params::yarn_user,
    mode           => 755
  }
}
