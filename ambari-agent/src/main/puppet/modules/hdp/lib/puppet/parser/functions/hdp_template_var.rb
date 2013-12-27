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
  newfunction(:hdp_template_var, :type => :rvalue) do |args|
    args = [args].flatten
    qualified_var = args[0]
    unless qualified_var =~ /^::/
      #module_name = lookupvar("module_name")||"UNKNOWN"
      #qualified_var = "::#{module_name}::params::#{args[0]}"
      component = lookupvar("component")||"UNKNOWN"
      module_name = (component == "base" ? "::hdp" : "::hdp-#{component}")      
      qualified_var = "#{module_name}::params::#{args[0]}"
    end
    val = lookupvar(qualified_var)
    if function_hdp_is_empty(val) == false and val.class == String
      val = val.strip
    end  
    (val.nil? or val == :undefined) ? "" : val 
  end
end
