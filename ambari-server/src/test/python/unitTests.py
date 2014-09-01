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

import unittest
import multiprocessing
import os
import sys
from random import shuffle
import fnmatch

#excluded directories with non-test staff from stack and service scanning,
#also we can add service or stack to skip here
STACK_EXCLUDE = ["utils"]
SERVICE_EXCLUDE = ["configs"]

TEST_MASK = '[Tt]est*.py'
CUSTOM_TEST_MASK = '_[Tt]est*.py'
def get_parent_path(base, directory_name):
  """
  Returns absolute path for directory_name, if directory_name present in base.
  For example, base=/home/user/test2, directory_name=user - will return /home/user
  """
  done = False
  while not done:
    base = os.path.dirname(base)
    if base == "/":
      return None
    done = True if os.path.split(base)[-1] == directory_name else False
  return base

def get_test_files(path, mask = None, recursive=True):
  """
  Returns test files for path recursively
  """
  current = []
  directory_items = os.listdir(path)

  for item in directory_items:
    add_to_pythonpath = False
    if os.path.isfile(path + "/" + item):
      if fnmatch.fnmatch(item, mask):
        add_to_pythonpath = True
        current.append(item)
    elif os.path.isdir(path + "/" + item):
      if recursive:
        current.extend(get_test_files(path + "/" + item, mask = mask))
    if add_to_pythonpath:
      sys.path.append(path)
  return current


def stack_test_executor(base_folder, service, stack, custom_tests, executor_result):
  """
  Stack tests executor. Must be executed in separate process to prevent module
  name conflicts in different stacks.
  """
  #extract stack scripts folders
  if custom_tests:
    test_mask = CUSTOM_TEST_MASK
  else:
    test_mask = TEST_MASK

  server_src_dir = get_parent_path(base_folder, 'src')

  base_stack_folder = os.path.join(server_src_dir,
                                   'main/resources/stacks/HDP/{0}'.format(stack))

  script_folders = set()
  for root, subFolders, files in os.walk(os.path.join(base_stack_folder,
                                                      "services", service)):
    if os.path.split(root)[-1] in ["scripts", "files"] and service in root:
      script_folders.add(root)

  sys.path.extend(script_folders)

  tests = get_test_files(base_folder, mask = test_mask)

  shuffle(tests)
  modules = [os.path.basename(s)[:-3] for s in tests]
  suites = [unittest.defaultTestLoader.loadTestsFromName(name) for name in
    modules]
  testSuite = unittest.TestSuite(suites)
  textRunner = unittest.TextTestRunner(verbosity=2).run(testSuite)

  #for pretty output
  sys.stdout.flush()
  sys.stderr.flush()
  exit_code = 0 if textRunner.wasSuccessful() else 1
  executor_result.put({'exit_code':exit_code,
                  'tests_run':textRunner.testsRun,
                  'errors':[(str(item[0]),str(item[1]),"ERROR") for item in textRunner.errors],
                  'failures':[(str(item[0]),str(item[1]),"FAIL") for item in textRunner.failures]})
  executor_result.put(0) if textRunner.wasSuccessful() else executor_result.put(1)

