#!/usr/bin/env python

'''
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
'''

import threading
from daemon import daemonRunner
from package import packageRunner
from shell import shellRunner

class Runner(threading.Thread):
    __instance = None
    lock = None
    def __init__(self):
        if Runner.__instance is None:
          Runner.lock = threading.RLock()
          Runner.__instance = self
    
    def run(self, data):
        Runner.lock.acquire()
        try:
            if data['actionType']=='info':
                ph = packageRunner()
                result = ph.info(data['packages'])
            elif data['actionType']=='install':
                ph = packageRunner()
                if 'dry-run' in data:
                    opt = data['dry-run']
                else:
                    opt = 'false'
                result = ph.install(data['packages'], opt)
            elif data['actionType']=='remove':
                ph = packageRunner()
                if 'dry-run' in data:
                    opt = data['dry-run']
                else:
                    opt = 'false'
                result = ph.remove(data['packages'], opt)
            elif data['actionType']=='status':
                dh = daemonRunner()
                result = dh.status(data['daemonName'])
            elif data['actionType']=='start':
                dh = daemonRunner()
                result = dh.start(data['daemonName'])
            elif data['actionType']=='stop':
                dh = daemonRunner()
                result = dh.stop(data['daemonName'])
            elif data['actionType']=='run' or data['@action']=='org.apache.hms.common.entity.action.ScriptAction':
                she = shellRunner()
                script = []
                script.append(data['script'])
                if "parameters" in data:
                    for parameter in data['parameters']:
                        script.append(parameter)
                result = she.run(script)
            return result
        finally:
            Runner.lock.release()
