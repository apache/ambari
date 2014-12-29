from resource_management.libraries.script import UnknownConfiguration

__all__ = ["get_namenode_states", "get_active_namenode"]

HDFS_NN_STATE_ACTIVE = 'active'
HDFS_NN_STATE_STANDBY = 'standby'

NAMENODE_HTTP_FRAGMENT = 'dfs.namenode.http-address.{0}.{1}'
JMX_URI_FRAGMENT = "http://{0}/jmx?qry=Hadoop:service=NameNode,name=NameNodeStatus"
  
def get_namenode_states(hdfs_site):
  active_namenodes = []
  standby_namenodes = []
  unknown_namenodes = []
  
  name_service = hdfs_site['dfs.nameservices']
  nn_unique_ids_key = 'dfs.ha.namenodes.' + name_service

  # now we have something like 'nn1,nn2,nn3,nn4'
  # turn it into dfs.namenode.[property].[dfs.nameservices].[nn_unique_id]
  # ie dfs.namenode.http-address.hacluster.nn1
  nn_unique_ids = hdfs_site[nn_unique_ids_key].split(',')
  for nn_unique_id in nn_unique_ids:
    key = NAMENODE_HTTP_FRAGMENT.format(name_service,nn_unique_id)

    if key in hdfs_site:
      # use str() to ensure that unicode strings do not have the u' in them
      value = str(hdfs_site[key])

      try:
        jmx_uri = JMX_URI_FRAGMENT.format(value)
        state = get_value_from_jmx(jmx_uri,'State')

        if state == HDFS_NN_STATE_ACTIVE:
          active_namenodes.append(value)
        elif state == HDFS_NN_STATE_STANDBY:
          standby_namenodes.append(value)
        else:
          unknown_namenodes.append(value)
      except:
        unknown_namenodes.append(value)
        
  return active_namenodes, active_namenodes, unknown_namenodes

def get_active_namenode(hdfs_site):
  active_namenodes = get_namenode_states(hdfs_site)[0]
  if active_namenodes:
    return active_namenodes[0]
  else:
    return UnknownConfiguration('fs_root')