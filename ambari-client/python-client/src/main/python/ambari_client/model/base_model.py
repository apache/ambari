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

import sys
import logging
import time
from ambari_client.model.utils import get_REF_object, get_unicode, getREF_var_name, LIST_KEY
from operator import itemgetter, attrgetter

__docformat__ = "epytext"

LOG = logging.getLogger(__name__)


class BaseModel(object):

    """
    The BaseModel

    RW_ATTR - A list of mutable attributes
    RO_ATTR - A list of immutable attributes
    REF_ATTR - A REF attribute

    """
    RO_ATTR = ()
    RW_ATTR = ()
    REF_ATTR = ()

    def __init__(self, resource_root, **rw_attrs):
        # print" ================== base_model\n"
        # print locals()
        self._resource_root = resource_root
        for k, v in rw_attrs.items():
            if k not in self.RW_ATTR:
                raise ValueError("Unknown argument '%s' in %s" %
                                 (k, self.__class__.__name__))
            self._setattr(k, v)

    def _get_resource_root(self):
        return self._resource_root

    def to_json_dict(self):
        dic = {}
        for attr in self.RW_ATTR:
            value = getattr(self, attr)
            try:
                value = value.to_json_dict()
            except Exception:
                pass
            dic[attr] = value
        return dic

    def _setattr(self, k, v):
        """Set an attribute. """
        value = v
        if v and k.endswith("Ref"):
            cls_name = k[0].upper() + k[1:]
            cls_name = cls_name[:-3] + "ModelRef"
            cls = get_REF_object(cls_name)
            LOG.debug(str(cls_name) + "  -  " + str(cls))
            v = get_unicode(v)
            var_name = getREF_var_name(cls_name)
            c = {str(var_name): str(v)}
            LOG.debug(c)
            value = cls(self._get_resource_root(), **c)
        setattr(self, k, value)


class ModelList(object):

    """A list of Model objects"""

    def __init__(self, objects):
        self.objects = objects

    def __str__(self):
        return "<<ModelList>>[size = %d]) = [%s]" % (
            len(self.objects),
            ", ".join([str(item) for item in self.objects]))

    def to_json_dict(self):
        return {LIST_KEY:
                [x.to_json_dict() for x in self.objects]}

    def __len__(self):
        return self.objects.__len__()

    def __iter__(self):
        return self.objects.__iter__()

    def sort(self, sortkey):
        self.objects = sorted(self.objects, key=sortkey, reverse=True)

    def __getitem__(self, i):
        return self.objects.__getitem__(i)
