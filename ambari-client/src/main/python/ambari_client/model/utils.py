#
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
import sys

LOG = logging.getLogger(__name__)

ref_dic = {"cluster_name":"clusterRef"}
ref_class_dic = {"ClusterModelRef":"cluster_name"}
ref_pkg_dic={"ClusterModelRef":"ambari_client.model.cluster"}

class ModelUtils(object):

  @staticmethod
  def get_model_list(member_cls, dic, resource_root , RESOURCE_KEY_WORD):
    #print locals()
    from ambari_client.model.base_model import  ModelList
    json_list = dic[ModelList.LIST_KEY]
    LOG.debug ("get_model_list : getting the "+str(ModelList.LIST_KEY)+ " value---> \n\t"+str(json_list) )
    
    if isinstance(json_list, list):
        json_list_new = [ x.get(RESOURCE_KEY_WORD) for x in json_list]
    
    LOG.debug ("get_model_list: creating a array for "+str(RESOURCE_KEY_WORD)+" value---> \n\t"+str(json_list_new))
    objects = [ ModelUtils.create_model(member_cls,x, resource_root ,RESOURCE_KEY_WORD) for x in json_list_new ]
    LOG.debug (objects)
    return ModelList(objects)

  @staticmethod
  def create_model(member_cls, dic, resource_root,RESOURCE_KEY_WORD):
    #print locals()
    rw_dict = { }
    LOG.debug ("    create_model : dic =   "+str(dic))
    if isinstance(dic, dict) and dic.has_key(RESOURCE_KEY_WORD):
        dic=dic[RESOURCE_KEY_WORD]
        LOG.debug ("    dic.items() 2   =   "+str(dic.items()))
    for k, v in dic.items():
      LOG.debug (k)
      LOG.debug (v)
      if k in member_cls.RW_ATTR:
        LOG.debug (k + " is there in RW_ATTR")
        rw_dict[k] = v
        del dic[k]

    rw_dict = get_unicode_kw(rw_dict)
    obj = member_cls(resource_root, **rw_dict)


    for attr in member_cls.RO_ATTR:
      obj._setattr(attr, None)


    for k, v in dic.items():
      if k in member_cls.RO_ATTR:
        obj._setattr(k, v)
      else:
        LOG.debug("Unexpected attribute '%s' in %s json" %(k, member_cls.__name__))

    for attr in member_cls.REF_ATTR:
      LOG.debug(attr)
      obj._setattr(getREF_class_name(attr), None)


    for k, v in dic.items():
      if k in member_cls.REF_ATTR:
        obj._setattr(getREF_class_name(k), v)
      else:
        LOG.debug("Unexpected attribute '%s' in %s json" %(k, member_cls.__name__))
             
    return obj




def getREF_class_name( REF_name):
  if ref_dic.has_key(REF_name):
    return ref_dic[str(REF_name)]
  else:
    return None
  

def getREF_var_name( REF_name):
  if ref_class_dic.has_key(REF_name):
    return ref_class_dic[str(REF_name)]
  else:
    return None

def get_REF_object(ref_class_name):
  """
  Gets the Ref object based on class_name
  """
  class_ref=getattr(sys.modules[ref_pkg_dic[ref_class_name]], ref_class_name)
  LOG.debug( class_ref )
  return class_ref  

def get_unicode( v):
  import unicodedata
  if v:
    v = unicodedata.normalize('NFKD', v).encode('ascii', 'ignore')
    LOG.debug( v )
  return v
  
def retain_self_helper(self=None, **kwargs):
    #print locals()
    from ambari_client.model.base_model import  BaseModel 
    BaseModel.__init__(self, **kwargs)
       
def get_unicode_kw(dic):
  """
  We use unicode strings as keys in kwargs.
  """
  res = { }
  for k, v in dic.iteritems():
    res[str(k)] = v
  return res
