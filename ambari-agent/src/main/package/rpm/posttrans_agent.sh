# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License


RESOURCE_MANAGEMENT_DIR="/usr/lib/python2.6/site-packages/resource_management"
RESOURCE_MANAGEMENT_DIR_AGENT="/usr/lib/ambari-agent/lib/resource_management"
JINJA_DIR="/usr/lib/python2.6/site-packages/ambari_jinja2"
JINJA_AGENT_DIR="/usr/lib/ambari-agent/lib/ambari_jinja2"

# remove RESOURCE_MANAGEMENT_DIR if it's a directory
if [ -d "$RESOURCE_MANAGEMENT_DIR" ]; then  # resource_management dir exists
  if [ ! -L "$RESOURCE_MANAGEMENT_DIR" ]; then # resource_management dir is not link
    rm -rf "$RESOURCE_MANAGEMENT_DIR"
  fi
fi
# setting resource_management shared resource
if [ ! -d "$RESOURCE_MANAGEMENT_DIR" ]; then
  ln -s "$RESOURCE_MANAGEMENT_DIR_AGENT" "$RESOURCE_MANAGEMENT_DIR"
fi

# setting jinja2 shared resource
if [ ! -d "$JINJA_DIR" ]; then
  ln -s "$JINJA_AGENT_DIR" "$JINJA_DIR"
fi

exit 0