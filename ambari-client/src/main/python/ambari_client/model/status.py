#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging
from ambari_client.model.base_model import  BaseModel 
from ambari_client.model.utils import retain_self_helper
from ambari_client.model.paths import BOOTSTRAP_PATH
LOG = logging.getLogger(__name__)




class StatusModel(BaseModel):
  RO_ATTR = ()
  RW_ATTR = ('status','requestId')
  REF_ATTR = ('cluster_name',)

  def __init__(self, resource_root, status ,requestId=None):
    #BaseModel.__init__(self, **locals())
    retain_self_helper(**locals())

  def __str__(self):
    return "<<StatusModel>> = %s (requestId = %s)" % (self.status, self.requestId)

  def get_request_path(self):
    return BOOTSTRAP_PATH + '/' + self.requestId