def main():
  custom_tests = False
  if len(sys.argv) > 1:
    if sys.argv[1] == "true":
      custom_tests = True
  pwd = os.path.abspath(os.path.dirname(__file__))

  ambari_server_folder = get_parent_path(pwd,'ambari-server')
  ambari_agent_folder = os.path.join(ambari_server_folder,"../ambari-agent")
  ambari_common_folder = os.path.join(ambari_server_folder,"../ambari-common")
  sys.path.append(ambari_common_folder + "/src/main/python")
  sys.path.append(ambari_common_folder + "/src/main/python/ambari_jinja2")
  sys.path.append(ambari_common_folder + "/src/main/python")
  sys.path.append(ambari_common_folder + "/src/test/python")
  sys.path.append(ambari_agent_folder + "/src/main/python")
  sys.path.append(ambari_server_folder + "/src/test/python")
  sys.path.append(ambari_server_folder + "/src/main/python")
  sys.path.append(ambari_server_folder + "/src/main/resources/scripts")
  sys.path.append(ambari_server_folder + "/src/main/resources/custom_actions")
  
  stacks_folder = pwd+'/stacks'
  #generate test variants(path, service, stack)
  test_variants = []
  for stack in os.listdir(stacks_folder):
    current_stack_dir = stacks_folder+"/"+stack
    if os.path.isdir(current_stack_dir) and stack not in STACK_EXCLUDE:
      for service in os.listdir(current_stack_dir):
        current_service_dir = current_stack_dir+"/"+service
        if os.path.isdir(current_service_dir) and service not in SERVICE_EXCLUDE:
          if service == 'hooks':
            for hook in os.listdir(current_service_dir):
              test_variants.append({'directory':current_service_dir + "/" + hook,
                                    'service':hook,
                                    'stack':stack})
          else:
            test_variants.append({'directory':current_service_dir,
                                  'service':service,
                                  'stack':stack})

  #run tests for every service in every stack in separate process
  has_failures = False
  test_runs = 0
  test_failures = []
  test_errors = []
  for variant in test_variants:
    executor_result = multiprocessing.Queue()
    sys.stderr.write( "Running tests for stack:{0} service:{1}\n"
                      .format(variant['stack'],variant['service']))
    process = multiprocessing.Process(target=stack_test_executor,
                                      args=(variant['directory'],
                                            variant['service'],
                                            variant['stack'],
                                            custom_tests,
                                            executor_result)
          )
    process.start()
    process.join()
    #for pretty output
    sys.stdout.flush()
    sys.stderr.flush()
    variant_result = executor_result.get()
    test_runs += variant_result['tests_run']
    test_errors.extend(variant_result['errors'])
    test_failures.extend(variant_result['failures'])

    if variant_result['exit_code'] != 0:
      has_failures = True

  #run base ambari-server tests
  sys.stderr.write("Running tests for ambari-server\n")
  if custom_tests:
    test_mask = CUSTOM_TEST_MASK
  else:
    test_mask = TEST_MASK

  tests = get_test_files(pwd, mask=test_mask, recursive=False)
  shuffle(tests)
  modules = [os.path.basename(s)[:-3] for s in tests]
  suites = [unittest.defaultTestLoader.loadTestsFromName(name) for name in
    modules]
  testSuite = unittest.TestSuite(suites)
  textRunner = unittest.TextTestRunner(verbosity=2).run(testSuite)
  test_runs += textRunner.testsRun
  test_errors.extend([(str(item[0]),str(item[1]),"ERROR") for item in textRunner.errors])
  test_failures.extend([(str(item[0]),str(item[1]),"FAIL") for item in textRunner.failures])
  tests_status = textRunner.wasSuccessful() and not has_failures

  if not tests_status:
    sys.stderr.write("----------------------------------------------------------------------\n")
    sys.stderr.write("Failed tests:\n")
  for failed_tests in [test_errors,test_failures]:
    for err in failed_tests:
      sys.stderr.write("{0}: {1}\n".format(err[2],err[0]))
      sys.stderr.write("----------------------------------------------------------------------\n")
      sys.stderr.write("{0}\n".format(err[1]))
  sys.stderr.write("----------------------------------------------------------------------\n")
  sys.stderr.write("Total run:{0}\n".format(test_runs))
  sys.stderr.write("Total errors:{0}\n".format(len(test_errors)))
  sys.stderr.write("Total failures:{0}\n".format(len(test_failures)))

  if tests_status:
    sys.stderr.write("OK\n")
    exit_code = 0
  else:
    sys.stderr.write("ERROR\n")
    exit_code = 1
  return exit_code


if __name__ == "__main__":
  sys.exit(main())

