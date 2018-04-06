#!/usr/bin/python

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

import gzip
import hashlib
import json
import logging
import optparse
import os
import shutil
import signal
import sys
import tarfile
import time
from datetime import datetime, timedelta
from subprocess import call, Popen, PIPE
from urllib import quote, unquote
from zipfile import ZipFile, ZIP_DEFLATED

VERSION = "1.0"

logger = logging.getLogger()
handler = logging.StreamHandler()
formatter = logging.Formatter("%(asctime)s - %(message)s")
handler.setFormatter(formatter)
logger.addHandler(handler)
verbose = False

def parse_arguments():
  parser = optparse.OptionParser("usage: %prog [options]", version="Solr Data Manager {0}".format(VERSION))

  parser.add_option("-m", "--mode", dest="mode", type="string", help="archive | delete | save")
  parser.add_option("-s", "--solr-url", dest="solr_url", type="string", help="the url of the solr server including the port")
  parser.add_option("-c", "--collection", dest="collection", type="string", help="the name of the solr collection")
  parser.add_option("-f", "--filter-field", dest="filter_field", type="string", help="the name of the field to filter on")
  parser.add_option("-r", "--read-block-size", dest="read_block_size", type="int", help="block size to use for reading from solr",
                    default=1000)
  parser.add_option("-w", "--write-block-size", dest="write_block_size", type="int", help="number of records in the output files",
                    default=100000)
  parser.add_option("-i", "--id-field", dest="id_field", type="string", help="the name of the id field", default="id")

  end_group = optparse.OptionGroup(parser, "specifying the end of the range")
  end_group.add_option("-e", "--end", dest="end", type="string", help="end of the range")
  end_group.add_option("-d", "--days", dest="days", type="int", help="number of days to keep")
  parser.add_option_group(end_group)

  parser.add_option("-o", "--date-format", dest="date_format", type="string", help="the date format to use for --days",
                    default="%Y-%m-%dT%H:%M:%S.%fZ")

  parser.add_option("-q", "--additional-filter", dest="additional_filter", type="string", help="additional solr filter")
  parser.add_option("-j", "--name", dest="name", type="string", help="name included in result files")

  parser.add_option("-g", "--ignore-unfinished-uploading", dest="ignore_unfinished_uploading", action="store_true", default=False)

  parser.add_option("--json-file", dest="json_file", help="create a json file instead of line delimited json", action="store_true", default=False)
  parser.add_option("-z", "--compression", dest="compression", help="none | tar.gz | tar.bz2 | zip | gz", default="gz")

  parser.add_option("-k", "--solr-keytab", dest="solr_keytab", type="string", help="the keytab for a kerberized solr")
  parser.add_option("-n", "--solr-principal", dest="solr_principal", type="string", help="the principal for a kerberized solr")

  parser.add_option("-a", "--hdfs-keytab", dest="hdfs_keytab", type="string", help="the keytab for a kerberized hdfs")
  parser.add_option("-l", "--hdfs-principal", dest="hdfs_principal", type="string", help="the principal for a kerberized hdfs")

  parser.add_option("-u", "--hdfs-user", dest="hdfs_user", type="string", help="the user for accessing hdfs")
  parser.add_option("-p", "--hdfs-path", dest="hdfs_path", type="string", help="the hdfs path to upload to")

  parser.add_option("-t", "--key-file-path", dest="key_file_path", type="string", help="the file that contains S3 <accessKey>,<secretKey>")
  parser.add_option("-b", "--bucket", dest="bucket", type="string", help="the bucket name for S3 upload")
  parser.add_option("-y", "--key-prefix", dest="key_prefix", type="string", help="the key prefix for S3 upload")

  parser.add_option("-x", "--local-path", dest="local_path", type="string", help="the local path to save the files to")

  parser.add_option("-v", "--verbose", dest="verbose", action="store_true", default=False)

  parser.add_option("--solr-output-collection", dest="solr_output_collection", help="target output solr collection for archive", type="string", default=None)
  parser.add_option("--exclude-fields", dest="exclude_fields", help="Comma separated list of excluded fields from json response", type="string", default=None)

  (options, args) = parser.parse_args()

  for r in ["mode", "solr_url", "collection", "filter_field"]:
    if options.__dict__[r] is None:
      print "argument '{0}' is mandatory".format(r)
      parser.print_help()
      sys.exit()

  mode_values = ["archive", "delete", "save"]
  if options.mode not in mode_values:
    print "mode must be one of {0}".format(" | ".join(mode_values))
    parser.print_help()
    sys.exit()

  if options.mode == "delete":
    for r in ["name", "hdfs_keytab", "hdfs_principal", "hdfs_user", "hdfs_path", "key_file_path", "bucket", "key_prefix", "local_path"]:
      if options.__dict__[r] is not None:
        print "argument '{0}' may not be specified in delete mode".format(r)
        parser.print_help()
        sys.exit()

  if options.__dict__["end"] is None and options.__dict__["days"] is None or \
          options.__dict__["end"] is not None and options.__dict__["days"] is not None:
    print "exactly one of 'end' or 'days' must be specfied"
    parser.print_help()
    sys.exit()

  is_any_solr_kerberos_property = options.__dict__["solr_keytab"] is not None or options.__dict__["solr_principal"] is not None
  is_all_solr_kerberos_property = options.__dict__["solr_keytab"] is not None and options.__dict__["solr_principal"] is not None
  if is_any_solr_kerberos_property and not is_all_solr_kerberos_property:
    print "either both 'solr-keytab' and 'solr-principal' must be specfied, or neither of them"
    parser.print_help()
    sys.exit()

  compression_values = ["none", "tar.gz", "tar.bz2", "zip", "gz"]
  if options.compression not in compression_values:
    print "compression must be one of {0}".format(" | ".join(compression_values))
    parser.print_help()
    sys.exit()

  is_any_solr_output_property = options.__dict__["solr_output_collection"] is not None

  is_any_hdfs_kerberos_property = options.__dict__["hdfs_keytab"] is not None or options.__dict__["hdfs_principal"] is not None
  is_all_hdfs_kerberos_property = options.__dict__["hdfs_keytab"] is not None and options.__dict__["hdfs_principal"] is not None
  if is_any_hdfs_kerberos_property and not is_all_hdfs_kerberos_property:
    print "either both 'hdfs_keytab' and 'hdfs_principal' must be specfied, or neither of them"
    parser.print_help()
    sys.exit()

  is_any_hdfs_property = options.__dict__["hdfs_user"] is not None or options.__dict__["hdfs_path"] is not None
  is_all_hdfs_property = options.__dict__["hdfs_user"] is not None and options.__dict__["hdfs_path"] is not None
  if is_any_hdfs_property and not is_all_hdfs_property:
    print "either both 'hdfs_user' and 'hdfs_path' must be specfied, or neither of them"
    parser.print_help()
    sys.exit()

  is_any_s3_property = options.__dict__["key_file_path"] is not None or options.__dict__["bucket"] is not None or \
                       options.__dict__["key_prefix"] is not None
  is_all_s3_property = options.__dict__["key_file_path"] is not None and options.__dict__["bucket"] is not None and \
                       options.__dict__["key_prefix"] is not None
  if is_any_s3_property and not is_all_s3_property:
    print "either all the S3 arguments ('key_file_path', 'bucket', 'key_prefix') must be specfied, or none of them"
    parser.print_help()
    sys.exit()

  if options.mode in ["archive", "save"]:
    count = (1 if is_any_solr_output_property else 0) + (1 if is_any_hdfs_property else 0) + \
            (1 if is_any_s3_property else 0) + (1 if options.__dict__["local_path"] is not None else 0)
    if count != 1:
      print "exactly one of the HDFS arguments ('hdfs_user', 'hdfs_path') or the S3 arguments ('key_file_path', 'bucket', 'key_prefix') or the solr arguments ('solr_output_collection') or the 'local_path' argument must be specified"
      parser.print_help()
      sys.exit()

  if options.__dict__["hdfs_keytab"] is not None and options.__dict__["hdfs_user"] is None:
    print "HDFS kerberos keytab and principal may only be specified if the upload target is HDFS"
    parser.print_help()
    sys.exit()

  print("You are running Solr Data Manager {0} with arguments:".format(VERSION))
  print("  mode: " + options.mode)
  print("  solr-url: " + options.solr_url)
  print("  collection: " + options.collection)
  print("  filter-field: " + options.filter_field)
  if options.mode in ["archive", "save"]:
    print("  id-field: " + options.id_field)
  if options.__dict__["exclude_fields"] is not None:
    print("  exclude fields: " + options.exclude_fields)
  if options.__dict__["end"] is not None:
    print("  end: " + options.end)
  else:
    print("  days: " + str(options.days))
    print("  date-format: " + options.date_format)
  if options.__dict__["additional_filter"] is not None:
    print("  additional-filter: " + str(options.additional_filter))
  if options.__dict__["name"] is not None:
    print("  name: " + str(options.name))
  if options.mode in ["archive", "save"]:
    print("  read-block-size: " + str(options.read_block_size))
    print("  write-block-size: " + str(options.write_block_size))
    print("  ignore-unfinished-uploading: " + str(options.ignore_unfinished_uploading))
  if (options.__dict__["solr_keytab"] is not None):
    print("  solr-keytab: " + options.solr_keytab)
    print("  solr-principal: " + options.solr_principal)
  if options.mode in ["archive", "save"]:
    print("  output: " + ("json" if options.json_file else "line-delimited-json"))
    print("  compression: " + options.compression)
  if options.__dict__["solr_output_collection"] is not None:
    print("  solr output collection: " + options.solr_output_collection)
  if (options.__dict__["hdfs_keytab"] is not None):
    print("  hdfs-keytab: " + options.hdfs_keytab)
    print("  hdfs-principal: " + options.hdfs_principal)
  if (options.__dict__["hdfs_user"] is not None):
    print("  hdfs-user: " + options.hdfs_user)
    print("  hdfs-path: " + options.hdfs_path)
  if (options.__dict__["key_file_path"] is not None):
    print("  key-file-path: " + options.key_file_path)
    print("  bucket: " + options.bucket)
    print("  key-prefix: " + options.key_prefix)
  if (options.__dict__["local_path"] is not None):
    print("  local-path: " + options.local_path)
  print("  verbose: " + str(options.verbose))
  print

  if options.__dict__["additional_filter"] is not None and options.__dict__["name"] is None:
    go = False
    while not go:
      sys.stdout.write("It is recommended to set --name in case of any additional filter is set.\n")
      sys.stdout.write("Are you sure that you want to proceed without a name (yes/no)? ")
      choice = raw_input().lower()
      if choice in ['yes', 'ye', 'y']:
        go = True
      elif choice in ['no', 'n']:
        sys.exit()

  return options

