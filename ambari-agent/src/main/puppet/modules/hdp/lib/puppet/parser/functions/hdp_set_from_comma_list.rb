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
#to handle differences in how args passed in
require 'set'
module Puppet::Parser::Functions
  newfunction(:hdp_set_from_comma_list, :type => :rvalue) do |args|
    dir_list = args[0]
    reject_items = args[1].nil? ? [] : function_hdp_array_from_comma_list(args[1])

    list = function_hdp_array_from_comma_list(dir_list)
    list.each_index {|i| list [i]=list [i].strip}
    #Delete empty strings
    list.reject! { |e| e.empty? or reject_items.include?(e) }
    list.uniq   
  end
end