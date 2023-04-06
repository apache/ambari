"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

PXF_PORT = 51200

# Service Check params 
service_check_hostname = "localhost"
pxf_hdfs_test_dir = "/pxf_hdfs_smoke_test"
pxf_hdfs_read_test_file = pxf_hdfs_test_dir + "/pxf_smoke_test_read_data"
pxf_hdfs_write_test_file = pxf_hdfs_test_dir + "/pxf_smoke_test_write_data"
pxf_hbase_test_table = "pxf_hbase_smoke_test_table"
hbase_populate_data_script = "hbase-populate-data.sh"
hbase_cleanup_data_script = "hbase-cleanup-data.sh"
pxf_hive_test_table = "pxf_hive_smoke_test_table"
hive_populate_data_script = "hive-populate-data.hql"
pxf_service_name = "pxf-service"
pxf_user = "pxf"
default_exec_timeout = 600
