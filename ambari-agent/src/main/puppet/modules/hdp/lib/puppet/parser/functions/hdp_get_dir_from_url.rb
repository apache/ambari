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
# to get directory from URL string
module Puppet::Parser::Functions
  newfunction(:hdp_get_dir_from_url, :type => :rvalue) do |args|

    if args.length > 1
      default = args[1]
    else
      default = ""
    end

    if args.empty?
      var = default
    else
      if args.kind_of?(Array)
        splitArgsResult = args[0].split(":")
      else
        splitArgsResult = args.split(":")
      end
      if splitArgsResult.length != 3
        var = default
      else
        strWithDir = splitArgsResult[2]
        startIndexOfDir = strWithDir.index('/')
        if startIndexOfDir == nil
          var = default
        else
          var = strWithDir[startIndexOfDir, strWithDir.size - 1]
        end
      end
    end
  end
end