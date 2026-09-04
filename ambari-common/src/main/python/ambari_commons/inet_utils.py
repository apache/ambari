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

import os
import time
import sys
import urllib.request, urllib.error, urllib.parse
import socket
import re

from .exceptions import FatalException, NonFatalException, TimeoutError

# MacOS not supported
from ambari_commons.os_linux import os_run_os_command

from .logging_utils import *
from .os_check import OSCheck


def openurl(url, timeout=socket._GLOBAL_DEFAULT_TIMEOUT, *args, **kwargs):
  """

  :param url: url to open
  :param timeout: open timeout, raise TimeoutError on timeout
  :rtype: http.client.HTTPResponse
  """
  try:
    return urllib.request.urlopen(url, timeout=timeout, *args, **kwargs)
  except urllib.error.URLError as e:
    if hasattr(e, "reason") and isinstance(e.reason, socket.timeout):
      raise TimeoutError(e.reason)
    else:
      raise e  # re-throw exception
  except socket.timeout as e:
    raise TimeoutError(e)


def download_file(link, destination, chunk_size=16 * 1024, progress_func=None):
  print_info_msg(f"Downloading {link} to {destination}")
  if os.path.exists(destination):
    print_warning_msg(
      f"File {destination} already exists, assuming it was downloaded before"
    )
    return

  force_download_file(link, destination, chunk_size, progress_func=progress_func)


def download_file_anyway(link, destination, chunk_size=16 * 1024, progress_func=None):
  print_info_msg(
    f"Trying to download {link} to {destination} with Python library [urllib.request]."
  )
  if os.path.exists(destination):
    print_warning_msg(
      f"File {destination} already exists, assuming it was downloaded before"
    )
    return
  try:
    force_download_file(link, destination, chunk_size, progress_func=progress_func)
  except:
    print_error_msg(
      f"Download {link} with Python library [urllib.request] failed with error: {str(sys.exc_info())}"
    )

  if not os.path.exists(destination):
    print(f"Trying to download {link} to {destination} with [curl] command.")
    # print_info_msg(f"Trying to download {link} to {destination} with [curl] command.")
    curl_command = f"curl --fail -o {destination} {link}"
    retcode, out, err = os_run_os_command(curl_command)
    if retcode != 0:
      print_error_msg(
        f"Download file {link} with [curl] command failed with error: {out + err}"
      )

  if not os.path.exists(destination):
    print_error_msg(f"Unable to download file {link}!")
    print(f"ERROR: unable to donwload file {link}!")


def download_progress(file_name, downloaded_size, blockSize, totalSize):
  percent = int(downloaded_size * 100 / totalSize)
  status = "\r" + file_name

  if totalSize < blockSize:
    status += "... %d%%" % (100)
  else:
    status += "... %d%% (%.1f MB of %.1f MB)" % (
      percent,
      downloaded_size / 1024 / 1024.0,
      totalSize / 1024 / 1024.0,
    )
  sys.stdout.write(status)
  sys.stdout.flush()


def wait_for_port_opened(hostname, port, tries_count, try_sleep):
  sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  sock.settimeout(2)

  for i in range(tries_count):
    if sock.connect_ex((hostname, port)) == 0:
      return True
    time.sleep(try_sleep)

  return False


def find_range_components(meta):
  file_size = 0
  seek_pos = 0
  hdr_range = meta.get_all("Content-Range")
  if hdr_range and len(hdr_range) > 0:
    range_comp1 = hdr_range[0].split("/")
    if len(range_comp1) > 1:
      range_comp2 = range_comp1[0].split(" ")  # split away the "bytes" prefix
      if len(range_comp2) == 0:
        raise FatalException(
          12, f'Malformed Content-Range response header: "{hdr_range}".'
        )
      range_comp3 = range_comp2[1].split("-")
      seek_pos = int(range_comp3[0])
      if range_comp1[1] != "*":  #'*' == unknown length
        file_size = int(range_comp1[1])

  if file_size == 0:
    # Try the old-fashioned way
    hdrLen = meta.get_all("Content-Length")
    if hdrLen and len(hdrLen) == 0:
      raise FatalException(
        12,
        "Response header doesn't contain Content-Length. Chunked Transfer-Encoding is not supported for now.",
      )
    file_size = int(hdrLen[0])

  return (file_size, seek_pos)


