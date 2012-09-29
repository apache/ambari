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
  newfunction(:hdp_nagios_all_hosts, :type => :rvalue) do 
    hg_defs = function_hdp_template_var("hostgroup_defs")
    ret = Array.new
    if hg_defs.kind_of?(Hash)
      hg_defs.each_value do |info|
        h = function_hdp_host(info['host_member_info'])
        unless function_hdp_is_empty(h)
          ret += [h].flatten 
        end
      end
    end
    ret.uniq
  end
end
