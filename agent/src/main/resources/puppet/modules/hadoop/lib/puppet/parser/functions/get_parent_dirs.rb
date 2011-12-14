module Puppet::Parser::Functions
  newfunction(:get_parent_dirs, :type => :rvalue) do |args|
    dir = args[0]
    dirs = Array.new
    temp = "/"
    # dir.split('/').each {|x| temp = temp + x + "/", dirs << temp}
    dir.split('/').each {|x| temp = temp+x+"/", dirs << temp}
    return dirs
  end
end