def force_download_file(link, destination, chunk_size=16 * 1024, progress_func=None):
  request = urllib.request.Request(link)

  if os.path.exists(destination) and not os.path.isfile(destination):
    # Directory specified as target? Must be a mistake. Bail out, don't assume anything.
    err = f"Download target {destination} is a directory."
    raise FatalException(1, err)

  (dest_path, file_name) = os.path.split(destination)

  temp_dest = destination + ".tmpdownload"
  partial_size = 0

  if os.path.exists(temp_dest):
    # Support for resuming downloads, in case the process is killed while downloading a file
    #  set resume range
    # Resume from the previous chunk boundary after an interrupted download.
    partial_size = os.stat(temp_dest).st_size
    if partial_size > chunk_size:
      # Re-download the last chunk, to minimize the possibilities of file corruption
      resume_pos = partial_size - chunk_size
      request.add_header("Range", f"bytes={resume_pos}-")
  else:
    # Make sure the full dir structure is in place, otherwise file open will fail
    if not os.path.exists(dest_path):
      os.makedirs(dest_path)

  response = urllib.request.urlopen(request)
  (file_size, seek_pos) = find_range_components(response.info())

  print_info_msg(f"Downloading to: {destination} Bytes: {file_size}")

  if partial_size < file_size:
    if seek_pos == 0:
      # New file, create it
      open_mode = "wb"
    else:
      # Resuming download of an existing file
      open_mode = "rb+"  # rb+ doesn't create the file, using wb to create it
    f = open(temp_dest, open_mode)

    try:
      # Resume the download from where it left off
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

    sys.stdout.write("\n")
    sys.stdout.flush()

  print_info_msg(f"Finished downloading {link} to {destination}")

  downloaded_size = os.stat(temp_dest).st_size
  if downloaded_size != file_size:
    err = f"Size of downloaded file {destination} is {downloaded_size} bytes, it is probably damaged or incomplete"
    raise FatalException(1, err)

  # when download is complete -> mv temp_dest destination
  if os.path.exists(destination):
    os.unlink(destination)
  os.rename(temp_dest, destination)


def resolve_address(address):
  """
  Returns the address used by alert probes.

  :param address: address to resolve
  :return: resulting address
  """
  return address


def resolve_tls_client_protocol(protocol="PROTOCOL_TLS_CLIENT"):
  """Resolve supported legacy configuration values to a secure client protocol."""
  import ssl

  allowed_values = {ssl.PROTOCOL_TLS_CLIENT}
  legacy_tls_1_2 = getattr(ssl, "PROTOCOL_TLSv1_2", None)
  if legacy_tls_1_2 is not None:
    allowed_values.add(legacy_tls_1_2)

  if isinstance(protocol, str):
    if protocol not in ("PROTOCOL_TLS_CLIENT", "PROTOCOL_TLSv1_2"):
      raise ValueError(f"Unsupported TLS client protocol: {protocol}")
    if protocol == "PROTOCOL_TLSv1_2" and legacy_tls_1_2 is None:
      raise ValueError("PROTOCOL_TLSv1_2 is not available in this Python runtime")
  elif protocol not in allowed_values:
    raise ValueError(f"Unsupported TLS client protocol: {protocol}")
  return ssl.PROTOCOL_TLS_CLIENT


def create_ssl_context(protocol="PROTOCOL_TLS_CLIENT", ca_certs=None):
  """
  Create an explicitly scoped client SSL context.

  :param protocol: PROTOCOL_TLS_CLIENT or legacy PROTOCOL_TLSv1_2
  :param ca_certs: path to ca_certs file
  :return: a context that verifies the certificate chain and hostname
  """
  import ssl

  context = ssl.SSLContext(protocol=resolve_tls_client_protocol(protocol))
  context.minimum_version = ssl.TLSVersion.TLSv1_2
  context.load_default_certs(ssl.Purpose.SERVER_AUTH)
  if ca_certs:
    context.load_verify_locations(ca_certs)
  context.verify_mode = ssl.CERT_REQUIRED
  context.check_hostname = True
  return context


def ensure_ssl_using_protocol(protocol="PROTOCOL_TLS_CLIENT", ca_certs=None):
  """Compatibility alias returning a scoped context without global patching."""
  return create_ssl_context(protocol, ca_certs)


"""
See RFC3986, Appendix B
Tested on the following cases:
  "192.168.54.1"
  "192.168.54.2:7661
  "hdfs://192.168.54.3/foo/bar"
  "ftp://192.168.54.4:7842/foo/bar"

  Returns None if only a port is passed in
"""


def get_host_from_url(uri):
  if uri is None:
    return None

  # if not a string, return None
  if not isinstance(uri, str):
    return None

    # RFC3986, Appendix B
  parts = re.findall(r"^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?", uri)

  # index of parts
  # scheme    = 1
  # authority = 3
  # path      = 4
  # query     = 6
  # fragment  = 8

  host_and_port = uri
  if 0 == len(parts[0][1]):
    host_and_port = parts[0][4]
  elif 0 == len(parts[0][2]):
    host_and_port = parts[0][1]
  elif parts[0][2].startswith("//"):
    host_and_port = parts[0][3]

  if -1 == host_and_port.find(":"):
    if host_and_port.isdigit():
      return None

    return host_and_port
  else:
    return host_and_port.split(":")[0]
