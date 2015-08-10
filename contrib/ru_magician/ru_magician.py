#!/usr/bin/env python

'''
Copyright (C)  2015, Apache Ambari

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


MIT License
Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

GNU Lesser General Public License v1.3
Permission is granted to copy, distribute and/or modify this document
under the terms of the GNU Free Documentation License, Version 1.3
or any later version published by the Free Software Foundation;
with no Invariant Sections, no Front-Cover Texts, and no Back-Cover Texts.
A copy of the license is included in the section entitled "GNU Free Documentation License".
'''

'''
This script is provided as is with no guarantees.
It is meant to be used on clusters deployed with Ambari and using Ambari version 2.0.0 or higher, in order
to identify any inconsistencies in the database related to the version of components for the HDP stack.
The script will pinpoint problems, and ask the user to take correct action to update their database.
As of this version, this script only supports MySQL and Postgres.

Further, this script must be ran from the host with Ambari-Server.
'''

# System imports
import sys
import os
import logging
import signal       # used to handle SIGINT and SIGTERM
import subprocess   # used to check if ambari-server is running
import re           # used to check if ambari-server is running when running regex on output


from optparse import OptionParser

Logger = logging.getLogger()

AMBARI_PROPERTIES_LOCATION = "/etc/ambari-server/conf/ambari.properties"

MIN_AMBARI_VERSION = "2.0.0"

class DB_TYPE:
  MYSQL = "MYSQL"
  POSTGRES = "POSTGRES"

