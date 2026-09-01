#!/usr/bin/env ambari-python-wrap
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

import argparse
import json
import urllib.request, urllib.parse
import re
from os import path
import xml.etree.ElementTree as ET

json_stack_version_re = re.compile(r"(\S*)-((\d\.*)+)")

family_map = {"redhat6": "centos6", "redhat7": "centos7"}

URL_OPEN_TIMEOUT_SECONDS = 30

XML_HEADER = """<?xml version="1.0"?>
<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
"""


def get_json_content(source):
  if urllib.parse.urlparse(source).scheme:
    with urllib.request.urlopen(
      source, timeout=URL_OPEN_TIMEOUT_SECONDS
    ) as response:
      content = response.read()
    if isinstance(content, bytes):
      content = content.decode("utf-8")
    return json.loads(content)

  with open(source, encoding="utf-8") as stream:
    return json.load(stream)


def replace_url_in_repoinfo_xml(repoinfo_xml_path, repo_id, repo_info):
  tree = ET.parse(repoinfo_xml_path)
  root = tree.getroot()
  for os_tag in root.findall("os"):
    family = os_tag.get("family", None)
    # hack, in hdp_urlinfo.json we have centos, but in repoinfo.xml it mapped to redhat
    family = family_map[family] if family in family_map else family
    for repo_tag in os_tag.findall("repo"):
      repo_id_tag = repo_tag.find("repoid")
      if repo_id_tag is not None and repo_id_tag.text == repo_id:
        baseurl_tag = repo_tag.find("baseurl")
        if baseurl_tag is not None and family in repo_info:
          print(
            f"URLINFO_PROCESSOR: replacing {baseurl_tag.text} to {repo_info[family]} for repo id:{repo_id} and family:{family}"
          )
          baseurl_tag.text = repo_info[family]

  with open(repoinfo_xml_path, "w") as out:
    out.write(XML_HEADER)
    tree.write(out, encoding="unicode")


def replace_urls(stack_location, repo_version_path):
  repo_dict = get_json_content(repo_version_path)
  repo_dict = {
    (json_stack_version_re.findall(ver)[0][1], ver): conf["latest"]
    for ver, conf in list(repo_dict.items())
  }

  for version_info, repo_info in repo_dict.items():
    stack_version, repo_id = version_info
    repoinfo_xml_path = path.join(
      stack_location, stack_version, "repos", "repoinfo.xml"
    )
    if path.exists(repoinfo_xml_path):
      replace_url_in_repoinfo_xml(repoinfo_xml_path, repo_id, repo_info)


def main(argv=None):
  parser = argparse.ArgumentParser(description="Replace stack repository base URLs")
  parser.add_argument("-u", "--urlinfo", required=True, dest="urlinfo_path")
  parser.add_argument("-s", "--stack-folder", required=True, dest="stack_folder")
  arguments = parser.parse_args(argv)

  print("URLINFO_PROCESSOR: starting replacement of repo urls")
  replace_urls(arguments.stack_folder, arguments.urlinfo_path)
  print("URLINFO_PROCESSOR: replacement finished")


if __name__ == "__main__":
  main()
