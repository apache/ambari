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

import optparse
import os
import subprocess
import sys
import xml.etree.ElementTree as ET

def load_file(filename):
  """
  Loads the specified XML file
  """
  if os.path.exists(filename):
    tree = ET.ElementTree()
    tree.parse(filename)
    root = tree.getroot()
  else:
    attribs = {}
    attribs['xmlns:xsi'] = "http://www.w3.org/2001/XMLSchema-instance"
    attribs['xsi:noNamespaceSchemaLocation'] = "version_definition.xsd"
    root = ET.Element("repository-version", attribs)

    ET.SubElement(root, "release")
    ET.SubElement(root, "manifest")
    ET.SubElement(root, "available-services")
    ET.SubElement(root, "repository-info")

  return root

def save_file(xml, filename):
  """
  Saves the XML file
  """
  p = subprocess.Popen(['xmllint', '--format', '--output', filename, '-'], stdout=subprocess.PIPE, stdin=subprocess.PIPE)
  (stdout, stderr) = p.communicate(input=ET.tostring(xml))

def check_xmllint():
  """
  Verifies utility xmllint is available
  """
  try:
    p = subprocess.Popen(['xmllint', '--version'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=False)
    (stdout, stderr) = p.communicate()

    if p.returncode != 0:
      raise Exception("xmllint command does not appear to be available")

  except:
    raise Exception("xmllint command does not appear to be available")
  

def validate_file(filename, xsdfile):
  """
  Validates the XML file against the XSD
  """
  args = ['xmllint', '--noout', '--load-trace', '--schema', xsdfile, filename]

  p = subprocess.Popen(args, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE)
  (stdout, stderr) = p.communicate()

  if p.returncode != 0:
    raise Exception(stderr)

  if len(stdout) > 0:
    print stdout

  if len(stderr) > 0:
    print stderr


def update_simple(parent, name, value):
  """
  Helper method to either update or create the element
  """
  element = parent.find('./' + name) 

  if element is None:
    element = ET.SubElement(parent, name)
    element.text = value
  else:
    element.text = value

def process_release(xmlroot, options):
  """
  Create elements of the 'release' parent
  """
  release_element = xmlroot.find("./release")

  if release_element is None:
    raise Exception("Element 'release' is not found")

  if options.release_type:
    update_simple(release_element, "type", options.release_type)

  if options.release_stack:
    update_simple(release_element, "stack-id", options.release_stack)

  if options.release_version:
    update_simple(release_element, "version", options.release_version)

  if options.release_build:
    update_simple(release_element, "build", options.release_build)

  if options.release_compatible:
    update_simple(release_element, "compatible-with", options.release_compatible)

  if options.release_notes:
    update_simple(release_element, "release-notes", options.release_notes)

  if options.release_display:
    update_simple(release_element, "display", options.release_display)

  if options.release_package_version:
    update_simple(release_element, "package-version", options.release_package_version)

def process_manifest(xmlroot, options):
  """
  Creates the manifest element
  """
  if not options.manifest:
    return

  manifest_element = xmlroot.find("./manifest")

  if manifest_element is None:
    raise Exception("Element 'manifest' is not found")

  service_element = manifest_element.find("./service[@id='{0}']".format(options.manifest_id))

  if service_element is None:
    service_element = ET.SubElement(manifest_element, "service")
    service_element.set('id', options.manifest_id)

  service_element.set('name', options.manifest_service)
  service_element.set('version', options.manifest_version)
  if options.manifest_version_id:
    service_element.set('version-id', options.manifest_version_id)

def process_available(xmlroot, options):
  """
  Processes available service elements
  """
  if not options.available:
    return

  manifest_element = xmlroot.find("./manifest")
  if manifest_element is None:
    raise Exception("'manifest' element is not found")

  service_element = manifest_element.find("./service[@id='{0}']".format(options.manifest_id))
  if service_element is None:
    raise Exception("Cannot add an available service for {0}; it's not on the manifest".format(options.manifest_id))

  available_element = xmlroot.find("./available-services")
  if available_element is None:
    raise Exception("'available-services' is not found")

  service_element = available_element.find("./service[@idref='{0}']".format(options.manifest_id))

  if service_element is not None:
    available_element.remove(service_element) 

  service_element = ET.SubElement(available_element, "service")
  service_element.set('idref', options.manifest_id)

  if options.available_components:
    components = options.available_components.split(',')
    for component in components:
      e = ET.SubElement(service_element, 'component')
      e.text = component


def process_repo(xmlroot, options):
  """
  Processes repository options.  This method doesn't update or create individual elements, it
  creates the entire repo structure
  """
  if not options.repo:
    return

  repo_parent = xmlroot.find("./repository-info")
  if repo_parent is None:
    raise Exception("'repository-info' element is not found")

  os_element = repo_parent.find("./os[@family='{0}']".format(options.repo_os))
  if os_element is None:
    os_element = ET.SubElement(repo_parent, 'os')
    os_element.set('family', options.repo_os)

  repo_element = os_element.find("./repo/[reponame='{0}']".format(options.repo_name))

  if repo_element is not None:
    os_element.remove(repo_element)

  repo_element = ET.SubElement(os_element, 'repo')
  e = ET.SubElement(repo_element, 'baseurl')
  e.text = options.repo_url

  e = ET.SubElement(repo_element, 'repoid')
  e.text = options.repo_id

  e = ET.SubElement(repo_element, 'reponame')
  e.text = options.repo_name

def validate_manifest(parser, options):
  """
  Validates manifest options from the command line
  """
  if not options.manifest:
    return

  template = "When specifying --manifest, {0} is also required"

  if not options.manifest_id:
    parser.error(template.format("--manifest-id"))
  
  if not options.manifest_service:
    parser.error(template.format("--manifest-service"))

  if not options.manifest_version:
    parser.error(template.format("--manifest-version"))

def validate_available(parser, options):
  """
  Validates available service options from the command line
  """
  if not options.available:
    return

  if not options.manifest_id:
    parser.error("When specifying --available, --manifest-id is also required")

def validate_repo(parser, options):
  """
  Validates repo options from the command line
  """
  if not options.repo:
    return

  template = "When specifying --repo, {0} is also required"

  if not options.repo_os:
    parser.error(template.format("--repo-os"))

  if not options.repo_url:
    parser.error(template.format("--repo-url"))

  if not options.repo_id:
    parser.error(template.format("--repo-id"))

  if not options.repo_name:
    parser.error(template.format("--repo-name"))


def main(argv):
  parser = optparse.OptionParser(
    epilog="OS utility 'xmllint' is required for this tool to function.  It handles pretty-printing and XSD validation.")
  
  parser.add_option('--file', dest='filename',
    help="The output XML file")

  parser.add_option('--finalize', action='store_true', dest='finalize',
    help="Finalize and validate the XML file")
  parser.add_option('--xsd', dest='xsd_file',
    help="The XSD location when finalizing")

  parser.add_option('--release-type', type='choice', choices=['STANDARD', 'PATCH'], dest='release_type' ,
    help="Indicate the release type: i.e. STANDARD or PATCH")
  parser.add_option('--release-stack', dest='release_stack',
    help="The stack id: e.g. HDP-2.4")
  parser.add_option('--release-version', dest='release_version',
    help="The release version without build number: e.g. 2.4.0.1")
  parser.add_option('--release-build', dest='release_build',
    help="The release build number: e.g. 1234")
  parser.add_option('--release-compatible', dest='release_compatible',
    help="Regular Expression string to identify version compatibility for patches: e.g. 2.4.1.[0-9]")
  parser.add_option('--release-notes', dest='release_notes',
    help="A http link to the documentation notes")
  parser.add_option('--release-display', dest='release_display',
    help="The display name for this release")
  parser.add_option('--release-package-version', dest='release_package_version',
    help="Identifier to use when installing packages, generally a part of the package name")

  parser.add_option('--manifest', action='store_true', dest='manifest',
    help="Add a manifest service with other options: --manifest-id, --manifest-service, --manifest-version, --manifest-version-id")
  parser.add_option('--manifest-id', dest='manifest_id',
    help="Unique ID for a service in a manifest.  Required when specifying --manifest and --available")
  parser.add_option('--manifest-service', dest='manifest_service')
  parser.add_option('--manifest-version', dest='manifest_version')
  parser.add_option('--manifest-version-id', dest='manifest_version_id')

  parser.add_option('--available', action='store_true', dest='available',
    help="Add an available service with other options: --manifest-id, --available-components")
  parser.add_option('--available-components', dest='available_components',
    help="A CSV of service components that are intended to be upgraded via patch. \
      Omitting this implies the entire service should be upgraded")

  parser.add_option('--repo', action='store_true', dest='repo',
    help="Add repository data with options: --repo-os, --repo-url, --repo-id, --repo-name")
  parser.add_option('--repo-os', dest='repo_os',
    help="The operating system type: i.e. redhat6, redhat7, debian7, ubuntu12, ubuntu14, suse11")
  parser.add_option('--repo-url', dest='repo_url',
    help="The base url for the repository data")
  parser.add_option('--repo-id', dest='repo_id', help="The ID of the repo")
  parser.add_option('--repo-name', dest='repo_name', help="The name of the repo")

  (options, args) = parser.parse_args()

  check_xmllint()

  # validate_filename
  if not options.filename:
    parser.error("--file option is required")

  validate_manifest(parser, options)
  validate_available(parser, options)
  validate_repo(parser, options)

  # validate_finalize
  if options.finalize and not options.xsd_file:
    parser.error("Must supply XSD (--xsd) when finalizing")

  # load file
  root = load_file(options.filename)

  process_release(root, options)
  process_manifest(root, options)
  process_available(root, options)
  process_repo(root, options)

  # save file
  save_file(root, options.filename)

  if options.finalize:
    validate_file(options.filename, options.xsd_file)

if __name__ == "__main__":
  main(sys.argv)
