class HttpClientInvoker():
  @staticmethod
  def http_client_invoke_side_effects(*args, **kwargs):
      print locals()
      
      if args[0] != "GET" and args[0] != "DELETE":
        return "", 201 , "text/plain"
      
      mocked_code = 200 
      mocked_content = "text/plain"
      if args[1] == "//clusters/test1":
        mocked_response = open('json/clustermodel_get_cluster.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/hosts/myhost":
        mocked_response = open('json/clustermodel_get_host.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/hosts?fields=*":
        mocked_response = open('json/clustermodel_get_all_hosts.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/configurations?type=global&tag=version1":
        mocked_response = open('json/clustermodel_get_global_config.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/configurations?type=core-site&tag=version1":
        mocked_response = open('json/clustermodel_get_core_site_config.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/configurations?type=hdfs-site&tag=version1":
        mocked_response = open('json/clustermodel_get_hdfs_site_config.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/configurations?type=mapred-site&tag=version1":
        mocked_response = open('json/clustermodel_get_mapred_site_config.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/services/GANGLIA":
        mocked_response = open('json/clustermodel_get_service.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/services?fields=*":
        mocked_response = open('json/clustermodel_get_all_services.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//stacks2/HDP/versions/2.0.5/stackServices/HDFS/serviceComponents?fields=*":
        mocked_response = open('json/get_components_from_stack.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//hosts":
        mocked_response = open('json/get_all_hosts.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test6/services/GANGLIA":
        mocked_response = open('json/get_cluster_service.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test6/hosts/r01wn01/host_components/NAMENODE":
        mocked_response = open('json/get_host_component.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test6/hosts/r01wn01/host_components?ServiceComponentInfo":
        mocked_response = open('json/get_host_components.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test6/services/GANGLIA/components/GANGLIA_MONITOR":
        mocked_response = open('json/get_service_component.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test6/services/GANGLIA/components?fields=*":
        mocked_response = open('json/get_service_components.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/hosts/deleted_nonexistant_cluster":
        mocked_response = open('json/error_deleting_host.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
      elif args[1] == "//clusters/test1/hosts/hostname01":
        mocked_response = None
        return mocked_response, mocked_code , mocked_content
      else:
        print "Unknown url: %s" % args[1]
        