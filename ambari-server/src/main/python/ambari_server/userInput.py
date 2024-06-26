#!/usr/bin/env python3

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
import re

from ambari_commons.logging_utils import get_silent
from ambari_commons.os_utils import get_password


#
# Gets the y/n input.
#
# return True if 'y' or False if 'n'
#
def get_YN_input(prompt, default, answer = None):
  yes = set(['yes', 'ye', 'y'])
  no = set(['no', 'n'])
  if answer is not None and answer:
    yes.update(['True', 'true'])
    no.update(['False', 'false'])

  return get_choice_string_input(prompt, default, yes, no, answer)


def get_choice_string_input(prompt, default, firstChoice, secondChoice, answer = None):
  if get_silent():
    print(prompt)
    return default
  hasAnswer = answer is not None and answer
  if hasAnswer:
    print(prompt)

  inputBool = True
  result = default
  while inputBool:
    choice = str(answer) if hasAnswer else input(prompt).lower()
    if choice in firstChoice:
      result = True
      inputBool = False
    elif choice in secondChoice:
      result = False
      inputBool = False
    elif choice == "":  # Just enter pressed
      result = default
      inputBool = False
    else:
      print("inputBool not recognized, please try again: ")
      quit_if_has_answer(hasAnswer)

  return result


def get_validated_string_input(prompt, default, pattern, description,
                               is_pass, allowEmpty=True, validatorFunction=None, answer = None):
  inputBool = ""
  hasAnswer = answer != None and (answer or allowEmpty)
  if hasAnswer:
    print(prompt)

  while not inputBool:
    if get_silent():
      print(prompt)
      inputBool = default
    elif is_pass:
      inputBool = str(answer) if hasAnswer else get_password(prompt)
    else:
      inputBool = str(answer) if hasAnswer else input(prompt)
    if not inputBool.strip():
      # Empty input - if default available use default
      if not allowEmpty and not default:
        msg = 'Property' if description == None or description == "" else description
        msg += ' cannot be blank.'
        print(msg)
        inputBool = ""
        quit_if_has_answer(hasAnswer)
        continue
      else:
        inputBool = default
        if validatorFunction:
          if not validatorFunction(inputBool):
            inputBool = ""
            quit_if_has_answer(hasAnswer)
            continue
        break  # done here and picking up default
    else:
      if not pattern == None and not re.search(pattern, inputBool.strip()):
        print(description)
        inputBool = ""
        quit_if_has_answer(hasAnswer)

      if validatorFunction:
        if not validatorFunction(inputBool):
          inputBool = ""
          quit_if_has_answer(hasAnswer)
          continue
  return inputBool

def get_validated_filepath_input(prompt, description, default = None, answer = None):
  inputBool = False
  hasAnswer = answer is not None and answer
  while not inputBool:
    if get_silent():
      print(prompt)
      return default
    else:
      inputBool = str(answer) if hasAnswer else input(prompt)
      if not inputBool == None:
        inputBool = inputBool.strip()
      if not inputBool == None and not "" == inputBool and os.path.isfile(inputBool):
        return inputBool
      else:
        print(description)
        quit_if_has_answer(hasAnswer)
        inputBool = False


def get_multi_line_input(prompt, end_line=""):
  full_prompt = prompt
  if end_line:
    full_prompt += " ([{0}] to finish input):".format(end_line)
  else:
    full_prompt += " (empty line to finish input):".format(end_line)

  print(full_prompt)
  user_input = None
  while True:
    line = input()
    if line == end_line:  # no strip() here for purpose
      return user_input
    else:
      user_input = line if user_input is None else user_input + "\n" + line


def get_prompt_default(defaultStr=None):
  if not defaultStr or defaultStr == "":
    return ""
  else:
    return '(' + defaultStr + ')'


def read_password(password_default,
                  password_pattern,
                  password_prompt=None,
                  password_descr=None,
                  answer=None,
                  confirm_password_prompt="Re-enter password: "):

  inputBool = True
  while(inputBool):
    # setup password
    if password_prompt is None:
      password_prompt = 'Password (' + password_default + '): '

    if password_descr is None:
      password_descr = "Invalid characters in password. Use only alphanumeric or " \
                      "_ or - characters"

    password = get_validated_string_input(password_prompt, password_default,
                                          password_pattern, password_descr, True, answer = answer)
    if not password:
      print('Password cannot be blank.')
      continue

    if password != password_default:
      password1 = get_validated_string_input(confirm_password_prompt, password_default, password_pattern,
                                             password_descr, True, answer = answer)
      if password != password1:
        print("Passwords do not match")
        continue

    inputBool = False

  return password

# quits from the application only if the input is provided with a flag ('--customInput=')
def quit_if_has_answer(hasAnswer):
  if hasAnswer:
    print("Validation has failed for the last input. Operation has interrupted.")
    exit(1)