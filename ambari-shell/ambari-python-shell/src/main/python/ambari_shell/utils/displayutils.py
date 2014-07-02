#!/usr/bin/env python
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging
import textwrap
import cStringIO
import operator
from xml.etree import ElementTree as etree
from functools import reduce


LOG = logging.getLogger(__name__)


class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'

    def disable(self):
        self.HEADER = ''
        self.OKBLUE = ''
        self.OKGREEN = ''
        self.WARNING = ''
        self.FAIL = ''


__header__ = textwrap.dedent("""
   ___         __            _
  / _ | __ _  / /  ___ _____(_)
 / __ |/  ' \/ _ \/ _ `/ __/ /
/_/ |_/_/_/_/_.__/\_,_/_/ /_/  CLI v%s
""" % str(1))


def shellBanner():
    """
     Prints the CLI Banner.
    """
    return __header__ + textwrap.dedent(
        """
    ====================================
        Welcome to Ambari python CLI
        type 'help' to list all commands..
    ====================================
    """)


def createXML(headers, rows):
    root = etree.Element('xmloutput')
    for r in rows:
        for h, relemt in zip(headers, r):

            child = etree.Element(h.lower().replace(' ', '_'))
            child.text = str(relemt)
            root.append(child)

    # pretty string
    s = etree.tostring(root)
    return s


def createCSV(headers, rows):
    headers = [x.lower().replace(' ', '_') for x in headers]
    print(','.join(headers))
    for r in rows:
        print(','.join(r))


def display_table(headers, rows):

    delimiter = '='
    delimiter1 = ' | '
    output = cStringIO.StringIO()
    temp_rows = [tuple(headers)] + rows
    row_tuple = [(row,) for row in temp_rows]
    # get max width
    verticalrows = map(None, *reduce(operator.add, row_tuple))

    if not rows:
        widthList = [len(str(x)) for x in headers]
        row_width = sum(widthList)
    else:
        widthList = [max([len(str(x)) for x in column])
                     for column in verticalrows]
        row_width = sum(widthList)
    header_line = delimiter * \
        (row_width + len(delimiter1) * (len(widthList) - 1))

    i = 0
    for rr in row_tuple:
        for row in rr:
            print >> output, delimiter1.join(
                [(str(x)).ljust(width) for (x, width) in zip(row, widthList)])
        if i == 0:
            print >> output, header_line
            i = 9999
    return output.getvalue()


if __name__ == '__main__':
    print createXML(['STACK NAME', ], [[u'HDP']])
    createCSV(['STACK NAME', ], [[u'HDP']])
    headers = ['First Name', 'Last Name', 'Age']
    data = \
        '''Sam ,Browne,21
       Jhon,Browne,23
       Adam,senio,21'''
    rows = [row.strip().split(',') for row in data.splitlines()]
    print display_table(headers, rows)
