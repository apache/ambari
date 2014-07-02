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
import unicodedata
from ambari_client.core import errors

LOG = logging.getLogger(__name__)


ref_dic = {"cluster_name": "clusterRef"}
ref_class_dic = {"ClusterModelRef": "cluster_name"}
ref_pkg_dic = {"ClusterModelRef": "ambari_client.model.cluster"}
LIST_KEY = "items"
ALL = "ALL"


class ModelUtils(object):

    @staticmethod
    def _check_is_error(expected_class, model_dict, resource_root):
        from ambari_client.model.status import StatusModel
        from ambari_client.model.cluster import TaskModel

        if expected_class == TaskModel:
            resp = ModelUtils.create_model(
                TaskModel,
                model_dict.copy(),
                resource_root,
                "NO_KEY",
                check_errors=False)
            return

        if "status" in model_dict:
            resp = ModelUtils.create_model(
                StatusModel,
                model_dict.copy(),
                resource_root,
                "NO_KEY",
                check_errors=False)

            if expected_class != StatusModel or resp.is_error():
                if resp.status in errors._exceptions_to_codes:
                    raise errors._exceptions_to_codes[
                        resp.status](
                        resp,
                        resource_root)
                else:
                    raise errors.UnknownServerError(resp, resource_root)

    @staticmethod
    def get_model_list(
            member_list_clss,
            member_cls,
            collection_dict,
            resource_root,
            RESOURCE_KEY_WORD,
            check_errors=True):
        """
        create a model.
        @param member_list_clss : model_list class.
        @param model_cls : model class.
        @param collection_dict : collection dict used for creating the list of objects.
        @param resource_root : resource object.
        @param RESOURCE_KEY_WORD : tsake subset of model_dict based on this key.
        @return: A  ModelList object.
        """
        tLIST_KEY = LIST_KEY

        if check_errors:
            ModelUtils._check_is_error(
                member_list_clss,
                collection_dict,
                resource_root)

        # print locals()
        json_list = []

        # remove items
        if isinstance(collection_dict, dict) and tLIST_KEY in collection_dict:
            json_list = collection_dict[tLIST_KEY]
            LOG.debug(
                "get_model_list: collection_dict is dict ? %s ; has_key = %s" %
                (isinstance(
                    collection_dict,
                    dict),
                    LIST_KEY in collection_dict))
            LOG.debug(
                "get_model_list: collection_dict has %s ;subset = %s" %
                (tLIST_KEY, str(json_list)))
        else:
            json_list = collection_dict
            LOG.error(
                "get_model_list: collection_dict is dict ? %s ; has_key = %s" %
                (isinstance(
                    collection_dict,
                    dict),
                    LIST_KEY in collection_dict))

        LOG.debug("get_model_list: json_list  value : \n\t" + str(json_list))
        if isinstance(json_list, list):
            json_list_new = [x.get(RESOURCE_KEY_WORD) for x in json_list]
            LOG.debug(
                "get_model_list: json_list is list ? %s ; " %
                (isinstance(
                    json_list,
                    list)))
        else:
            json_list_new = [json_list]
            LOG.error(
                "get_model_list: json_list is list ? %s ; " %
                (isinstance(
                    json_list,
                    list)))

        LOG.debug(
            "get_model_list: json_list_new used for creating ModelList  \n\t" +
            str(json_list_new))
        objects = [
            ModelUtils.create_model(
                member_cls,
                x,
                resource_root,
                RESOURCE_KEY_WORD) for x in json_list_new]
        LOG.debug(objects)
        return member_list_clss(objects)

    @staticmethod
    def create_model(
            model_cls,
            model_dict,
            resource_root,
            RESOURCE_KEY_WORD,
            check_errors=True):
        """
        create a model.
        @param model_cls : model class.
        @param model_dict : model dict used for creating the object.
        @param resource_root : resource object.
        @param RESOURCE_KEY_WORD : tsake subset of model_dict based on this key.
        @return: A model_cls object.
        """
        if check_errors:
            ModelUtils._check_is_error(model_cls, model_dict, resource_root)

        # print locals()
        rw_dict = {}
        LOG.debug("model_dict =   " + str(model_dict))

        # extract model /keyword
        if isinstance(model_dict, dict) and RESOURCE_KEY_WORD in model_dict:
            model_dict = model_dict[RESOURCE_KEY_WORD]
            if not isinstance(model_dict, list):
                LOG.debug(
                    "model_dict has %s ;subset = %s" %
                    (RESOURCE_KEY_WORD, str(
                        model_dict.items())))
            else:
                LOG.debug(
                    "model_dict is list and has %s ;subset = %s" %
                    (RESOURCE_KEY_WORD, str(
                        model_dict)))
        # check for Requests
        if isinstance(model_dict, dict) and "Requests" in model_dict:
            model_dict = model_dict["Requests"]
            LOG.debug(
                "model_dict has Requests ;subset = %s" %
                (str(
                    model_dict.items())))

        # check for composition i.e list of Models
        if isinstance(model_dict, list):
            LOG.debug(
                "model_dict is list")
        else:
            for k, v in model_dict.items():
                LOG.debug("key = %s ; value = %s " % (str(k), str(v)))
                if k in model_cls.RW_ATTR:
                    LOG.debug(k + " is there in RW_ATTR")
                    rw_dict[k] = v
                    del model_dict[k]

        rw_dict = get_unicode_kw(rw_dict)
        obj = model_cls(resource_root, **rw_dict)

        for attr in model_cls.RO_ATTR:
            obj._setattr(attr, None)

        for k, v in model_dict.items():
            if k in model_cls.RO_ATTR:
                obj._setattr(k, v)
            else:
                LOG.debug(
                    "Unexpected attribute '%s' in %s json" %
                    (k, model_cls.__name__))

        for attr in model_cls.REF_ATTR:
            LOG.debug("%s found as reference var" % (attr))
            obj._setattr(getREF_class_name(attr), None)

        for k, v in model_dict.items():
            if k in model_cls.REF_ATTR:
                obj._setattr(getREF_class_name(k), v)
            else:
                LOG.debug(
                    "Unknown attribute '%s' found in model_dict for %s " %
                    (k, model_cls.__name__))
        return obj


