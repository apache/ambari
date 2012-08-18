#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
module Puppet::Parser::Functions
  newfunction(:hdp_calc_xmn_from_xms, :type => :rvalue) do |args|
    heapsize_orig_str = args[0].to_s
    xmn_percent = args[1].to_f
    xmn_max = args[2].to_i
    heapsize_str = heapsize_orig_str.gsub(/\D/,"")
    heapsize = heapsize_str.to_i
    heapsize_unit = heapsize_orig_str.gsub(/\d/,"")
    xmn_val = heapsize*xmn_percent
    xmn_val = xmn_val.floor.to_i
    xmn_val = xmn_val/8
    xmn_val = xmn_val*8
    xmn_val = xmn_val > xmn_max ? xmn_max : xmn_val
    xmn_val_str = "" + xmn_val.to_s + heapsize_unit
    xmn_val_str
  end
end
