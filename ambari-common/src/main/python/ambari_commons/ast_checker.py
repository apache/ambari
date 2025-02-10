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

import ast
from abc import ABC, abstractmethod
from typing import Set, Dict, Callable, List

import logging

logger = logging.getLogger(__name__)
"""
This module provides a framework for checking the safety of Python code expressions.
It includes abstract base classes for defining rule templates, concrete rule implementations,
and an AST checker that applies these rules to validate code safety.
"""


class RuleTemplate(ABC):
  """
  Abstract base class for defining rule templates.
  Subclasses should implement methods to specify allowed names, functions, node types,
  and custom checks for AST nodes.
  """

  @abstractmethod
  def allowed_names(self) -> Set[str]:
    """Return a set of allowed variable names."""
    pass

  @abstractmethod
  def allowed_functions(self) -> Set[str]:
    """Return a set of allowed function names."""
    pass

  @abstractmethod
  def allowed_node_types(self) -> Set[type]:
    """Return a set of allowed AST node types."""
    pass

  @abstractmethod
  def custom_checks(self) -> Dict[type, Callable[[ast.AST], bool]]:
    """Return a dictionary of custom check functions for specific AST node types."""
    pass


class DefaultRule(RuleTemplate):
  """
  A default implementation of RuleTemplate that provides a basic set of allowed
  names, functions, node types, and no custom checks.
  """

  def allowed_names(self) -> Set[str]:
    """Allow only 'args' as a variable name."""
    return {"args"}

  def allowed_functions(self) -> Set[str]:
    """Allow a limited set of safe functions."""
    return {"calculate", "max", "min", "len"}

  def allowed_node_types(self) -> Set[type]:
    """Allow a comprehensive set of safe AST node types."""
    return {
      ast.Num,
      ast.Str,
      ast.Constant,
      ast.Name,
      ast.Call,
      ast.BinOp,
      ast.UnaryOp,
      ast.Compare,
      ast.BoolOp,
      ast.Subscript,
      ast.Index,
      ast.Load,
      ast.Expression,
      ast.Add,
      ast.Sub,
      ast.Mult,
      ast.Div,
      ast.FloorDiv,
      ast.Mod,
      ast.Pow,
      ast.UAdd,
      ast.USub,
      ast.Eq,
      ast.NotEq,
      ast.Lt,
      ast.LtE,
      ast.Gt,
      ast.GtE,
      ast.And,
      ast.Or,
      ast.Not,
      ast.Subscript,
      ast.Lambda,
      ast.IfExp,
      ast.If,
      ast.Return,
      ast.Continue,
    }

  def custom_checks(self) -> Dict[type, Callable[[ast.AST], bool]]:
    """No custom checks for the default rule."""
    return {}


