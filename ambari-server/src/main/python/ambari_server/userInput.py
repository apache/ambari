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

import os
import getpass
import re

from ambari_commons.logging_utils import *


#
# Gets the y/n input.
#
# return True if 'y' or False if 'n'
#
from ambari_commons.os_utils import get_password


def get_YN_input(prompt, default):
  yes = set(['yes', 'ye', 'y'])
  no = set(['no', 'n'])
  return get_choice_string_input(prompt, default, yes, no)


def get_choice_string_input(prompt, default, firstChoice, secondChoice):
  if SILENT:
    print(prompt)
    return default
  choice = raw_input(prompt).lower()
  if choice in firstChoice:
    return True
  elif choice in secondChoice:
    return False
  elif choice is "":  # Just enter pressed
    return default
  else:
    print "input not recognized, please try again: "
    return get_choice_string_input(prompt, default, firstChoice, secondChoice)


def get_validated_string_input(prompt, default, pattern, description,
                               is_pass, allowEmpty=True, validatorFunction=None):

  input = ""
  while not input:
    if SILENT:
      print (prompt)
      input = default
    elif is_pass:
      input = get_password(prompt)
    else:
      input = raw_input(prompt)
    if not input.strip():
      # Empty input - if default available use default
      if not allowEmpty and not default:
        msg = 'Property' if description is None or description is "" else description
        msg += ' cannot be blank.'
        print msg
        input = ""
        continue
      else:
        input = default
        if validatorFunction:
          if not validatorFunction(input):
            input = ""
            continue
        break  # done here and picking up default
    else:
      if not pattern == None and not re.search(pattern, input.strip()):
        print description
        input = ""

      if validatorFunction:
        if not validatorFunction(input):
          input = ""
          continue
  return input

def get_validated_filepath_input(prompt, description, default=None):
  input = False
  while not input:
    if SILENT:
      print (prompt)
      return default
    else:
      input = raw_input(prompt)
      if not input == None:
        input = input.strip()
      if not input == None and not "" == input and os.path.isfile(input):
        return input
      else:
        print description
        input = False
