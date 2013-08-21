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
#to get namenode service id in HA setup

module Puppet::Parser::Functions
  newfunction(:hdp_hadoop_get_namenode_id, :type => :rvalue) do |args|
    namenode_id = ""
    if args.length > 1
      # Get hdfs-site to lookup hostname properties
      lookup_property = args[0]
      siteName = args[1]
      siteConfig = lookupvar("#{siteName}")
      nn_ids_str = lookupvar("::hdp::params::dfs_ha_namenode_ids")
      hostname = lookupvar("::hdp::params::hostname")
      nn_ids = nn_ids_str.to_s.split(',')

      if nn_ids.length > 1
        nn_ids.each do |id|
          lookup_key = lookup_property + "." + id.to_s.strip
          property_val = siteConfig.fetch(lookup_key, "")
          if property_val != "" and property_val.include? hostname
            namenode_id = id
          end
        end
      end
    end
    namenode_id.strip
  end
end