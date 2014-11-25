import os
import tempfile
from resource_management.core import shell

# os.chown replacement
def chown(path, owner, group):
  if owner:
    shell.checked_call(["chown", owner, path], sudo=True)
  if group:
    shell.checked_call(["chgrp", owner, path], sudo=True)
    
# os.chmod replacement
def chmod(path, mode):
  shell.checked_call(["chmod", oct(mode), path], sudo=True)
  
# os.makedirs replacement
def makedirs(path, mode):
  shell.checked_call(["mkdir", "-p", path], sudo=True)
  chmod(path, mode)
  
# os.makedir replacement
def makedir(path, mode):
  shell.checked_call(["mkdir", path], sudo=True)
  chmod(path, mode)
  
# os.symlink replacement
def symlink(source, link_name):
  shell.checked_call(["ln","-sf", source, link_name], sudo=True)
  
# os.link replacement
def link(source, link_name):
  shell.checked_call(["ln", "-f", source, link_name], sudo=True)
  
# os unlink
def unlink(path):
  shell.checked_call(["rm","-f", path], sudo=True)
  
# fp.write replacement
def create_file(filename, content):
  """
  if content is None, create empty file
  """
  tmpf = tempfile.NamedTemporaryFile()
  
  if content:
    with open(tmpf.name, "wb") as fp:
      fp.write(content)
  
  with tmpf:    
    shell.checked_call(["cp", "-f", tmpf.name, filename], sudo=True)
    
  # set default files mode
  chmod(filename, 0644)
    
# fp.read replacement
def read_file(filename):
  tmpf = tempfile.NamedTemporaryFile()
  shell.checked_call(["cp", "-f", filename, tmpf.name], sudo=True)
  
  with tmpf:
    with open(tmpf.name, "rb") as fp:
      return fp.read()
