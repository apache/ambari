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
  newfunction(:hdp_hadoop_get_owner, :type => :rvalue) do |args|
  
    dir = args[0]
    
    oozie_dir = lookupvar("::hdp::params::oozie_hdfs_user_dir")
    oozie_user = lookupvar("::hdp::params::oozie_user") 

    hcat_dir = lookupvar("::hdp::params::hcat_hdfs_user_dir")
    hcat_user = lookupvar("::hdp::params::hcat_user") 

    webhcat_dir = lookupvar("::hdp::params::webhcat_hdfs_user_dir")
    webhcat_user = lookupvar("::hdp::params::webhcat_user") 

    hive_dir = lookupvar("::hdp::params::hive_hdfs_user_dir")
    hive_user = lookupvar("::hdp::params::hive_user") 

    smoke_dir = lookupvar("::hdp::params::smoke_hdfs_user_dir")
    smoke_user = lookupvar("::hdp::params::smokeuser") 

    dirs_to_owners = {}
    dirs_to_owners[oozie_dir] = oozie_user
    dirs_to_owners[hcat_dir] = hcat_user
    dirs_to_owners[webhcat_dir] = webhcat_user
    dirs_to_owners[hive_dir] = hive_user
    dirs_to_owners[smoke_dir] = smoke_user

    dirs_to_owners[dir]
  end
end