def set_log_level():
  if verbose:
    logger.setLevel(logging.DEBUG)
  else:
    logger.setLevel(logging.INFO)

def get_end(options):
  if options.end:
    return options.end
  else:
    d = datetime.now() - timedelta(days=options.days)
    end = d.strftime(options.date_format)
    logger.info("The end date will be: %s", end)
    return end

def delete(solr_url, collection, filter_field, end, solr_keytab, solr_principal):
  logger.info("Deleting data where %s <= %s", filter_field, end)
  solr_kinit_command = None
  if solr_keytab:
    solr_kinit_command = "kinit -kt {0} {1}".format(solr_keytab, solr_principal)
    curl_prefix = "curl -k --negotiate -u : "
  else:
    curl_prefix = "curl -k"

  delete_query = "{0}:[* TO \"{1}\"]".format(filter_field, end)
  delete_command = "{0}/{1}/update?commit=true&wt=json".format(solr_url, collection)
  delete_data = "<delete><query>{0}</query></delete>".format(delete_query)

  query_solr(solr_kinit_command, delete_command, "{0} -H Content-Type:text/xml {1}".format(curl_prefix, delete_command), "Deleting", delete_data)

def save(mode, solr_url, collection, filter_field, id_field, range_end, read_block_size, write_block_size,
         ignore_unfinished_uploading, additional_filter, name, solr_keytab, solr_principal, json_file,
         compression, hdfs_keytab, hdfs_principal, hdfs_user, hdfs_path, key_file_path, bucket, key_prefix, local_path,
         solr_output_collection, exclude_fields):
  solr_kinit_command = None
  if solr_keytab:
    solr_kinit_command = "kinit -kt {0} {1}".format(solr_keytab, solr_principal)
    curl_prefix = "curl -k --negotiate -u : "
  else:
    curl_prefix = "curl -k"

  hdfs_kinit_command = None
  if hdfs_keytab:
    hdfs_kinit_command = "sudo -u {0} kinit -kt {1} {2}".format(hdfs_user, hdfs_keytab, hdfs_principal)

  if options.hdfs_path:
    ensure_hdfs_path(hdfs_kinit_command, hdfs_user, hdfs_path)

  working_dir = get_working_dir(solr_url, collection)
  if mode == "archive":
    handle_unfinished_uploading(solr_kinit_command, hdfs_kinit_command, curl_prefix, working_dir, ignore_unfinished_uploading)

  save_data(mode, solr_kinit_command, hdfs_kinit_command, curl_prefix, solr_url, collection, filter_field, id_field,
            range_end, read_block_size, write_block_size, working_dir, additional_filter, name, json_file, compression,
            hdfs_user, hdfs_path, key_file_path, bucket, key_prefix, local_path, solr_output_collection, exclude_fields)

