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
  newfunction(:hdp_nagios_members_exist, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    host_type = args[0]
    hg_defs = function_hdp_template_var("hostgroup_defs")
    if  hg_defs.kind_of?(Hash)
      #TODO: see if needed    Puppet::Parser::Functions.autoloader.loadall
      member_info = (hg_defs[host_type]||{})['host_member_info']
      member_info and not function_hdp_is_empty(function_hdp_host(member_info))
    else
      nil
    end
  end
end
