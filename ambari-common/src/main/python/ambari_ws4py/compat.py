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
# -*- coding: utf-8 -*-
__doc__ = """
This compatibility module is inspired by the one found
in CherryPy. It provides a common entry point for the various
functions and types that are used with ambari_ws4py but which
differ from Python 2.x to Python 3.x

There are likely better ways for some of them so feel
free to provide patches.

Note this has been tested against 2.7 and 3.3 only but
should hopefully work fine with other versions too.
"""
import sys

if sys.version_info >= (3, 0):
    py3k = True
    from urllib.parse import urlsplit
    range = range
    unicode = str
    basestring = (bytes, str)
    _ord = ord

    def get_connection(fileobj):
        return fileobj.raw._sock

    def detach_connection(fileobj):
        fileobj.detach()

    def ord(c):
        if isinstance(c, int):
            return c
        return _ord(c)
else:
    py3k = False
    from urlparse import urlsplit
    range = xrange
    unicode = unicode
    basestring = basestring
    ord = ord

    def get_connection(fileobj):
        return fileobj._sock

    def detach_connection(fileobj):
        fileobj._sock = None
