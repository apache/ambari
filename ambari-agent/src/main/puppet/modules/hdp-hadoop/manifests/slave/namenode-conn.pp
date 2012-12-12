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
#TODO: this might be replaced by just using hdp::namenode-conn
class hdp-hadoop::slave::namenode-conn($namenode_host)
{
  #TODO: check if can get rido of both
  Hdp-Hadoop::Configfile<||>{namenode_host => $namenode_host}
  Hdp::Configfile<||>{namenode_host => $namenode_host} #for components other than hadoop (e.g., hbase) 
}
