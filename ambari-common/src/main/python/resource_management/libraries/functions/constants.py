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

__all__ = ["Direction", "SafeMode", "StackFeature"]

class Direction:
  """
  Stack Upgrade direction
  """
  UPGRADE = "upgrade"
  DOWNGRADE = "downgrade"

class SafeMode:
  """
  Namenode Safe Mode state
  """
  ON = "ON"
  OFF = "OFF"
  UNKNOWN = "UNKNOWN"
  
class StackFeature:
  """
  Stack Feature supported
  """
  SNAPPY = "snappy"
  EXPRESS_UPGRADE = "express_upgrade"
  ROLLING_UPGRADE = "rolling_upgrade"
  CONFIG_VERSIONING = "config_versioning"
  RANGER = "ranger"
  NFS = "nfs"
  TEZ_FOR_SPARK = "tez_for_spark"
  SPARK_16PLUS = "spark_16plus"
  SPARK_THRIFTSERVER = "spark_thriftserver"
    
