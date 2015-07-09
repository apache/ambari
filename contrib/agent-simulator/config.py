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

import ConfigParser

class Config:
    ATTRIBUTES = {}

    @staticmethod
    def load():
        config = ConfigParser.RawConfigParser()
        # keep file case sensitive
        config.optionxform = str
        config.read("config/config.ini")
        for section in config.sections():
            for key in config.options(section):
                Config.ATTRIBUTES[key] = config.get(section, key)


# Config.load()
# print Config.ATTRIBUTES