def ensure_hdfs_path(hdfs_kinit_command, hdfs_user, hdfs_path):
  if hdfs_kinit_command:
    run_kinit(hdfs_kinit_command, "HDFS")

  try:
    hdfs_create_dir_command = "sudo -u {0} hadoop fs -mkdir -p {1}".format(hdfs_user, hdfs_path)
    logger.debug("Ensuring that the HDFS path %s exists:\n%s", hdfs_path, hdfs_create_dir_command)
    result = call(hdfs_create_dir_command.split())
  except Exception as e:
    print
    logger.warn("Could not execute hdfs ensure dir command:\n%s", hdfs_create_dir_command)
    logger.warn(str(e))
    sys.exit()

  if result != 0:
    print
    logger.warn("Could not ensure HDFS dir command:\n%s", hdfs_create_dir_command)
    logger.warn(str(err))
    sys.exit()

def get_working_dir(solr_url, collection):
  md5 = hashlib.md5()
  md5.update(solr_url)
  md5.update(collection)
  hash = md5.hexdigest()
  working_dir = "/tmp/solrDataManager/{0}".format(hash)

  if not(os.path.isdir(working_dir)):
    os.makedirs(working_dir)

  logger.debug("Working directory is %s", working_dir)
  return working_dir

def handle_unfinished_uploading(solr_kinit_command, hdfs_kinit_command, curl_prefix, working_dir, ignore_unfinished_uploading):
  command_json_path = "{0}/command.json".format(working_dir)
  if os.path.isfile(command_json_path):
    with open(command_json_path) as command_file:
      command = json.load(command_file)

    if "upload" in command.keys() and ignore_unfinished_uploading:
      logger.info("Ignoring unfinished uploading left by previous run")
      os.remove(command_json_path)
      return

    if "upload" in command.keys():
      logger.info("Previous run has left unfinished uploading")
      logger.info("You may try to run the program with '-g' or '--ignore-unfinished-uploading' to ignore it if it keeps on failing")

      if command["upload"]["type"] == "solr":
        upload_file_to_solr(solr_kinit_command, curl_prefix, command["upload"]["command"], command["upload"]["upload_file_path"], command["upload"]["solr_output_collection"])
      elif command["upload"]["type"] == "hdfs":
        upload_file_hdfs(hdfs_kinit_command, command["upload"]["command"], command["upload"]["upload_file_path"],
                         command["upload"]["hdfs_path"], command["upload"]["hdfs_user"])
      elif command["upload"]["type"] == "s3":
        upload_file_s3(command["upload"]["command"], command["upload"]["upload_file_path"], command["upload"]["bucket"],
                       command["upload"]["key_prefix"])
      elif command["upload"]["type"] == "local":
        upload_file_local(command["upload"]["command"], command["upload"]["upload_file_path"], command["upload"]["local_path"])
      else:
        logger.warn("Unknown upload type: %s", command["upload"]["type"])
        sys.exit()

    if "delete" in command.keys():
      delete_data(solr_kinit_command, curl_prefix, command["delete"]["command"], command["delete"]["collection"],
                  command["delete"]["filter_field"], command["delete"]["id_field"], command["delete"]["prev_lot_end_value"],
                  command["delete"]["prev_lot_end_id"])

    os.remove(command_json_path)

