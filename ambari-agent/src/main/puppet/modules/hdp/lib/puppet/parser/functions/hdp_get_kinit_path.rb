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
#given set of paths find the first full path to knit
module Puppet::Parser::Functions
  newfunction(:hdp_get_kinit_path, :type => :rvalue) do |args|
    kinit_path = ""
    if args.length > 0
      args.join(",").split(',').reject{|s| s.strip.length < 1}.each do |s|
        path = File.join(s.strip, "kinit")
        if File.exist?(path) and File.file?(path)
          kinit_path = path
          break
        end
      end
    end
    kinit_path
  end
end