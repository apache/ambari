__all__ = ["get_kinit_path"]
import os

def get_kinit_path(pathes_list):
  """
  @param pathes: comma separated list
  """
  kinit_path = ""
  
  for x in pathes_list:
    if not x:
      continue
    
    path = os.path.join(x,"kinit")

    if os.path.isfile(path):
      kinit_path = path
      break
    
  return kinit_path
