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

=begin
  This function returns value of an attribute for a given host
  or an array of attributes for a given array of hosts (one-to-one mapping).
  The attribute type is specified by a string identifier (like "publicfqdn").
=end

  newfunction(:hdp_host_attribute, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    hash,attr,source = args
    ret_val = lambda do |hash,attr,s|
      ret = ""
      ndx = hash[s]
      unless function_hdp_is_empty(ndx)
        val = ndx[attr]
        ret = function_hdp_is_empty(val) ? "" : val
      end
      ret
    end
    if source.kind_of?(Array)
      source.map{|s|ret_val.call(hash,attr,s)}
    else
     ret_val.call(hash,attr,source)
    end
  end
end
