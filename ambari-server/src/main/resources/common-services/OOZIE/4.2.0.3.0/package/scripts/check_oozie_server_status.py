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

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def check_oozie_server_status():
  import status_params
  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status

  check_windows_service_status(status_params.oozie_server_win_service_name)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def check_oozie_server_status():
  import status_params
  from resource_management.libraries.functions.check_process_status import check_process_status

  check_process_status(status_params.pid_file)

