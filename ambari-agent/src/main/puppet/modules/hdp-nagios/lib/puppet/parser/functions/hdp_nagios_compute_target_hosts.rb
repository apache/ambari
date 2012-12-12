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
module Puppet::Parser::Functions
  newfunction(:hdp_nagios_compute_target_hosts, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    monitored_hosts = args[0]
    component_name_mapping = args[1]
    ret = Hash.new
    monitored_hosts.each do |host_info|
      hostname = host_info.keys.first
      cmps = host_info.values.first
      cmps.each do |cmp|
        next unless host_var_info = component_name_mapping[cmp]
        host_var = host_var_info['host_var']
	if host_var_info['type'] == 'array'
          (ret[host_var] ||= Array.new) << hostname
	elsif host_var_info['type'] == 'scalar'
	  ret[host_var] = hostname
        end
      end
    end	
    ret
  end
end
