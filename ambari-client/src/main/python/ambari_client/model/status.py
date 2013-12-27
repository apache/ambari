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
from ambari_client.model import paths , utils


LOG = logging.getLogger(__name__)


class StatusModel(BaseModel):
  """
  The ServiceModel class
  """
  RO_ATTR = ('id',)
  RW_ATTR = ('status', 'requestId', "message")
  REF_ATTR = ('cluster_name',)

  def __init__(self, resource_root, status , requestId=None, message=None):
    #BaseModel.__init__(self, **locals())
    utils.retain_self_helper(BaseModel, **locals())

  def __str__(self):
    return "<<StatusModel>> status = %s ; requestId = %s ;message = %s" % (self.status, self._get_id() , self.get_message())

  def get_bootstrap_path(self):
    return paths.BOOTSTRAP_PATH + '/' + str(self.requestId)

  def get_request_path(self):
    return self.request_path

  def get_message(self):
    if hasattr(self, 'message'):
        return self.message
    else:
        None

  def is_error(self):
    return (self.status != 200 and self.status != 201 and self.status != 202)  
        
  def _get_id(self):
    if hasattr(self, 'requestId') and self.requestId:
        return self.requestId
    elif hasattr(self, 'id') and self.id:
        return self.id
    else:
        None
