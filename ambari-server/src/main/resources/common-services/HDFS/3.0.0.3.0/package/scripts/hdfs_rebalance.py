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

import re
import sys
from resource_management.core.exceptions import Fail
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop

class HdfsParser():
  def __init__(self):
    self.initialLine = None
    self.state = None
  
  def parseLine(self, line):
    hdfsLine = HdfsLine()
    type, matcher = hdfsLine.recognizeType(line)
    if(type == HdfsLine.LineType.HeaderStart):
      self.state = 'PROCESS_STARTED'
    elif (type == HdfsLine.LineType.Progress):
      self.state = 'PROGRESS'
      hdfsLine.parseProgressLog(line, matcher)
      if(self.initialLine == None): self.initialLine = hdfsLine
      
      return hdfsLine 
    elif (type == HdfsLine.LineType.ProgressEnd):
      self.state = 'PROCESS_FINISED'
    return None
    
class HdfsLine():
  
  class LineType:
    HeaderStart, Progress, ProgressEnd, Unknown = range(4)
  
  
  MEMORY_SUFFIX = ['B','KB','MB','GB','TB','PB','EB']
  MEMORY_PATTERN = '(?P<memmult_%d>(?P<memory_%d>(\d+)(.|,)?(\d+)?) (?P<mult_%d>'+"|".join(MEMORY_SUFFIX)+'))'
  
  HEADER_BEGIN_PATTERN = re.compile('Time Stamp\w+Iteration#\w+Bytes Already Moved\w+Bytes Left To Move\w+Bytes Being Moved')
  PROGRESS_PATTERN = re.compile(
                            "(?P<date>.*?)\s+" + 
                            "(?P<iteration>\d+)\s+" + 
                            MEMORY_PATTERN % (1,1,1) + "\s+" + 
                            MEMORY_PATTERN % (2,2,2) + "\s+" +
                            MEMORY_PATTERN % (3,3,3)
                            )
  PROGRESS_END_PATTERN = re.compile('(The cluster is balanced. Exiting...|The cluster is balanced. Exiting...)')
  
  def __init__(self):
    self.date = None
    self.iteration = None
    self.bytesAlreadyMoved = None 
    self.bytesLeftToMove = None
    self.bytesBeingMoved = None 
    self.bytesAlreadyMovedStr = None 
    self.bytesLeftToMoveStr = None
    self.bytesBeingMovedStr = None 
  
  def recognizeType(self, line):
    for (type, pattern) in (
                            (HdfsLine.LineType.HeaderStart, self.HEADER_BEGIN_PATTERN),
                            (HdfsLine.LineType.Progress, self.PROGRESS_PATTERN), 
                            (HdfsLine.LineType.ProgressEnd, self.PROGRESS_END_PATTERN)
                            ):
      m = re.match(pattern, line)
      if m:
        return type, m
    return HdfsLine.LineType.Unknown, None
    
  def parseProgressLog(self, line, m):
    '''
    Parse the line of 'hdfs rebalancer' output. The example output being parsed:
    
    Time Stamp               Iteration#  Bytes Already Moved  Bytes Left To Move  Bytes Being Moved
    Jul 28, 2014 5:01:49 PM           0                  0 B             5.74 GB            9.79 GB
    Jul 28, 2014 5:03:00 PM           1                  0 B             5.58 GB            9.79 GB
    
    Throws AmbariException in case of parsing errors

    '''
    m = re.match(self.PROGRESS_PATTERN, line)
    if m:
      self.date = m.group('date') 
      self.iteration = int(m.group('iteration'))
       
      self.bytesAlreadyMoved = self.parseMemory(m.group('memory_1'), m.group('mult_1')) 
      self.bytesLeftToMove = self.parseMemory(m.group('memory_2'), m.group('mult_2')) 
      self.bytesBeingMoved = self.parseMemory(m.group('memory_3'), m.group('mult_3'))
       
      self.bytesAlreadyMovedStr = m.group('memmult_1') 
      self.bytesLeftToMoveStr = m.group('memmult_2')
      self.bytesBeingMovedStr = m.group('memmult_3') 
    else:
      raise AmbariException("Failed to parse line [%s]") 
  
  def parseMemory(self, memorySize, multiplier_type):
    try:
      factor = self.MEMORY_SUFFIX.index(multiplier_type)
    except ValueError:
      raise AmbariException("Failed to memory value [%s %s]" % (memorySize, multiplier_type))
    
    return float(memorySize) * (1024 ** factor)
  def toJson(self):
    return {
            'timeStamp' : self.date,
            'iteration' : self.iteration,
            
            'dataMoved': self.bytesAlreadyMovedStr,
            'dataLeft' : self.bytesLeftToMoveStr,
            'dataBeingMoved': self.bytesBeingMovedStr,
            
            'bytesMoved': self.bytesAlreadyMoved,
            'bytesLeft' : self.bytesLeftToMove,
            'bytesBeingMoved': self.bytesBeingMoved,
          }
  def __str__(self):
    return "[ date=%s,iteration=%d, bytesAlreadyMoved=%d, bytesLeftToMove=%d, bytesBeingMoved=%d]"%(self.date, self.iteration, self.bytesAlreadyMoved, self.bytesLeftToMove, self.bytesBeingMoved)

def is_balancer_running():
  import params
  check_balancer_command = "fs -test -e /system/balancer.id"
  does_hdfs_file_exist = False
  try:
    _print("Checking if the balancer is running ...")
    ExecuteHadoop(check_balancer_command,
                  user=params.hdfs_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  bin_dir=params.hadoop_bin_dir)

    does_hdfs_file_exist = True
    _print("Balancer is running. ")
  except Fail:
    pass

  return does_hdfs_file_exist

def _print(line):
  sys.stdout.write(line)
  sys.stdout.flush()