def save_data(mode, solr_kinit_command, hdfs_kinit_command, curl_prefix, solr_url, collection, filter_field, id_field,
              range_end, read_block_size, write_block_size, working_dir, additional_filter, name, json_file,
              compression, hdfs_user, hdfs_path, key_file_path, bucket, key_prefix, local_path, solr_output_collection, exclude_fields):
  logger.info("Starting to save data")

  tmp_file_path = "{0}/tmp.json".format(working_dir)

  prev_lot_end_value = None
  prev_lot_end_id = None

  if additional_filter:
    q = quote("{0}+AND+{1}:[*+TO+\"{2}\"]".format(additional_filter, filter_field, range_end), safe="/+\"*")
  else:
    q = quote("{0}:[*+TO+\"{1}\"]".format(filter_field, range_end), safe="/+\"*")

  sort = quote("{0}+asc,{1}+asc".format(filter_field, id_field), safe="/+\"*")
  solr_query_url_prefix = "{0}/{1}/select?q={2}&sort={3}&rows={4}&wt=json".format(solr_url, collection, q, sort, read_block_size)

  exclude_field_list = exclude_fields.split(',') if exclude_fields else None
  if solr_output_collection and not exclude_field_list:
    exclude_field_list = ['_version_']

  done = False
  total_records = 0
  while not done:
    results = create_block(tmp_file_path, solr_kinit_command, curl_prefix, solr_query_url_prefix, filter_field,
                           id_field, range_end, write_block_size, prev_lot_end_value, prev_lot_end_id, json_file,
                           exclude_field_list)
    done = results[0]
    records = results[1]
    prev_lot_end_value = results[2]
    prev_lot_end_id = results[3]

    if records > 0:
      upload_block(mode, solr_kinit_command, hdfs_kinit_command, curl_prefix, solr_url, collection, filter_field,
                   id_field, working_dir, tmp_file_path, name, prev_lot_end_value, prev_lot_end_id, hdfs_user,
                   hdfs_path, key_file_path, bucket, key_prefix, local_path, compression, solr_output_collection)
      total_records += records
      logger.info("A total of %d records are saved", total_records)

