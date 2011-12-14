module Puppet::Parser::Functions
  newfunction(:get_category_name, :type => :rvalue) do |args|
    return File.basename(args[0])
  end
end
