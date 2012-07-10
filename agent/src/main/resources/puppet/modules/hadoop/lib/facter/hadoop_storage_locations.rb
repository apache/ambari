# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# We make the assumption hadoop's data files will be located in /data/
# Puppet needs to know where they are
Facter.add("hadoop_storage_locations") do
        setcode do

            data_dir_path = "/data/"
            storage_locations = ""

            # We need to check /data/ exist
            if File.directory?(data_dir_path)

              # We assume all data directory will be a number
              Dir.foreach(data_dir_path) { |directory|
                  storage_locations += (data_dir_path + directory + ';') if directory =~ /\d+/
              }
            end

            # Return the list of storage locations for hadoop
            if storage_locations == ""
              storage_locations = "/mnt"
            end
            storage_locations
        end
end

