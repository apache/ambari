__all__ = ["get_unique_id_and_date"]
import datetime
from resource_management.core.shell import checked_call

def get_unique_id_and_date():
    code, out = checked_call("hostid")
    id = out.strip()
    
    now = datetime.datetime.now()
    date = now.strftime("%M%d%y")

    return "id{id}_date{date}".format(id=id, date=date)
