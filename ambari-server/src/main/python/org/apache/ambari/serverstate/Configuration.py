from pip.backwardcompat import raw_input

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#* Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

class Configuration:

    CORE_FILE = "core-site.xml";
    NAME_NODE_FILE = "hdfs-site.xml";
    JOB_TRACKER_FILE = "mapred-site.xml";
    OOZIE_FILE = "oozie-site.xml";
    HBASE_MASTER_FILE = "hbase-site.xml";

    servicesPath = {"Core": "core-site.xml",
             "NameNode": "hdfs-site.xml",
             "JobTracker": "mapred-site.xml",
             "OOZIE": "oozie-site.xml",
             "HBASE": "hbase-site.xml",
             "ZooKeeper": "zk.properties",
    }

    CONFIG_INIT_TYPE = ("CURRENT_DIR", "USER_PATH_INPUT");
    configurationType = 0;

    def __init__(self, type):
        self.configurationType = type
        self.initPaths()

    def initPaths(self):
        if self.configurationType != 0:
            for service in self.servicesPath.keys():
                path = raw_input("Please enter path for "+ service+" :")
                if len(path) > 0:
                    self.servicesPath[service] = path
                else:
                    raise ValueError("Path to the configuration file can't be empty.") #Catch it layter and start input mode automatically
        #self.getDataFromServices()

    def getDataFromServices(self):
        #TODO
        self.getCoreData()
        self.getNameNodeData()
        self.getOozieData()
        self.getHBaseData()
        self.getZooKeeperData()
        print("###getDataFromServices")


    def getCoreData(self):
        print("TODO get data from core configuration")

    def getNameNodeData(self):
        print("TODO get data from NameNode configuration")

    def getOozieData(self):
        print("TODO get data from Oozie configuration")

    def getHBaseData(self):
        print("TODO get data from HBase configuration")

    def getZooKeeperData(self):
        print("TODO get data from ZooKeeper configuration")






