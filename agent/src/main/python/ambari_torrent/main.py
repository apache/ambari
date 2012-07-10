#!/usr/bin/env python

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
# limitations under the License.

import os, errno
import logging
import logging.handlers
from ambari_agent.shell import shellRunner
import threading
import sys
import time
import signal
#from config import Config

logger = logging.getLogger()

def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError, exc:
        if exc.errno == errno.EEXIST:
            pass
        else: 
            raise

def main():
    logger.setLevel(logging.DEBUG)
    formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - %(message)s")
    stream_handler = logging.StreamHandler()
    stream_handler.setFormatter(formatter)
    logger.addHandler(stream_handler)
    try:
#        try:
#            f = file('/etc/ambari/agent.cfg')
#            cfg = Config(f)
#            if cfg.ambariPrefix != None:
#                ambariPrefix = cfg.ambariPrefix
#            else:
#                ambariPrefix = '/opt/ambari'
#        except Exception, cfErr:
        ambariPrefix = '/home/ambari'
        time.sleep(15)
        workdir = ambariPrefix + '/var/tmp'
        if not os.path.exists(workdir):
          mkdir_p(workdir)
          
        tracker = workdir + '/tracker'
        f = open(tracker, 'w')
        f.write(str(0))
        f.close()
    except Exception, err:
        logger.exception(str(err))
    
if __name__ == "__main__":
    main()
