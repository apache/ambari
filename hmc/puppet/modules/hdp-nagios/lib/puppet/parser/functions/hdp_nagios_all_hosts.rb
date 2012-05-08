module Puppet::Parser::Functions
  newfunction(:hdp_nagios_all_hosts, :type => :rvalue) do 
    hg_defs = function_hdp_template_var("hostgroup_defs")
    ret = Array.new
    if hg_defs.kind_of?(Hash)
      hg_defs.each_value do |info|
        h = function_hdp_host(info['host_member_info'])
        unless function_hdp_is_empty(h)
          ret += [h].flatten 
        end
      end
    end
    ret.uniq
  end
end
