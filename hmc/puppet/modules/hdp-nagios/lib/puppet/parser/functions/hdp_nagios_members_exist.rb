module Puppet::Parser::Functions
  newfunction(:hdp_nagios_members_exist, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    host_type = args[0]
    hg_defs = function_hdp_template_var("hostgroup_defs")
    if  hg_defs.kind_of?(Hash)
      #TODO: see if needed    Puppet::Parser::Functions.autoloader.loadall
      member_info = (hg_defs[host_type]||{})['host_member_info']
      member_info and not function_hdp_is_empty(function_hdp_host(member_info))
    else
      nil
    end
  end
end