def create_block(tmp_file_path, solr_kinit_command, curl_prefix, solr_query_url_prefix, filter_field, id_field, range_end,
                 write_block_size, prev_lot_end_value, prev_lot_end_id, json_file, exclude_field_list):
  if os.path.exists(tmp_file_path):
    os.remove(tmp_file_path)
  tmp_file = open(tmp_file_path, 'w')
  logger.debug("Created tmp file %s", tmp_file_path)

  init_file(tmp_file, json_file)
  records = 0
  done = False
  while records < write_block_size:
    if prev_lot_end_value:
      fq_prev_end_rest = "({0}:\"{1}\"+AND+{2}:{{\"{3}\"+TO+*])".format(filter_field, prev_lot_end_value, id_field,
                                                                        prev_lot_end_id)
      fq_new = "{0}:{{\"{1}\"+TO+\"{2}\"]".format(filter_field, prev_lot_end_value, range_end)
      fq = "{0}+OR+{1}".format(fq_prev_end_rest, fq_new)
    else:
      fq = "{0}:[*+TO+\"{1}\"]".format(filter_field, range_end)

    url = "{0}&fq={1}".format(solr_query_url_prefix, quote(fq, safe="/+\"*"))
    curl_command = "{0} {1}".format(curl_prefix, url)

    rsp = query_solr(solr_kinit_command, url, curl_command, "Obtaining")

    if rsp['response']['numFound'] == 0:
      done = True
      break

    for doc in rsp['response']['docs']:
      last_doc = doc
      add_line(tmp_file, doc, json_file, records, exclude_field_list)
      records += 1
      if records == write_block_size:
        break

    prev_lot_end_value = last_doc[filter_field]
    prev_lot_end_id = last_doc[id_field]
    sys.stdout.write("\r{0} records are written".format(records))
    sys.stdout.flush()
    if verbose and records < write_block_size:
      print
      logger.debug("Collecting next lot of data")

  finish_file(tmp_file, json_file)
  sys.stdout.write("\n")
  logger.debug("Finished data collection")
  return [done, records, prev_lot_end_value, prev_lot_end_id]

def init_file(tmp_file, json_file):
  if json_file:
    tmp_file.write("{\n")

def add_line(tmp_file, doc, json_file, records, exclude_fields):
  if records > 0:
    if json_file:
      tmp_file.write(",\n")
    else:
      tmp_file.write("\n")
  if exclude_fields:
    for exclude_field in exclude_fields:
      if doc and exclude_field in doc:
        del doc[exclude_field]

  tmp_file.write(json.dumps(doc))

def finish_file(tmp_file, json_file):
  if json_file:
    tmp_file.write("\n}")

