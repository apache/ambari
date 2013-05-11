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
module Puppet::Parser::Functions
  newfunction(:hdp_hadoop_get_mode, :type => :rvalue) do |args|
  
    dir = args[0]

    oozie_dir = lookupvar("::hdp::params::oozie_hdfs_user_dir")
    oozie_dir_mode = lookupvar("::hdp::params::oozie_hdfs_user_mode") 
    
    hcat_dir = lookupvar("::hdp::params::hcat_hdfs_user_dir")
    hcat_dir_mode = lookupvar("::hdp::params::hcat_hdfs_user_mode") 
    
    webhcat_dir = lookupvar("::hdp::params::webhcat_hdfs_user_dir")
    webhcat_dir_mode = lookupvar("::hdp::params::webhcat_hdfs_user_mode") 
    
    hive_dir = lookupvar("::hdp::params::hive_hdfs_user_dir")
    hive_dir_mode = lookupvar("::hdp::params::hive_hdfs_user_mode") 
    
    smoke_dir = lookupvar("::hdp::params::smoke_hdfs_user_dir")
    smoke_dir_mode = lookupvar("::hdp::params::smoke_hdfs_user_mode") 
    
    modes = []
    modes.push({:dir => oozie_dir, :mode => oozie_dir_mode})
    modes.push({:dir => hcat_dir, :mode => hcat_dir_mode})
    modes.push({:dir => webhcat_dir, :mode => webhcat_dir_mode})
    modes.push({:dir => hive_dir, :mode => hive_dir_mode})
    modes.push({:dir => smoke_dir, :mode => smoke_dir_mode})

    modes_grouped = {}
    modes.each do |item|
      if modes_grouped[item[:dir]].nil?
        modes_grouped[item[:dir]]=[]
      end
      modes_grouped[item[:dir]]=modes_grouped[item[:dir]] + [(item[:mode])]
    end

    modes_max = {}
    
    modes_grouped.each_key do |key|
      modes_max[key] = modes_grouped[key].max
    end

    modes_max[dir]
  end
end
