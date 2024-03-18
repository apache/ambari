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

import time

class BlacklistedSet(set):
  BLACKLIST_TIMEOUT = 60

  def __init__(self, items=[], blacklist_timeout=BLACKLIST_TIMEOUT):
    self.__dict = {}
    self.__blacklist_timeout = blacklist_timeout
    for item in items:
      set.add(self, item)

  def add(self, item):
    self.__dict[item] = time.time()
    set.add(self, item)

  def __contains__(self, item):
    return item in self.__dict and time.time() > self.__dict.get(item)

  def __iter__(self):
    for item in set.__iter__(self):
      if time.time() > self.__dict.get(item):
        yield item

  def get_actual_size(self):
    size = 0
    for item in self.__iter__():
      size += 1
    return size

  def get_item_at_index(self, index):
    i = 0
    for item in self.__iter__():
      if i == index:
        return item
      i += 1
    return None

  def blacklist(self, item):
    self.__dict[item] = time.time() + self.__blacklist_timeout

if __name__ == "__main__":
  hosts = [1, 2, 3, 4]
  bs = BlacklistedSet(hosts)
  bs.blacklist(4)
  print bs
  for a in bs:
    print a
  time.sleep(2)

  bs.blacklist(1)
  bs.blacklist(5)
  for a in bs:
    print a
