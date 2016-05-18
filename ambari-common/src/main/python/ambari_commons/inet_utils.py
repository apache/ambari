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
import sys
import urllib2
import socket

from exceptions import FatalException, NonFatalException, TimeoutError

from logging_utils import *
from os_check import OSCheck


def openurl(url, timeout=socket._GLOBAL_DEFAULT_TIMEOUT, *args, **kwargs):
  """

  :param url: url to open
  :param timeout: open timeout, raise TimeoutError on timeout
  :rtype urllib2.Request
  """
  try:
    return urllib2.urlopen(url, timeout=timeout, *args, **kwargs)
  except urllib2.URLError as e:
    # Python 2.6 timeout handling
    if hasattr(e, "reason") and isinstance(e.reason, socket.timeout):
      raise TimeoutError(e.reason)
    else:
      raise e  # re-throw exception
  except socket.timeout as e:  # Python 2.7 timeout handling
    raise TimeoutError(e)


def download_file(link, destination, chunk_size=16 * 1024, progress_func = None):
  print_info_msg("Downloading {0} to {1}".format(link, destination))
  if os.path.exists(destination):
      print_warning_msg("File {0} already exists, assuming it was downloaded before".format(destination))
      return

  force_download_file(link, destination, chunk_size, progress_func = progress_func)


def download_progress(file_name, downloaded_size, blockSize, totalSize):
  percent = int(downloaded_size * 100 / totalSize)
  status = "\r" + file_name

  if totalSize < blockSize:
    status += "... %d%%" % (100)
  else:
    status += "... %d%% (%.1f MB of %.1f MB)" % (
      percent, downloaded_size / 1024 / 1024.0, totalSize / 1024 / 1024.0)
  sys.stdout.write(status)
  sys.stdout.flush()


def find_range_components(meta):
  file_size = 0
  seek_pos = 0
  hdr_range = meta.getheaders("Content-Range")
  if len(hdr_range) > 0:
    range_comp1 = hdr_range[0].split('/')
    if len(range_comp1) > 1:
      range_comp2 = range_comp1[0].split(' ') #split away the "bytes" prefix
      if len(range_comp2) == 0:
        raise FatalException(12, 'Malformed Content-Range response header: "{0}".' % hdr_range)
      range_comp3 = range_comp2[1].split('-')
      seek_pos = int(range_comp3[0])
      if range_comp1[1] != '*': #'*' == unknown length
        file_size = int(range_comp1[1])

  if file_size == 0:
    #Try the old-fashioned way
    hdrLen = meta.getheaders("Content-Length")
    if len(hdrLen) == 0:
      raise FatalException(12, "Response header doesn't contain Content-Length. Chunked Transfer-Encoding is not supported for now.")
    file_size = int(hdrLen[0])

  return (file_size, seek_pos)


def force_download_file(link, destination, chunk_size = 16 * 1024, progress_func = None):
  request = urllib2.Request(link)

  if os.path.exists(destination) and not os.path.isfile(destination):
    #Directory specified as target? Must be a mistake. Bail out, don't assume anything.
    err = 'Download target {0} is a directory.' % destination
    raise FatalException(1, err)

  (dest_path, file_name) = os.path.split(destination)

  temp_dest = destination + ".tmpdownload"
  partial_size = 0

  if os.path.exists(temp_dest):
    #Support for resuming downloads, in case the process is killed while downloading a file
    #  set resume range
    # See http://stackoverflow.com/questions/6963283/python-urllib2-resume-download-doesnt-work-when-network-reconnects
    partial_size = os.stat(temp_dest).st_size
    if partial_size > chunk_size:
      #Re-download the last chunk, to minimize the possibilities of file corruption
      resume_pos = partial_size - chunk_size
      request.add_header("Range", "bytes=%s-" % resume_pos)
  else:
    #Make sure the full dir structure is in place, otherwise file open will fail
    if not os.path.exists(dest_path):
      os.makedirs(dest_path)

  response = urllib2.urlopen(request)
  (file_size, seek_pos) = find_range_components(response.info())

  print_info_msg("Downloading to: %s Bytes: %s" % (destination, file_size))

  if partial_size < file_size:
    if seek_pos == 0:
      #New file, create it
      open_mode = 'wb'
    else:
      #Resuming download of an existing file
      open_mode = 'rb+' #rb+ doesn't create the file, using wb to create it
    f = open(temp_dest, open_mode)

    try:
      #Resume the download from where it left off
      if seek_pos > 0:
        f.seek(seek_pos)

      file_size_dl = seek_pos
      while True:
        buffer = response.read(chunk_size)
        if not buffer:
            break

        file_size_dl += len(buffer)
        f.write(buffer)
        if progress_func is not None:
          progress_func(file_name, file_size_dl, chunk_size, file_size)
    finally:
      f.close()

    sys.stdout.write('\n')
    sys.stdout.flush()

  print_info_msg("Finished downloading {0} to {1}".format(link, destination))

  downloaded_size = os.stat(temp_dest).st_size
  if downloaded_size != file_size:
    err = 'Size of downloaded file {0} is {1} bytes, it is probably damaged or incomplete' % (destination, downloaded_size)
    raise FatalException(1, err)

  # when download is complete -> mv temp_dest destination
  if os.path.exists(destination):
    #Windows behavior: rename fails if the destination file exists
    os.unlink(destination)
  os.rename(temp_dest, destination)

def resolve_address(address):
  """
  Resolves address to proper one in special cases, for example 0.0.0.0 to 127.0.0.1 on windows os.

  :param address: address to resolve
  :return: resulting address
  """
  if OSCheck.is_windows_family():
    if address == '0.0.0.0':
      return '127.0.0.1'
  return address