def upload_block(mode, solr_kinit_command, hdfs_kinit_command, curl_prefix, solr_url, collection, filter_field,
                 id_field, working_dir, tmp_file_path, name, prev_lot_end_value, prev_lot_end_id, hdfs_user, hdfs_path,
                 key_file_path, bucket, key_prefix, local_path, compression, solr_output_collection):
  if name:
    file_name = "{0}_-_{1}_-_{2}_-_{3}".format(collection, name, prev_lot_end_value, prev_lot_end_id).replace(':', '_')
  else:
    file_name = "{0}_-_{1}_-_{2}".format(collection, prev_lot_end_value, prev_lot_end_id).replace(':', '_')

  upload_file_path = compress_file(working_dir, tmp_file_path, file_name, compression)

  upload_command = create_command_file(mode, True, working_dir, upload_file_path, solr_url, collection, filter_field,
                                       id_field, prev_lot_end_value, prev_lot_end_id, hdfs_user, hdfs_path,
                                       key_file_path, bucket, key_prefix, local_path, solr_output_collection)
  if solr_output_collection:
    upload_file_to_solr(solr_kinit_command, curl_prefix, upload_command, upload_file_path, solr_output_collection)
  elif hdfs_user:
    upload_file_hdfs(hdfs_kinit_command, upload_command, upload_file_path, hdfs_path, hdfs_user)
  elif key_file_path:
    upload_file_s3(upload_command, upload_file_path, bucket, key_prefix)
  elif local_path:
    upload_file_local(upload_command, upload_file_path, local_path)
  else:
    logger.warn("Unknown upload destination")
    sys.exit()

  delete_command = create_command_file(mode, False, working_dir, upload_file_path, solr_url, collection, filter_field,
                                       id_field, prev_lot_end_value, prev_lot_end_id, None, None, None, None, None, None, None)
  if mode == "archive":
    delete_data(solr_kinit_command, curl_prefix, delete_command, collection, filter_field, id_field, prev_lot_end_value,
                prev_lot_end_id)
    os.remove("{0}/command.json".format(working_dir))

def compress_file(working_dir, tmp_file_path, file_name, compression):
  data_file_name = "{0}.json".format(file_name)
  if compression == "none":
    upload_file_path = "{0}/{1}.json".format(working_dir, file_name)
    os.rename(tmp_file_path, upload_file_path)
  elif compression == "tar.gz":
    upload_file_path = "{0}/{1}.json.tar.gz".format(working_dir, file_name)
    tar = tarfile.open(upload_file_path, mode="w:gz")
    try:
      tar.add(tmp_file_path, arcname=data_file_name)
    finally:
      tar.close()
  elif compression == "tar.bz2":
    upload_file_path = "{0}/{1}.json.tar.bz2".format(working_dir, file_name)
    tar = tarfile.open(upload_file_path, mode="w:bz2")
    try:
      tar.add(tmp_file_path, arcname=data_file_name)
    finally:
      tar.close()
  elif compression == "zip":
    upload_file_path = "{0}/{1}.json.zip".format(working_dir, file_name)
    zip = ZipFile(upload_file_path, 'w')
    zip.write(tmp_file_path, data_file_name, ZIP_DEFLATED)
  elif compression == "gz":
    upload_file_path = "{0}/{1}.json.gz".format(working_dir, file_name)
    gz = gzip.open(upload_file_path, mode="wb")
    f = open(tmp_file_path)
    try:
      shutil.copyfileobj(f, gz)
    finally:
      gz.close()
      f.close()
  else:
    logger.warn("Unknown compression type")
    sys.exit()

  logger.info("Created data file %s", data_file_name)


  return upload_file_path

