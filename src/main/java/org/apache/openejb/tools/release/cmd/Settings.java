/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.tools.release.cmd;

import org.apache.openejb.tools.release.Command;
import org.apache.openejb.tools.release.Release;
import org.apache.openejb.tools.release.util.Files;
import org.apache.openejb.tools.release.util.IO;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @version $Rev$ $Date$
 */
public class Settings {

    @Command
    public static class Save {
        public static void main(String... args) throws IOException {
            final Map<String,Object> map = Release.map();

            final File file = Files.file(System.getProperty("user.home"), ".tomee-release.properties");
            final OutputStream write = IO.write(file);

            final Properties properties = new Properties();
            properties.putAll(map);
            properties.store(write, "Default settings");
            write.close();
        }
    }


    @Command
    public static class Load {
        public static void main(String... args) throws IOException {
            final File file = Files.file(System.getProperty("user.home"), ".tomee-release.properties");
            if (!file.exists()) return;
            final Properties properties1 = IO.readProperties(IO.read(file));

            final Map map = Release.map();
            map.putAll(properties1);
        }
    }


}