class ASTChecker:
  """
  A class that checks the safety of Python code by analyzing its Abstract Syntax Tree (AST).
  It applies a set of rules to determine if the code contains only allowed constructs.
  """

  def __init__(self, rules: List[RuleTemplate], use_blacklist: bool = False):
    """
    Initialize the ASTChecker with a list of rules and a flag to use blacklist mode.

    :param rules: List of RuleTemplate objects defining the safety rules.
    :param use_blacklist: If True, use blacklist mode; otherwise, use whitelist mode.
    """
    self.rules = rules
    self.use_blacklist = use_blacklist
    if not use_blacklist:
      self._compile_rules()

  def _compile_rules(self):
    """Compile all rules into combined sets of allowed constructs."""
    self.allowed_names = set().union(*(rule.allowed_names() for rule in self.rules))
    self.allowed_functions = set().union(
      *(rule.allowed_functions() for rule in self.rules)
    )
    self.allowed_node_types = set().union(
      *(rule.allowed_node_types() for rule in self.rules)
    )

    # Combine custom checks from all rules
    self.custom_checks = {}
    for rule in self.rules:
      for node_type, check_func in rule.custom_checks().items():
        if node_type in self.custom_checks:
          original_func = self.custom_checks[node_type]
          self.custom_checks[node_type] = (
            lambda node, of=original_func, nf=check_func: of(node) and nf(node)
          )
        else:
          self.custom_checks[node_type] = check_func

  def is_safe_expression(self, code: str) -> bool:
    """
    Check if the given code string is a safe expression according to the defined rules.

    :param code: The code string to check.
    :return: True if the code is safe, False otherwise.
    """
    try:
      # First, try to parse as an expression
      tree = ast.parse(code, mode="eval")
    except SyntaxError:
      try:
        # If that fails, try to parse as a statement
        tree = ast.parse(code, mode="exec")
      except SyntaxError:
        logger.info(f"Syntax error in expression: {code}")
        return False

    return self.is_safe_node(tree)

  def is_safe_node(self, node: ast.AST) -> bool:
    """
    Recursively check if an AST node and all its children are safe.

    :param node: The AST node to check.
    :return: True if the node and all its children are safe, False otherwise.
    """
    # Apply custom checks from all rules
    for rule in self.rules:
      custom_checks = rule.custom_checks()
      for node_type, check_func in custom_checks.items():
        if isinstance(node, node_type):
          if not check_func(node):
            return False

    # Recursively check all child nodes
    for child in ast.iter_child_nodes(node):
      if not self.is_safe_node(child):
        return False

    return True

  def _is_safe_node_blacklist(self, node: ast.AST) -> bool:
    """
    Check if a node is safe using blacklist rules.

    :param node: The AST node to check.
    :return: True if the node is not blacklisted, False otherwise.
    """
    for rule in self.rules:
      custom_checks = rule.custom_checks()
      if type(node) in custom_checks:
        if not custom_checks[type(node)](node):
          return False
    return True

  def _is_safe_node_whitelist(self, node: ast.AST) -> bool:
    """
    Check if a node is safe using whitelist rules.

    :param node: The AST node to check.
    :return: True if the node is allowed, False otherwise.
    """
    if not isinstance(node, tuple(self.allowed_node_types)):
      logger.info(f"Node type not allowed: {type(node).__name__}")
      return False

    if isinstance(node, ast.Name):
      if node.id not in self.allowed_names and node.id not in self.allowed_functions:
        logger.info(f"Name not allowed: {node.id}")
        return False
    elif isinstance(node, ast.Call):
      if (
        not isinstance(node.func, ast.Name)
        or node.func.id not in self.allowed_functions
      ):
        logger.info(f"Function call not allowed: {ast.dump(node.func)}")
        return False

    node_type = type(node)
    if node_type in self.custom_checks:
      if not self.custom_checks[node_type](node):
        logger.info(f"Custom check failed for node: {ast.dump(node)}")
        return False

    # Recursively check child nodes
    for child in ast.iter_child_nodes(node):
      if not self.is_safe_node(child):
        return False

    return True

  def print_ast_tree(self, code: str):
    """
    Print the AST tree of the given code string.

    :param code: The code string to visualize.
    """
    try:
      tree = ast.parse(code, mode="eval")
      logger.info("AST Tree:")
      self._print_node(tree, "", True)
    except SyntaxError:
      logger.info(f"Syntax error in expression: {code}")

  def _print_node(self, node: ast.AST, prefix: str, is_last: bool):
    """
    Recursively print an AST node and its children.

    :param node: The AST node to print.
    :param prefix: The prefix string for the current line.
    :param is_last: Whether this is the last child of its parent.
    """
    print(prefix + ("└── " if is_last else "├── ") + type(node).__name__)

    # Prepare the prefix for child nodes
    child_prefix = prefix + ("    " if is_last else "│   ")

    # Get all fields of the node
    fields = [(name, value) for name, value in ast.iter_fields(node)]

    # Print fields and child nodes
    for i, (name, value) in enumerate(fields):
      is_last_field = i == len(fields) - 1

      if isinstance(value, ast.AST):
        self._print_node(value, child_prefix, is_last_field)
      elif isinstance(value, list) and value and isinstance(value[0], ast.AST):
        print(child_prefix + ("└── " if is_last_field else "├── ") + name + ":")
        for j, item in enumerate(value):
          self._print_node(item, child_prefix + "    ", j == len(value) - 1)
      else:
        print(child_prefix + ("└── " if is_last_field else "├── ") + f"{name}: {value}")


