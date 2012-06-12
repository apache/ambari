module Puppet::Parser::Functions
  newfunction(:hdp_host_attribute, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    hash,attr,source = args
    ret_val = lambda do |hash,attr,s|
      ret = ""
      ndx = hash[s]
      unless function_hdp_is_empty(ndx)
        val = ndx[attr]
        ret = function_hdp_is_empty(val) ? "" : val
      end
      ret
    end
    if source.kind_of?(Array)
      source.map{|s|ret_val.call(hash,attr,s)}
    else
     ret_val.call(hash,attr,source)
    end
  end
end