def create_command_file(mode, upload, working_dir, upload_file_path, solr_url, collection, filter_field, id_field,
                        prev_lot_end_value, prev_lot_end_id, hdfs_user, hdfs_path, key_file_path, bucket, key_prefix,
                        local_path, solr_output_collection):
  commands = {}

  if upload:
    logger.debug("Creating command file with upload and delete instructions in case of an interruption")
  else:
    logger.debug("Creating command file with delete instructions in case of an interruption")

  if upload:
    if solr_output_collection:
      upload_command = "{0}/{1}/update/json/docs --data-binary @{2}"\
        .format(solr_url, solr_output_collection, upload_file_path)
      upload_command_data = {}
      upload_command_data["type"] = "solr"
      upload_command_data["command"] = upload_command
      upload_command_data["upload_file_path"] = upload_file_path
      upload_command_data["solr_output_collection"] = solr_output_collection
      commands["upload"] = upload_command_data
    elif hdfs_path:
      upload_command = "sudo -u {0} hadoop fs -put {1} {2}".format(hdfs_user, upload_file_path, hdfs_path)
      upload_command_data = {}
      upload_command_data["type"] = "hdfs"
      upload_command_data["command"] = upload_command
      upload_command_data["upload_file_path"] = upload_file_path
      upload_command_data["hdfs_path"] = hdfs_path
      upload_command_data["hdfs_user"] = hdfs_user
      commands["upload"] = upload_command_data
    elif key_file_path:
      upload_command = "java -cp {0}/libs/* org.apache.ambari.infra.solr.S3Uploader {1} {2} {3} {4}".format( \
        os.path.dirname(os.path.realpath(__file__)), key_file_path, bucket, key_prefix, upload_file_path)
      upload_command_data = {}
      upload_command_data["type"] = "s3"
      upload_command_data["command"] = upload_command
      upload_command_data["upload_file_path"] = upload_file_path
      upload_command_data["bucket"] = bucket
      upload_command_data["key_prefix"] = key_prefix
      commands["upload"] = upload_command_data
    elif local_path:
      upload_command = "mv {0} {1}".format(upload_file_path, local_path)
      upload_command_data = {}
      upload_command_data["type"] = "local"
      upload_command_data["command"] = upload_command
      upload_command_data["upload_file_path"] = upload_file_path
      upload_command_data["local_path"] = local_path
      commands["upload"] = upload_command_data
    else:
      logger.warn("Unknown upload destination")
      sys.exit()

    if mode == "save":
      return upload_command


  delete_prev = "{0}:[*+TO+\"{1}\"]".format(filter_field, prev_lot_end_value)
  delete_last = "({0}:\"{1}\"+AND+{2}:[*+TO+\"{3}\"])".format(filter_field, prev_lot_end_value, id_field, prev_lot_end_id)
  delete_query = "{0}+OR+{1}".format(delete_prev, delete_last)

  delete_command = "{0}/{1}/update?commit=true&wt=json --data-binary <delete><query>{2}</query></delete>" \
    .format(solr_url, collection, delete_query)
  if mode == "save":
    return delete_command

  delete_command_data = {}
  delete_command_data["command"] = delete_command
  delete_command_data["collection"] = collection
  delete_command_data["filter_field"] = filter_field
  delete_command_data["id_field"] = id_field
  delete_command_data["prev_lot_end_value"] = prev_lot_end_value
  delete_command_data["prev_lot_end_id"] = prev_lot_end_id
  commands["delete"] = delete_command_data

  command_file_path = "{0}/command.json".format(working_dir)
  command_file_path_tmp = "{0}.tmp".format(command_file_path)
  cft = open(command_file_path_tmp, 'w')
  cft.write(json.dumps(commands, indent=4))
  os.rename(command_file_path_tmp, command_file_path)

  logger.debug("Command file %s was created", command_file_path)

  if upload:
    return upload_command
  else:
    return delete_command

def upload_file_hdfs(hdfs_kinit_command, upload_command, upload_file_path, hdfs_path, hdfs_user):
  if hdfs_kinit_command:
    run_kinit(hdfs_kinit_command, "HDFS")

  try:
    hdfs_file_exists_command = "sudo -u {0} hadoop fs -test -e {1}".format(hdfs_user, hdfs_path + os.path.basename(upload_file_path))
    logger.debug("Checking if file already exists on hdfs:\n%s", hdfs_file_exists_command)
    hdfs_file_exists = (0 == call(hdfs_file_exists_command.split()))
  except Exception as e:
    print
    logger.warn("Could not execute command to check if file already exists on HDFS:\n%s", hdfs_file_exists_command)
    logger.warn(str(e))
    sys.exit()

  if os.path.isfile(upload_file_path) and not hdfs_file_exists:
    try:
      logger.debug("Uploading file to hdfs:\n%s", upload_command)
      result = call(upload_command.split())
    except Exception as e:
      print
      logger.warn("Could not execute command to upload file to HDFS:\n%s", upload_command)
      logger.warn(str(e))
      sys.exit()

    if result != 0:
      logger.warn("Could not upload file to HDFS with command:\n%s", upload_command)
      sys.exit()

    logger.info("File %s was uploaded to hdfs %s", os.path.basename(upload_file_path), hdfs_path)
    os.remove(upload_file_path)

def upload_file_s3(upload_command, upload_file_path, bucket, key_prefix):
  if os.path.isfile(upload_file_path):
    try:
      logger.debug("Uploading file to s3:\n%s", upload_command)
      result = call(upload_command.split())
    except Exception as e:
      print
      logger.warn("Could not execute command to upload file to S3:\n%s", upload_command)
      logger.warn(str(e))
      sys.exit()

    if result != 0:
      logger.warn("Could not upload file to S3 with command:\n%s", upload_command)
      sys.exit()

    logger.info("File %s was uploaded to s3 bucket '%s', key '%s'", os.path.basename(upload_file_path), bucket,
                key_prefix + os.path.basename(upload_file_path))
    os.remove(upload_file_path)

