import os
import re
import math
import datetime

from resource_management.core.shell import checked_call

def calc_xmn_from_xms(heapsize_str, xmn_percent, xmn_max):
  """
  @param heapsize_str: str (e.g '1000m')
  @param xmn_percent: float (e.g 0.2)
  @param xmn_max: integer (e.g 512)
  """
  heapsize = int(re.search('\d+',heapsize_str).group(0))
  heapsize_unit = re.search('\D+',heapsize_str).group(0)
  xmn_val = int(math.floor(heapsize*xmn_percent))
  xmn_val -= xmn_val % 8
  
  result_xmn_val = xmn_max if xmn_val > xmn_max else xmn_val
  return str(result_xmn_val) + heapsize_unit

def get_unique_id_and_date():
    code, out = checked_call("hostid")
    id = out.strip()
    
    now = datetime.datetime.now()
    date = now.strftime("%M%d%y")

    return "id{id}_date{date}".format(id=id, date=date)
  
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