class RUMagician:
  """
  Rolling Upgrade Magician analyzes the database to find and correct any issues.
  It is a terminal-driven application, that prompts the user to select options.
  """

  def __init__(self, argv):
    parser = OptionParser()
    parser.add_option("-v", "--verbose", action="store_true", dest="verbose", default=False)

    (self.options, self.args) = parser.parse_args(argv)

    # Log to stdout
    logging_level = logging.DEBUG if self.options.verbose else logging.INFO
    Logger.setLevel(logging_level)
    ch = logging.StreamHandler(sys.stdout)
    ch.setLevel(logging_level)
    formatter = logging.Formatter('%(levelname)s - %(message)s')
    ch.setFormatter(formatter)
    Logger.addHandler(ch)

    # Handle terminations gracefully
    signal.signal(signal.SIGTERM, self.terminate)
    signal.signal(signal.SIGINT, self.terminate)

    self.print_usage()
    self.configure()

    if not self.check_ambari_server_process_down():
      Logger.info("Ambari Server cannot be running while we make database updates. Please call \"ambari-server stop\" and try running this script again.")
      self.terminate()

    stage = None
    while stage is None:
      question = "What stage of Stack Upgrade are you in? Enter a number.\n" \
                 "1) Just upgraded to Ambari {0} or higher, and do not see your stack version in the Versions page.\n".format(MIN_AMBARI_VERSION) + \
                 "2) Registered a Repository and Installed Packages.\n" \
                 "3) Performed an automated/manual upgrade and want to force it to the version I want.\n"

      stage = self.ask_question(question, numeric=True)
      try:
        if stage:
          stage = int(stage)
      except ValueError, e:
        stage = None

      if stage == 1:
        Logger.info("Selected {0}. Will verify that exactly one version exists and that it is marked as CURRENT.".format(stage))
        self.check_exactly_one_current_version()
      elif stage == 2:
        Logger.info("Selected {0}. Will check that all Repository Versions are consistent.".format(stage))
        self.check_repo_versions()
        self.check_installation()
      elif stage == 3:
        Logger.info("Selected {0}. Will confirm that the cluster is in a good shape to finalize.".format(stage))
        self.finalize()
      else:
        Logger.error("Invalid option \"{0}\", please try again.".format(stage))
        stage = None

  def ask_question(self, question, numeric=False):
    """
    :param question: Question to prompt.
    :param numeric: If True, will cast to an integer.
    :return: Returns the answer to the question, or None if not a complete string or couldn't cast to an integer.
    If the user enters "q" at any time, will quit.
    """
    answer = raw_input(question)
    if answer and answer != "":
      answer = answer.strip()
      if answer.lower() == "q":
        Logger.info("Bye bye...")
        self.terminate()  # this will close any open database connections

    if answer == "":
      answer = None

    if numeric and answer is not None:
      try:
        answer = int(answer)
      except Exception:
        answer = None
    return answer

  def print_license(self):
    # LGPL License header
    license = "Permission is granted to copy, distribute and/or modify this document\n" \
              "under the terms of the GNU Free Documentation License, Version 1.3\n" \
              "or any later version published by the Free Software Foundation;\n" \
              "with no Invariant Sections, no Front-Cover Texts, and no Back-Cover Texts.\n" \
              "A copy of the license is included in the section entitled \"GNU Free Documentation License\".\n\n"
    print(license)

  def print_usage(self):
    self.print_license()

    msg = "\n*********************************************************************\n" \
          "This script tries to find inconsistencies in your cluster\n" \
          "while registering and installing versions.\n" \
          "It will try to fix any problems,\n" \
          "and assumes that you have Ambari {0} or higher,\n".format(MIN_AMBARI_VERSION) + \
          "and are trying to perform either a manual or automatic stack upgrade.\n" \
          "IMPORTANT, this script must be ran from the host with Ambari Server.\n" \
          "Further, please take a database backup before continuing.\n" \
          "To exit, enter \"q\"\n" \
          "*********************************************************************\n"
    Logger.info(msg)

  def terminate(self, signum=None, stack=None):
    """
    Exit gracefully, closing any option file handles or connections.
    It is important to use print statements instead of Logging statements in case that the logger is not yet
    initialized.
    :param signum: Usually SIGTERM, SIGTINT, or None (if user entered "q").
    :param stack: Stack trace
    """
    if signum:
      print("Caught termination signal {0}. Will exit gracefully.".format(signum))
    if hasattr(self, "cursor") or hasattr(self, "conn"):
      try:
        print("Will try to close database connection.")
        if hasattr(self, "cursor") and self.cursor:
          self.cursor.close()
        if hasattr(self, "conn") and self.conn:
          self.conn.close()
        print("Closed database connection successfully.")
      except Exception, e:
        print("Unable to close database connection. Error: {0}\n".format(e.message))
    sys.exit(0)

  def check_ambari_server_process_down(self):
    """
    Before running any DB commands, ensure that Ambari Server is not running.
    :return: Return True if ambari-server is not running, otherwise, False.
    """
    process_name = "ambari-server"
    output = self.__find_process(process_name)
    return re.search(process_name, output) is None

  def __find_process(self, process_name):
    ps = subprocess.Popen("ps -ef | grep {0} | grep -v grep".format(process_name), shell=True, stdout=subprocess.PIPE)
    output = ps.stdout.read()
    #Logger.debug("Checking if process {0} is running. Output: {1}.\n".format(process_name, output))
    ps.stdout.close()
    ps.wait()
    return output

  def configure(self):
    """
    Read configurations and ensure can connect to database.
    """
    # Defaults
    self.db_type = DB_TYPE.POSTGRES
    self.db_name = "ambari"
    self.db_user = "ambari"
    self.db_password = "bigdata"
    self.db_host = "localhost"
    self.db_url = None

    if os.path.exists(AMBARI_PROPERTIES_LOCATION):
      self.ambari_props = self.read_conf_file(AMBARI_PROPERTIES_LOCATION)

      if "server.jdbc.database" in self.ambari_props:
        self.db_type = self.ambari_props["server.jdbc.database"].upper()
      if "server.jdbc.database_name" in self.ambari_props:
        self.db_name = self.ambari_props["server.jdbc.database_name"]
      if "server.jdbc.user.name" in self.ambari_props:
        self.db_user = self.ambari_props["server.jdbc.user.name"]
      if "server.jdbc.user.passwd" in self.ambari_props:
        self.db_password = self.read_file(self.ambari_props["server.jdbc.user.passwd"])
      if "server.jdbc.hostname" in self.ambari_props:
        self.db_host = self.ambari_props["server.jdbc.hostname"]
      if "server.jdbc.url" in self.ambari_props:
        self.db_url = self.ambari_props["server.jdbc.url"]

      Logger.info("Using database type: {0}, name: {1}, host: {2}".format(self.db_type, self.db_name, self.db_host))
      connection_string = "dbname='{0}' user='{1}' host='{2}' password='{3}'".format(self.db_name, self.db_user, self.db_host, self.db_password)

      if self.db_type == DB_TYPE.POSTGRES:
        try:
          import psycopg2     # covered by GNU Lesser General Public License
        except Exception, e:
          Logger.error("Need to install python-psycopg2 package for Postgres DB. E.g., yum install python-psycopg2\n")
          self.terminate()
      elif self.db_type == DB_TYPE.MYSQL:
        try:
          import pymysql      # covered by MIT License
        except Exception, e:
          Logger.error("Need to install PyMySQL package for Python. E.g., yum install python-setuptools && easy_install pip && pip install PyMySQL\n")
          self.terminate()
      else:
        Logger.error("Unknown database type: {0}.".format(self.db_type))
        self.terminate()

      self.conn = None
      self.cursor = None
      try:
        Logger.debug("Initializing database connection and cursor.")
        if self.db_type == DB_TYPE.POSTGRES:
          self.conn = psycopg2.connect(connection_string)
          self.cursor = self.conn.cursor()
        elif self.db_type == DB_TYPE.MYSQL:
          self.conn = pymysql.connect(self.db_host, self.db_user, self.db_password, self.db_name)
          self.cursor = self.conn.cursor()

        Logger.debug("Created database connection and cursor.")
        self.cursor.execute("SELECT metainfo_key, metainfo_value FROM metainfo WHERE metainfo_key='version';")
        rows = self.cursor.fetchall()
        if rows and len(rows) == 1:
          self.ambari_version = rows[0][1]
          Logger.info("Connected to database!!! Ambari version is {0}\n".format(self.ambari_version))

          # Must be Ambari 2.0.0 or higher
          if self.compare_versions(self.ambari_version, MIN_AMBARI_VERSION) < 0:
            Logger.error("Must be running Ambari Version {0} or higher.\n".format(MIN_AMBARI_VERSION))
            self.terminate()
        else:
          Logger.error("Unable to determine Ambari version.")
          self.terminate()

        self.set_cluster()
      except Exception, e:
        Logger.error("I am unable to connect to the database. Error: {0}\n".format(e))
        self.terminate()
    else:
      raise Exception("Could not find file {0}".format(AMBARI_PROPERTIES_LOCATION))

  def read_conf_file(self, file_path):
    """
    Parse the configuration file, and return a dictionary of key, value pairs.
    Ignore any lines that begin with #
    :param file_path: Properties file to parse.
    :return: Dictionary with key, value pairs.
    """
    ambari_props = {}
    if os.path.exists(file_path):
      with open(file_path, "r") as f:
        lines = f.readlines()
        if lines:
          Logger.debug("Reading file {0}, has {1} lines.".format(file_path, len(lines)))
          for l in lines:
            l = l.strip()
            if l.startswith("#"):
              continue
            parts = l.split("=")
            if len(parts) >= 2:
              prop = parts[0]
              value = "".join(parts[1:])
              ambari_props[prop] = value
    return ambari_props

  def read_file(self, file_path):
    """
    :param file_path: File to read. Typically the ambari database password file.
    :return: Return the contents of the file
    """
    if os.path.exists(file_path):
      with open(file_path, "r") as f:
        lines = f.readlines()
        return "\n".join(lines)
    return None

  def compare_versions(self, version1, version2):
    """
    Used to compare  Ambari Versions.
    E.g., Ambari version 2.0.1 vs 2.1.1,
    :param version1: First parameter for version
    :param version2: Second parameter for version
    :return: Returns -1 if version1 is before version2, 0 if they are equal, and 1 if version1 is after version2
    """
    max_segments = max(len(version1.split(".")), len(version2.split(".")))
    return cmp(self.__normalize_version(version1, desired_segments=max_segments), self.__normalize_version(version2, desired_segments=max_segments))

  def __normalize_version(self, v, desired_segments=0):
    """
    :param v: Input string of the form "#.#.#" or "#.#.#.#"
    :param desired_segments: If greater than 0, and if v has fewer segments this parameter, will pad v with segments
    containing "0" until the desired segments is reached.
    :return: Returns a list of integers representing the segments of the version
    """
    v_list = v.split(".")
    if desired_segments > 0 and len(v_list) < desired_segments:
      v_list = v_list + ((desired_segments - len(v_list)) * ["0", ])
    return [int(x) for x in v_list]

  def set_cluster(self):
    self.cluster_id = None
    self.cluster_name = None
    try:
      query = "SELECT cluster_id, cluster_name FROM clusters ORDER BY cluster_name;"
      self.cursor.execute(query)
      rows = self.cursor.fetchall()
      if rows:
        if len(rows) == 1:
          if len(rows[0]) == 2:
            self.cluster_id = int(rows[0][0])
            self.cluster_name = rows[0][1]
        elif len(rows) > 1:
          # Found multiple clusters
          question = "We found multiple cluster names, which one should we use?\n"
          for i in range(0, len(rows)):
            question += "{0}) {1}\n".format(i+1, rows[i][1])

          answer = None
          while answer is None:
            answer = self.ask_question(question, numeric=True)
            if answer > 0 and answer <= len(rows):
              self.cluster_id = int(rows[answer- 1][0])
              self.cluster_name = rows[answer - 1][1]
            else:
              Logger.error("Invalid option \"{0}\", please try again.\n".format(answer))
              answer = None
        pass

        if self.cluster_name is None:
          Logger.error("Unable to determine the cluster name.\n")
          self.terminate()
        else:
          Logger.info("Selected cluster name: {0}\n".format(self.cluster_name))
      else:
        Logger.error("Unable to get cluster from query: {0}\n".format(query))
        self.terminate()
    except Exception, e:
      Logger.error("Caught an exception. Error: {0}\n".format(e.message))
      self.terminate()

  def check_exactly_one_current_version(self):
    """
    If there are no cluster_version records, or host_version records, the user will have to restart at least one component
    that can advertise a version. Ideally, they need to restart all services.

    If there is exactly one cluster_version, and every host_version record corresponds to the same repo_version,
    then need to ensure that all of these entities have a state of CURRENT.
    If not, prompt user if they want to change all to CURRENT.
    """
    expected_state = "CURRENT"

    query = "SELECT COUNT(*) FROM cluster_version;"
    self.cursor.execute(query)
    result = self.cursor.fetchone()
    if result is None or len(result) != 1:
      Logger.error("Unable to run query: {0}".format(query))
      return

    count = result[0]
    if count == 0:
      msg = "There are no cluster_versions. Start ambari-server, and then perform a Restart on one of the services.\n" + \
        "Then navigate to the \"Stacks and Versions > Versions\" page and ensure you can see the stack version.\n" + \
        "Next, restart all services, one-by-one, so that Ambari knows what version each component is running."
      Logger.warning(msg)
    elif count == 1:
      query = "SELECT rv.repo_version_id, rv.version, cv.state FROM cluster_version cv JOIN repo_version rv ON cv.repo_version_id = rv.repo_version_id;"
      self.cursor.execute(query)
      result = self.cursor.fetchone()

      repo_version_id = None
      repo_version = None
      cluster_version_state = None

      if result and len(result) == 3:
        repo_version_id = result[0]
        repo_version = result[1]
        cluster_version_state = result[2]

      if repo_version_id and repo_version and cluster_version_state:
        if cluster_version_state.upper() == expected_state:
          Logger.info("Everything looks correct. Cluster Version {0} has a state of {1}.".format(repo_version, cluster_version_state))
          self.check_all_hosts_current(repo_version_id, repo_version)
        else:
          Logger.error("Cluster Version {0} should have a state of {1} but is {2}. Make sure to restart all of the Services.".format(repo_version, expected_state, cluster_version_state))
          # TODO, can we force it to CURRENT? And all Host Versions too?
      else:
        Logger.error("Unable to run query: {0}".format(query))
    elif count > 1:
      # Ensure at least one Cluster Version is CURRENT
      Logger.info("Found multiple Cluster versions, checking that exactly one is {0}.".format(expected_state))
      query = "SELECT rv.repo_version_id, rv.version, cv.state FROM cluster_version cv JOIN repo_version rv ON cv.repo_version_id = rv.repo_version_id WHERE cv.state = '{0}';".format(expected_state)
      self.cursor.execute(query)
      rows = self.cursor.fetchall()
      if rows:
        if len(rows) == 1:
          Logger.info("Good news; Cluster Version {0} has a state of {1}.".format(rows[0][1], expected_state))
          self.check_all_hosts_current(rows[0][0], rows[0][1])
        elif len(rows) > 1:
          # Take the repo_version's version column
          repo_versions = [row[1] for row in rows if len(row) == 3]
          Logger.error("Found multiple cluster versions with a state of {0}, but only one should be {0}.\n" \
                       "Will need to fix this manually, please contact Support. Cluster Versions found: {1}".format(expected_state, ", ".join(repo_versions)))
      else:
        Logger.error("Unable to run query: {0}\n".format(query))
    pass

  def check_all_hosts_current(self, repo_version_id, version_name):
    """
    Ensure that all of the hosts in the cluster have a state of CURRENT for the host_version that corresponds to the id.
    :param repo_version_id: repo_version table's repo_version_id column
    :param version_name: repo_version table's version column
    """
    if self.compare_versions(self.ambari_version, "2.1.0") < 0:
      query1 = "SELECT chm.host_name from ClusterHostMapping chm JOIN clusters c ON c.cluster_name = '{0}';".format(self.cluster_name)
    else:
      query1 = "SELECT h.host_name from ClusterHostMapping chm JOIN clusters c ON c.cluster_name = '{0}' JOIN hosts h ON chm.host_id = h.host_id;".format(self.cluster_name)

    if self.compare_versions(self.ambari_version, "2.1.0") < 0:
      query2 = "SELECT hv.host_name, hv.state FROM host_version hv WHERE hv.repo_version_id = {0};".format(repo_version_id)
    else:
      query2 = "SELECT h.host_name, hv.state FROM hosts h JOIN host_version hv ON h.host_id = hv.host_id WHERE hv.repo_version_id = {0};".format(repo_version_id)

    # All cluster hosts
    host_names = set()
    self.cursor.execute(query1)
    rows = self.cursor.fetchall()
    if self.options.verbose:
      Logger.debug(query1 + "\n")
    if rows and len(rows) > 0:
      host_names = set([row[0] for row in rows if len(row) == 1])
      Logger.debug("Hosts: {0}".format(", ".join(host_names)))

    host_name_to_state = {} # keys should be a subset of host_names
    hosts_with_repo_version_state_not_in_current = set()
    self.cursor.execute(query2 + "\n")
    rows = self.cursor.fetchall()
    Logger.debug(query2)
    if rows and len(rows) > 0:
      for row in rows:
        if len(row) == 2:
          host_name = row[0]
          state = row[1]
          host_name_to_state[host_name] = state
          if state.upper() != "CURRENT":
            hosts_with_repo_version_state_not_in_current.add(host_name)

    host_names_with_version = set(host_name_to_state.keys())
    host_names_without_version = host_names - host_names_with_version

    if len(host_names) > 0:
      if len(host_names_without_version) > 0:
        Logger.error("{0} host(s) do not have a Host Version for Repo Version {1}.\n" \
                     "Host(s):\n{2}\n".
                     format(len(host_names_without_version), version_name, ", ".join(host_names_without_version)))

      if len(hosts_with_repo_version_state_not_in_current) > 0:
        Logger.error("{0} host(s) have a Host Version for Repo Version {1} but the state is not CURRENT.\n" \
                     "Host(s):\n{2}\n".
                     format(len(hosts_with_repo_version_state_not_in_current), version_name, ", ".join(hosts_with_repo_version_state_not_in_current)))

      if len(host_names_without_version) == 0 and len(hosts_with_repo_version_state_not_in_current) == 0:
        Logger.info("Found {0} host(s) in the cluster, and all have a Host Version of CURRENT for " \
                    "Repo Version {1}. Things look good.\n".format(len(host_names), version_name))
      else:
        Logger.error("Make sure that all of these hosts are heartbeating, that they have the packages installed, the\n" \
          "hdp-select symlinks are correct, and that the services on these hosts have been restarated.\n")
    pass

  def check_repo_versions(self):
    """
    Check that all display names are unique, that all versions are unique.
    """
    query = "SELECT version, display_name FROM repo_version;"
    self.cursor.execute(query)
    rows = self.cursor.fetchall()
    if rows is None:
      Logger.warning("Did not find any Repo Versions, nothing to do. Query: {0}".format(query))
      return

    if len(rows) == 0:
      Logger.info("There are no Repo Versions, nothing to do.")
    elif len(rows) > 0:
      # Dictionary from the key to the number of occurrences
      version_name_to_count = {}
      display_name_to_count = {}
      for row in rows:
        if len(row) == 2:
          version = row[0]
          display_name = row[1]

          if version in version_name_to_count:
            version_name_to_count[version] += 1
          else:
            version_name_to_count[version] = 1

          if display_name in display_name_to_count:
            display_name_to_count[display_name] += 1
          else:
            display_name_to_count[display_name] = 1

      # If multiple occurrences are found, report a problem.
      problematic_versions = set()
      for k, v in version_name_to_count.iteritems():
        if v > 1:
          problematic_versions.add(k)

      problematic_display_names = set()
      for k, v in display_name_to_count.iteritems():
        if v > 1:
          problematic_display_names.add(k)

      found_error = False
      if len(problematic_versions) > 0:
        found_error = True
        Logger.error("The following version(s) exist multiple times in the Repo Versions: {0}".format(", ".join(problematic_versions)))
      if len(problematic_display_names) > 0:
        found_error = True
        Logger.error("The following display name(s) exist multiple times in the Repo Versions: {0}".format(", ".join(problematic_display_names)))

      if found_error:
        Logger.error("You will have to correct this error manually by ensuring uniqueness.")
      else:
        Logger.info("Looks good; all of the Repo Versions have unique display names and versions.")
    pass

  def check_installation(self):
    """
    If any Cluster Versions are stuck in INSTALLING, ask the user if they want to
    retry (transition to INSTALL_FAILED), or force it (transition to INSTALLED).
    """
    query = "SELECT rv.repo_version_id, rv.version, rv.display_name, cv.state FROM cluster_version cv JOIN repo_version rv ON cv.repo_version_id = rv.repo_version_id ORDER BY rv.repo_version_id;"
    self.cursor.execute(query)
    rows = self.cursor.fetchall()
    if rows is None or len(rows) == 0:
      Logger.warning("Did not find any Cluster Versions, nothing to do. Query: {0}".format(query))
      return

    for row in rows:
      if len(row) == 4:
        repo_version_id = row[0]
        version = row[1]
        display_name = row[2]
        state = row[3]

        if state.upper() == "INSTALLING":
          question = "Repo Version {0} ({1}) is stuck in INSTALLING. ".format(display_name, version) + \
          "Do you want to,\n" \
          "1) Leave as is (DEFAULT)\n" \
          "2) Retry installation\n" \
          "3) Force it to Installed (WARNING: Only if you are certain you have installed the bits already!!!)\n"

          answer = self.ask_question(question, numeric=True)
          desired_state = None
          if answer is None or answer == 1:
            Logger.info("  Ignoring, moving on...")
          elif answer == 2:
            Logger.info("  Retrying installation")
            desired_state = "INSTALL_FAILED"
          elif answer == 3:
            Logger.info("  Forcing to INSTALLED")
            desired_state = "INSTALLED"

          if desired_state is not None:
            statement1 = "UPDATE cluster_version SET state = '{0}' WHERE repo_version_id={1};".format(desired_state, repo_version_id)
            statement2 = "UPDATE host_version SET state = '{0}' WHERE repo_version_id={1};".format(desired_state, repo_version_id)

            Logger.info("Will run update statements:\n{0}\n{1}\n".format(statement1, statement2))
            self.cursor.execute(statement1)
            self.cursor.execute(statement2)
            self.conn.commit()
        elif state.upper() in ["UPGRADING", "UPGRADED"]:
          self.__handle_cluster_version_in_upgrading_or_upgraded(repo_version_id, version, display_name, state)
    pass

  def __handle_cluster_version_in_upgrading_or_upgraded(self, repo_version_id, version, display_name, state):
    """
    In some cases, installing hdp-select can flip the symlinks, so that restarting components will actually
    cause them to start on the newer version without the user's knowledge.
    In this case, the version in UPGRADING/UPGRADED has not been used in an upgrade yet, so we must revert
    to a good known state if the user intends on making an automated RU.
    Before doing so, the user will have to call "hdp-select set all <current_version>" on all affected hosts.
    """
    attempted_rolling_upgrade = False

    query1 = "SELECT COUNT(*) FROM upgrade WHERE to_version = '{0}';".format(version)
    self.cursor.execute(query1)
    row = self.cursor.fetchone()
    if row and len(row) == 1:
      attempted_rolling_upgrade = True if int(row[0]) > 0 else False
    else:
      Logger.error("Unable to run query: {0}".format(query1))
      return

    if not attempted_rolling_upgrade:
      host_names_already_upgrading = set()

      if self.compare_versions(self.ambari_version, "2.1.0") < 0:
        query2 = "SELECT hv.host_name, hv.state FROM host_version hv WHERE hv.repo_version_id = {0} AND hv.state IN ('UPGRADING', 'UPGRADED');".format(repo_version_id)
      else:
        query2 = "SELECT h.host_name, hv.state FROM hosts h JOIN host_version hv ON h.host_id = hv.host_id WHERE hv.repo_version_id = {0} AND hv.state IN ('UPGRADING', 'UPGRADED');".format(repo_version_id)

      self.cursor.execute(query2)
      rows = self.cursor.fetchall()
      if rows and len(rows) > 0:
        host_names_already_upgrading = set([row[0] for row in rows if len(row) == 2])

      question = None
      answer = None
      if len(host_names_already_upgrading) > 0:
        question = "We noticed that you did not launch an Automated Rolling Upgrade to version {0},\n" \
                   "but its state is {1} and some host(s) are already upgrading to it.\n" \
                   "Did you intend to perform a manual upgrade already?\n" \
                   "1) Yes, I already started changing versions manually. Leave it as is.\n" \
                   "2) No, should not have started yet. Please revert it back to INSTALLED.\n".format(version, state)
      else:
        question = "We noticed that you did not launch an Automated Rolling Upgrade to version {0},\n" \
                   "but its state is {1}.\n" \
                   "Did you intend to perform a manual upgrade?\n" \
                   "1) Yes, I already started changing versions manually. Leave it as is.\n" \
                   "2) No, should not have started yet. Please revert it back to INSTALLED.\n".format(version, state)

      while answer is None:
        answer = self.ask_question(question, numeric=True)
        if answer == 1:
          Logger.info("Ok\n")
        elif answer == 2:
          current_cluster_version = self.__get_current_cluster_version()
          if current_cluster_version is not None and "version" in current_cluster_version:
            Logger.info("IMPORTANT: We are forcing the Cluster Version and Host Versions back to INSTALLED. Before starting ambari-server,\n" \
                        "please ensure that all hosts are indeed on the original version, by running \"hdp-select set all {0}\" on each host.\n".format(current_cluster_version["version"]))
          else:
            Logger.error("Unable to find a Cluster Version that is CURRENT\n.")
            return

          statement1 = "UPDATE cluster_version SET state = 'INSTALLED' WHERE repo_version_id = {0} AND state = '{1}';".format(repo_version_id, state)
          statement2 = "UPDATE host_version SET state = 'INSTALLED' WHERE repo_version_id = {0} AND state IN ('UPGRADING', 'UPGRADED');".format(repo_version_id)
          statement3 = "UPDATE hostcomponentstate set version = '{0}' WHERE version = '{1}';".format(current_cluster_version["version"], version)
          statement4 = "UPDATE hostcomponentstate set upgrade_state = 'NONE';"

          Logger.info("Will run update statements:\n{0}\n{1}\n{2}\n{3}\n".format(statement1, statement2, statement3, statement4))
          self.cursor.execute(statement1)
          self.cursor.execute(statement2)
          self.cursor.execute(statement3)
          self.cursor.execute(statement4)
          self.conn.commit()
        else:
          Logger.error("Invalid option \"{0}\", please try again.\n".format(answer))
          answer = None

  def __get_current_cluster_version(self):
    """
    :return: Return a dictionary with stats from the Cluster Version whose state is CURRENT. If one doesn't exist, return None.
    """
    query = "SELECT rv.repo_version_id, rv.version, rv.display_name FROM cluster_version cv JOIN repo_version rv ON cv.repo_version_id = rv.repo_version_id WHERE cv.state = 'CURRENT';"
    self.cursor.execute(query)
    row = self.cursor.fetchone()
    if row and len(row) == 3:
      return {"repo_version_id": row[0],
              "version": row[1],
              "display_name": row[2]}
    Logger.warning("Unable to run query: {0}\n".format(query))
    return None

  def finalize(self):
    """
    Check any versions in UPGRADING/UPGRADED, ask the user to pick the one they want CURRENT, and all others
    will go to INSTALLED.
    This will also update the hostcomponentstate with that version for all components that advertise a version.
    Further, it will set the upgrade_state to None.
    Next, it will update the host_version and cluster_version accordingly.
    And pick the CURRENT cluster version, and change it to INSTALLED.
    If this was a manual stack upgrade, will need to fix the Current and Desired Stack version.
    If there are any pending RU requests, will mark them as ABORTED.
    """
    query = "SELECT rv.repo_version_id, rv.version, rv.display_name, cv.state FROM cluster_version cv JOIN repo_version rv ON cv.repo_version_id = rv.repo_version_id ORDER BY rv.repo_version_id;"
    self.cursor.execute(query)
    rows = self.cursor.fetchall()
    if rows is None or len(rows) == 0:
      Logger.warning("Did not find any Cluster Versions, nothing to do. Query: {0}".format(query))
      return

    current_cluster_version = self.__get_current_cluster_version()
    if current_cluster_version is None or "version" not in current_cluster_version:
      Logger.error("Unable to find a Cluster Version that is CURRENT. Please resolve this with help from support.\n")
      return

    question1 = "Which Cluster Version would you like to make CURRENT?\n"
    for i in range(0, len(rows)):
      version = rows[i][1]
      state = rows[i][3]
      question1 += "{0}) {1} in state {2}\n".format(i+1, version, state)

    answer1 = None
    while answer1 is None:
      answer1 = self.ask_question(question1, numeric=True)
      if answer1 >= 1 and answer1 <= len(rows):
        desired_current_version = rows[answer1 - 1][1]

        if current_cluster_version["version"] == desired_current_version:
          Logger.info("Version {0} is already CURRENT, nothing to do.\n".format(desired_current_version))
        else:
          question2 = "Will make version {0} CURRENT, and change version {1} to INSTALLED. Is that your final answer?\n" \
                      "1) Yes. I already ran or plan to run \"HDFS Finalize\".\n" \
                      "2) No\n".format(desired_current_version, current_cluster_version["version"])
          answer2 = None
          while answer2 is None:
            answer2 = self.ask_question(question2, numeric=True)
            if answer2 == 1:
              Logger.info("Your wish is my command!\n")
              self.__finalize_version(current_cluster_version, desired_current_version)
            elif answer2 == 2:
              Logger.info("Ok, we'll leave it as is.\n")
            else:
              Logger.error("Invalid option \"{0}\", please try again.\n".format(answer2))
              answer2 = None
      else:
        Logger.error("Invalid option \"{0}\", please try again.\n".format(answer1))
        answer1 = None

  def __finalize_version(self, current_cluster_version, desired_cluster_version):
    """
    Finalize a version by marking it as CURRENT
    :param current_cluster_version: Dictionary with information about the current version.
    :param desired_cluster_version: String of version that should be marked as CURRENT.
    """
    if current_cluster_version is None or "version" not in current_cluster_version:
      Logger.error("Could not finalize to version {0} because did not receive a Cluster Version that is CURRENT.".format(desired_cluster_version))
      return

    if current_cluster_version["version"] == desired_cluster_version:
      Logger.info("Asking to finalize to a version that is already CURRENT, nothing to do.\n")
      return

    # Get the repo_version_id for the desired object
    repo_version = self.__get_repo_version(desired_cluster_version)
    if repo_version is None or "repo_version_id" not in repo_version:
      Logger.error("Unable to query Repo Version {0}.\n".format(desired_cluster_version))
      return

    statements = []
    statements.append("UPDATE cluster_version SET state = 'CURRENT' WHERE repo_version_id = {0};".format(repo_version["repo_version_id"]))
    statements.append("UPDATE host_version SET state = 'CURRENT' WHERE repo_version_id = {0};".format(repo_version["repo_version_id"]))
    statements.append("UPDATE hostcomponentstate set version = '{0}' WHERE version NOT IN ('{0}', 'UNKNOWN');".format(desired_cluster_version))
    statements.append("UPDATE hostcomponentstate set upgrade_state = 'NONE';")
    statements.append("UPDATE cluster_version SET state = 'INSTALLED' WHERE repo_version_id = {0} AND state = 'CURRENT';".format(current_cluster_version["repo_version_id"]))
    statements.append("UPDATE host_version SET state = 'INSTALLED' WHERE repo_version_id = {0} AND state = 'CURRENT';".format(current_cluster_version["repo_version_id"]))

    upgrade_in_progress = self.__get_last_upgrade_id_for_repo_version(desired_cluster_version)
    additional_statements = self.__get_update_statements_for_tasks_in_progress(upgrade_in_progress)
    statements += additional_statements

    # Upgrade the stack
    if self.compare_versions(self.ambari_version, "2.1.0") < 0:
      '''
      # In Ambari 2.0, it was not possible to do major stack upgrades, so several columns will retain HDP 2.2 as the stackVersion.
      E.g.,
      _Table____________________|_Column________________|_Value____________________________________
      clusters                  | desired_stack_version | {"stackName":"HDP","stackVersion":"2.2"}
      clusterstate              | current_stack_version | {"stackName":"HDP","stackVersion":"2.2"}
      hostcomponentstate        | current_stack_version | {"stackName":"HDP","stackVersion":"2.2"}
      hostcomponentdesiredstate | desired_stack_version | {"stackName":"HDP","stackVersion":"2.2"}
      '''
      pass
    else:
      statements.append("UPDATE clusters SET desired_stack_id = {0} WHERE cluster_id = {1};".format(repo_version["stack_id"], self.cluster_id))
      statements.append("UPDATE clusterstate SET current_stack_id = {0} WHERE cluster_id = {1};".format(repo_version["stack_id"], self.cluster_id))

    Logger.info("Will run update statements:\n" + "\n".join(statements) + "\n")

    try:
      for statement in statements:
        self.cursor.execute(statement)
      self.conn.commit()
      Logger.info("Success, enjoy the rest of your day!\n")
    except Exception, e:
      Logger.error("Unable to run update statements to finalize version {0}. Error: {1}\n".format(desired_cluster_version, e.message))

  def __get_repo_version(self, version):
    """
    Get the Repo Version object whose version matches.
    :param version: Version string, e.g., 2.3.0.0-1234
    :return: Return the a dictionary with the parameters of the object.
    """
    if self.compare_versions(self.ambari_version, "2.1.0") < 0:
      query = "SELECT repo_version_id, version, display_name FROM repo_version WHERE version = '{0}';".format(version)
    else:
      query = "SELECT repo_version_id, version, display_name, stack_id FROM repo_version WHERE version = '{0}';".format(version)

    self.cursor.execute(query)
    rows = self.cursor.fetchall()
    if rows is not None or len(rows) == 1:
      obj = {}
      if len(rows[0]) >= 3:
        obj = {"repo_version_id": rows[0][0],
               "version": rows[0][1],
               "display_name": rows[0][2]}
      if len(rows[0]) >= 4:
        obj["stack_id"] = rows[0][3]
      return obj
    Logger.warning("Unable to run query, or generated multiple rows: {0}".format(query))
    return None

  def __get_last_upgrade_id_for_repo_version(self, version):
    """
    Get the most recent upgrade_id for the Upgrade record whose to_version matches the version.
    :param version: Version string, e.g., 2.3.0.0-1234
    :return: Return the a dictionary with the parameters of the object.
    """
    query = "SELECT u.upgrade_id, u.direction, u.from_version, u.to_version FROM upgrade u JOIN repo_version rv " \
            "ON u.to_version = rv.version JOIN cluster_version cv ON rv.repo_version_id = cv.repo_version_id AND " \
            "cv.cluster_id = {0} AND u.to_version = '{1}' AND cv.state IN ('UPGRADING', 'UPGRADED') " \
            "ORDER BY u.upgrade_id DESC;".format(self.cluster_id, version)

    self.cursor.execute(query)
    rows = self.cursor.fetchall()
    if rows is not None and len(rows) == 1:
      obj = {}
      if len(rows[0]) == 4:
        obj = {"upgrade_id": rows[0][0],
               "direction": rows[0][1],
               "from_version": rows[0][2],
               "from_version": rows[0][3]}
        return obj
    return None

  def __get_update_statements_for_tasks_in_progress(self, upgrade_in_progress):
    """
    When an RU is in progress, need to abort any host_role_command records for it that are still PENDING, QUEUED, IN_PROGRESS, HOLDING.
    :param upgrade_in_progress: Dictionary of the upgrade in progress, which contains an "upgrade_id"
    :return: List of update statements to run, or empty list if no statements could be generated.
    """
    statements = []
    if upgrade_in_progress and "upgrade_id" in upgrade_in_progress:
      # There should only be one request, which is used for all of the host_role_command records as part of either the
      # upgrade or downgrade.
      query = "SELECT DISTINCT(hrc.request_id) FROM upgrade u " \
              "JOIN upgrade_group g ON u.upgrade_id = g.upgrade_id " \
              "JOIN upgrade_item i ON g.upgrade_group_id = i.upgrade_group_id " \
              "JOIN host_role_command hrc ON i.stage_id = hrc.stage_id AND u.request_id = hrc.request_id " \
              "WHERE u.upgrade_id = {0} LIMIT 1;".format(upgrade_in_progress["upgrade_id"])

      self.cursor.execute(query)
      row = self.cursor.fetchone()
      if row and len(row) == 1:
        request_id = row[0]
        statements.append("UPDATE host_role_command SET status = 'ABORTED' WHERE request_id = {0} AND status IN ('PENDING', 'QUEUED', 'IN_PROGRESS', 'HOLDING');".format(request_id))
    return statements


if __name__ == '__main__':
  magician = RUMagician(sys.argv)
