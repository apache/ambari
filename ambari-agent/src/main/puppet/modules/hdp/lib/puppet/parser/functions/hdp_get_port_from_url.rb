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
# to get Port from URL string
module Puppet::Parser::Functions
  newfunction(:hdp_get_port_from_url, :type => :rvalue) do |args|
    def is_numeric?(s)
       !!Integer(s) rescue false
    end

    var = args.empty? ? "" : args.kind_of?(Array) ? args[0].split(":")[1] : args.split(":")[1]
    
    if function_hdp_is_empty(var)
       if args.kind_of?(Array)
          if args.length > 1
             var = args[1]        
          else 
             is_numeric?(args[0]) ? args[0] : ""
          end
       else 
          is_numeric?(args) ? args : "";
       end 
    else 
       var
    end
  end
end
