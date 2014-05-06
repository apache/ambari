# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information rega4rding copyright ownership.
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
# limitations under the License.


##################################################################
#                      AGENT INSTALL HELPER                      #
##################################################################

COMMON_DIR="/usr/lib/python2.6/site-packages/common_functions"
INSTALL_HELPER_SERVER="/var/lib/ambari-server/install-helper.sh"
COMMON_DIR_AGENT="/usr/lib/ambari-agent/lib/common_functions"


do_install(){
  if [ ! -d "$COMMON_DIR" ]; then
    ln -s "$COMMON_DIR_AGENT" "$COMMON_DIR"
  fi
}

do_remove(){
  if [ -d "$COMMON_DIR" ]; then  # common dir exists
    rm -f "$COMMON_DIR"
    if [ -f "$INSTALL_HELPER_SERVER" ]; then  #  call server shared files installer
      $INSTALL_HELPER_SERVER install
    fi
  fi
}


case "$1" in
install)
  do_install
  ;;
remove)
  do_remove
  ;;
esac
