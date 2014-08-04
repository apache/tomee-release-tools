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

import java.io.File;

import static org.apache.openejb.tools.release.util.Exec.exec;

/**
 * @version $Rev$ $Date$
 */
@Command(dependsOn = {Close.class})
public class Legal {

    public static void main(final String[] args) throws Exception {
        String legal = Files.file(Release.builddir, "staging-" + Release.build, "legal").getAbsolutePath();
        org.apache.creadur.tentacles.Main.main(new String[]{
                Release.staging,
                legal
        });

        //We no longer need repo
        final File repo = new File(legal, "repo");
        if (repo.exists() && repo.isDirectory()) {
            exec("rm", "-R", repo.getAbsolutePath());
        }

        legal = new File(legal,"content").getAbsolutePath();

        //Clean up other files
        exec("find", legal, "-name", "*.class", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.properties", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.gif", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.png", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.jpg", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.j", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.js", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.jar", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.sh", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.exe", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.zip", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.gz", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.java", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.original", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.xml", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.xsd", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.dtd", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.htm", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.html", "-type", "f", "-delete");
        exec("find", legal, "-name", "*.conf", "-type", "f", "-delete");

        exec("find", legal, "-type", "d", "-name", "\"*-javadoc.*\"", "-delete");
        exec("find", legal, "-empty", "-type", "d", "-delete");
    }
}