class BlacklistRule:
  """
  A rule that defines a blacklist of dangerous functions, modules, and constructs.
  It provides custom checks to ensure these blacklisted items are not used in the code.
  """

  def __init__(self):
    """Initialize the blacklist of dangerous items and modules."""
    self.blacklist = {
      "eval",
      "exec",
      "compile",
      "__import__",
      "open",
      "file",
      "os.system",
      "subprocess.call",
      "subprocess.Popen",
      "pickle.loads",
      "pickle.load",
      "marshal.loads",
      "builtins",
      "__builtins__",
      "globals",
      "locals",
      "getattr",
      "setattr",
      "delattr",
      "hasattr",
      "importlib",
      "importlib.import_module",
      "os",
      "subprocess",
      "sys",
      "shutil",
      "pty",
    }
    self.dangerous_modules = {
      "os",
      "subprocess",
      "sys",
      "importlib",
      "pickle",
      "marshal",
    }

  def custom_checks(self) -> Dict[type, Callable[[ast.AST], bool]]:
    """Return a dictionary of custom check functions for specific AST node types."""
    return {
      ast.Name: self._check_name,
      ast.Call: self._check_call,
      ast.Attribute: self._check_attribute,
      ast.Import: self._check_import,
      ast.ImportFrom: self._check_importfrom,
      ast.Subscript: self._check_subscript,
      ast.Module: self._check_module,
    }

  def _check_name(self, node: ast.AST) -> bool:
    """Check if a Name node is not in the blacklist and doesn't start with an underscore."""
    if isinstance(node, ast.Name):
      return node.id not in self.blacklist and not node.id.startswith("_")
    return True

  def _check_call(self, node: ast.Call) -> bool:
    """Check if a function call is safe."""
    if isinstance(node.func, ast.Name):
      return node.func.id not in self.blacklist and not node.func.id.startswith("_")
    elif isinstance(node.func, ast.Attribute):
      return self._check_attribute(node.func)
    return True

  def _check_attribute(self, node: ast.Attribute) -> bool:
    """Check if an attribute access is safe."""
    full_name = self._get_attribute_name(node)
    return full_name not in self.blacklist and not full_name.split(".")[-1].startswith(
      "_"
    )

  def _check_import(self, node: ast.Import) -> bool:
    """Check if an import statement is safe."""
    return all(
      alias.name not in self.dangerous_modules and not alias.name.startswith("_")
      for alias in node.names
    )

  def _check_importfrom(self, node: ast.ImportFrom) -> bool:
    """Check if an import from statement is safe."""
    if node.module in self.dangerous_modules or (
      node.module and node.module.startswith("_")
    ):
      return False
    return all(
      alias.name not in self.blacklist and not alias.name.startswith("_")
      for alias in node.names
    )

  def _check_subscript(self, node: ast.Subscript) -> bool:
    """Check if a subscript operation is safe."""
    if isinstance(node.value, ast.Name):
      return node.value.id not in self.blacklist
    elif isinstance(node.value, ast.Attribute):
      return self._check_attribute(node.value)
    return True

  def _check_module(self, node: ast.Module) -> bool:
    """Check if a module is safe by examining its contents."""
    for stmt in node.body:
      if isinstance(stmt, (ast.Import, ast.ImportFrom)):
        if isinstance(stmt, ast.Import):
          if not self._check_import(stmt):
            return False
        else:
          if not self._check_importfrom(stmt):
            return False
      elif isinstance(stmt, ast.Expr):
        if not self.is_safe_node(stmt.value):
          return False
    return True

  def _get_attribute_name(self, node: ast.Attribute) -> str:
    """Get the full name of an attribute."""
    if isinstance(node.value, ast.Name):
      return f"{node.value.id}.{node.attr}"
    elif isinstance(node.value, ast.Attribute):
      return f"{self._get_attribute_name(node.value)}.{node.attr}"
    return node.attr

  def is_safe_node(self, node: ast.AST) -> bool:
    """Check if a node and all its children are safe."""
    for check_func in self.custom_checks().values():
      if not check_func(node):
        return False
    for child in ast.iter_child_nodes(node):
      if not self.is_safe_node(child):
        return False
    return True


class CustomRule(RuleTemplate):
  """
  A custom rule implementation that allows specific constructs and provides
  custom checks for list comprehensions and container sizes.
  """

  def allowed_names(self) -> Set[str]:
    """Return a set of allowed variable names."""
    return {"custom_var", "another_var", "x"}  # 'x' added for list comprehension

  def allowed_functions(self) -> Set[str]:
    """Return a set of allowed function names."""
    return {"safe_function", "range"}  # 'range' added for list comprehension

  def allowed_node_types(self) -> Set[type]:
    """Return a set of allowed AST node types."""
    return {
      ast.List,
      ast.Dict,
      ast.ListComp,
      ast.comprehension,
      ast.Compare,
      ast.BinOp,
      ast.BoolOp,
      ast.And,
      ast.Or,
      ast.Eq,
      ast.Gt,
      ast.Lt,
      ast.Mod,
      ast.Name,
      ast.Load,
      ast.Store,
      ast.Call,
      ast.Constant,
    }

  def custom_checks(self) -> Dict[type, Callable[[ast.AST], bool]]:
    return {
      ast.List: lambda node: len(node.elts) <= 10,
      ast.Dict: lambda node: len(node.keys) <= 5,
      ast.ListComp: self._check_list_comp,
    }

  def _check_list_comp(self, node: ast.ListComp) -> bool:
    # Check if the list comprehension would produce at most 10 elements
    if (
      isinstance(node.generators[0].iter, ast.Call)
      and isinstance(node.generators[0].iter.func, ast.Name)
      and node.generators[0].iter.func.id == "range"
    ):
      range_arg = node.generators[0].iter.args[0]
      if isinstance(range_arg, ast.Constant):
        return range_arg.value <= 10
    return False  # If we can't determine the size, consider it unsafe
