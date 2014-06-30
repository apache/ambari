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


import types
import urllib
import urllib2


def uri_encoding(url):
    """
    Returns an ASCII string version of the URL.
    """
    if url is None:
        return url
    return urllib.quote(get_utf8_str(url), safe="/#%[]=:;$&()+,!?*@'~")


def get_utf8_str(strr, encoding='utf-8'):
    """
    Returns a utf8 ecoded 'str'.
    """
    errors = 'strict'
    if not isinstance(strr, basestring):
        try:
            return str(strr)
        except UnicodeEncodeError:
            if isinstance(strr, Exception):
                return ' '.join([get_utf8_str(arg, encoding) for arg in strr])
            return unicode(strr).encode(encoding, errors)
    elif isinstance(strr, unicode):
        return strr.encode(encoding, errors)
    elif strr and encoding != 'utf-8':
        return strr.decode('utf-8', errors).encode(encoding, errors)
    else:
        return strr
