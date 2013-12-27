// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


using System;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace Ambari_Result
{
    static class Program
    {
        [STAThread]
        static void Main()
        {
            string file = Environment.GetEnvironmentVariable("tmp") + @"\ambari_failed.txt";
            if (File.Exists(file))
            {
                string text = File.ReadAllText(file);
                DialogResult result = MessageBox.Show("Uninstallation on next nodes failed.\r\nPlease proceed with manual uninstallation for these nodes\r\n" + text, "Ambari-SCOM Warning", MessageBoxButtons.OK, MessageBoxIcon.Warning, MessageBoxDefaultButton.Button1,MessageBoxOptions.DefaultDesktopOnly);
                if (result == DialogResult.OK)
                {
                    Environment.Exit(0);
                }
            }
        }
    }
}
