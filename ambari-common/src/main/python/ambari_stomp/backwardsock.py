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
"""Networking functions to support backwards compatibility.

Distinct from the backward(2/3) functions to handle ipv6 changes between Python versions 2.5 and 2.6.
"""

import sys

if sys.hexversion < 0x02060000:  # < Python 2.6
    from ambari_stomp.backwardsock25 import *
else:  # Python 2.6 onwards
    from ambari_stomp.backwardsock26 import *
