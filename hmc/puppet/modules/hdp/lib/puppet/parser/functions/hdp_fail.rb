module Puppet::Parser::Functions
  newfunction(:hdp_fail) do |args|
    args = [args].flatten
    msg = args[0]
    function_fail(msg)
  end
end