# get attribute with REF
def getREF_class_name(REF_name):
    if REF_name in ref_dic:
        return ref_dic[str(REF_name)]
    else:
        return None


def getREF_var_name(REF_name):
    if REF_name in ref_class_dic:
        return ref_class_dic[str(REF_name)]
    else:
        return None


def get_REF_object(ref_class_name):
    """
    Gets the Ref object based on class_name
    """
    class_ref = getattr(
        sys.modules[
            ref_pkg_dic[ref_class_name]],
        ref_class_name)
    LOG.debug(class_ref)
    return class_ref


def get_unicode(v):
    # import unicodedata
    if v:
        if isinstance(v, unicode):
            v = unicodedata.normalize('NFKD', v).encode('ascii', 'ignore')
            LOG.debug(v)
        elif isinstance(v, str):
            LOG.debug("warning: string found while expecting unicode %s" % v)
    return v


def retain_self_helper(memclass, self=None, **kwargs):
    # print locals()
        # from ambari_client.model.base_model import  BaseModel
    memclass.__init__(self, **kwargs)


def get_unicode_kw(dic):
    """
    We use unicode strings as keys in kwargs.
    """
    res = {}
    for k, v in dic.iteritems():
        res[str(k)] = v
    return res


def get_config_type(service_name):
    """
    get the config tmp_type based on service_name
    """
    if service_name == "HDFS":
        tmp_type = "hdfs-site"
    elif service_name == "HDFS":
        tmp_type = "core-site"
    elif service_name == "MAPREDUCE":
        tmp_type = "mapred-site"
    elif service_name == "HBASE":
        tmp_type = "hbase-site"
    elif service_name == "OOZIE":
        tmp_type = "oozie-site"
    elif service_name == "HIVE":
        tmp_type = "hive-site"
    elif service_name == "WEBHCAT":
        tmp_type = "webhcat-site"
    else:
        tmp_type = "global"
    return tmp_type


def get_key_value(dictt, key):
    """
    Search for some random key in the dict
    """
    if isinstance(dictt, dict) and key in dictt:
        return dictt[key]
    elif isinstance(dictt, dict) and key not in dictt:
        # check if values has it?
        for v in dictt.values():
            if isinstance(v, dict):
                return get_key_value(v, key)
            elif isinstance(v, list):
                for l in list:
                    return get_key_value(l, key)