def upload_file_local(upload_command, upload_file_path, local_path):
  if os.path.exists(local_path) and not os.path.isdir(local_path):
    logger.warn("Local path %s exists, but not a directory, can not save there", local_path)
  if not os.path.isdir(local_path):
    os.mkdir(local_path)
    logger.debug("Directory %s was created", local_path)

  try:
    logger.debug("Moving file to local directory %s with command\n%s", local_path, upload_command)
    call(upload_command.split())
    logger.info("File %s was moved to local directory %s", os.path.basename(upload_file_path), local_path)
  except Exception as e:
    print
    logger.warn("Could not execute move command command:\n%s", upload_command)
    logger.warn(str(e))
    sys.exit()

def upload_file_to_solr(solr_kinit_command, curl_prefix, upload_command, upload_file_path, collection):
  if os.path.isfile(upload_file_path):
    query_solr(solr_kinit_command, upload_command, "{0} -H Content-type:application/json {1}".format(curl_prefix, upload_command), "Saving")
    logger.info("Save data to collection: %s", collection)

def delete_data(solr_kinit_command, curl_prefix, delete_command, collection, filter_field, id_field, prev_lot_end_value,
                prev_lot_end_id):
  delete_cmd = delete_command.split(" --data-binary")[0]
  delete_query_data = delete_command.split("--data-binary ")[1].replace("+", " ")
  query_solr(solr_kinit_command, delete_cmd, "{0} -H Content-Type:text/xml {1}".format(curl_prefix, delete_cmd), "Deleting", delete_query_data)
  logger.info("Deleted data from collection %s where %s,%s < %s,%s", collection, filter_field, id_field, prev_lot_end_value,
              prev_lot_end_id)

def query_solr(solr_kinit_command, url, curl_command, action, data=None):
  if solr_kinit_command:
    run_kinit(solr_kinit_command, "Solr")

  try:
    cmd = curl_command.split()
    if data:
      cmd.append("--data-binary")
      cmd.append(data)
    logger.debug("%s data from solr:\n%s", action, ' '.join(cmd))
    process = Popen(cmd, stdin=PIPE, stdout=PIPE, stderr=PIPE)
  except Exception as e:
    print
    logger.warn("Could not execute curl command:\n%s", ' '.join(cmd))
    logger.warn(str(e))
    sys.exit()

  out, err = process.communicate()
  if process.returncode != 0:
    print
    logger.warn("Could not execute curl command:\n%s", ' '.join(cmd))
    logger.warn(str(err))
    sys.exit()

  true = True # needed to be able to eval 'true' in the returned json
  rsp = eval(str(out))
  if rsp["responseHeader"]["status"] != 0:
    print
    logger.warn("Could not execute solr query:\n%s", unquote(url))
    logger.warn(rsp["error"]["msg"])
    sys.exit()

  return rsp

def run_kinit(kinit_command, program):
  try:
    logger.debug("Running kinit for %s:\n%s", program, kinit_command)
    result = call(kinit_command.split())
  except Exception as e:
    print
    logger.warn("Could not execute %s kinit command:\n%s", program, kinit_command)
    logger.warn(str(e))
    sys.exit()

  if result != 0:
    print
    logger.warn("%s kinit command was not successful:\n%s", program, kinit_command)
    sys.exit()

if __name__ == '__main__':
  try:
    start_time = time.time()

    options = parse_arguments()
    verbose = options.verbose
    set_log_level()

    end = get_end(options)

    if options.mode == "delete":
      delete(options.solr_url, options.collection, options.filter_field, end, options.solr_keytab, options.solr_principal)
    elif options.mode in ["archive", "save"]:
      save(options.mode, options.solr_url, options.collection, options.filter_field, options.id_field, end,
           options.read_block_size, options.write_block_size, options.ignore_unfinished_uploading,
           options.additional_filter, options.name, options.solr_keytab, options.solr_principal, options.json_file,
           options.compression, options.hdfs_keytab, options.hdfs_principal, options.hdfs_user, options.hdfs_path,
           options.key_file_path, options.bucket, options.key_prefix, options.local_path, options.solr_output_collection,
           options.exclude_fields)
    else:
      logger.warn("Unknown mode: %s", options.mode)

    print("--- %s seconds ---" % (time.time() - start_time))
  except KeyboardInterrupt:
    print
    sys.exit(128 + signal.SIGINT)
