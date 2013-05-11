Puppet::Type.type(:append_line).provide(:ruby) do

  def exists?
    File.readlines(resource[:path]).find do |line|
      line.chomp == resource[:line].chomp
    end
  end

  def create
    File.open(resource[:path], 'a') do |fh|
      fh.puts resource[:line]
    end
  end

end
