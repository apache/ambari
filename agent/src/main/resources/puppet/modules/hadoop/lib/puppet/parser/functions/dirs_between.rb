module Puppet::Parser::Functions
  newfunction(:dirs_between, :type => :rvalue, :doc => "Generate a list of pathnames") do |args|
    subbottom = args[0]
    subdirs = []
    while subbottom != "/"
        subbottom, component = File.split(subbottom)
        subdirs.unshift(component)
    end
    dir = '/'
    paths = [ ]
    newpaths = [ ]
    while subdirs.length > 0
        component = subdirs.shift()
        dir = File.join(dir, component)
        paths.push(dir)
    end
    paths.each do |d| 
      if !File.exists?(d)
#        Dir.mkdir(d)
        newpaths.push(d)
      end
    end
    return newpaths
  end
end
