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
import org.codehaus.swizzle.stream.ReplaceStringInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static java.lang.String.format;
import static org.apache.openejb.tools.release.util.Exec.cd;
import static org.apache.openejb.tools.release.util.Exec.exec;
import static org.apache.openejb.tools.release.util.Files.collect;

/**
 * @version $Rev$ $Date$
 */
@Command(dependsOn = {Branch.class})
public class FinalizeVersions {

    public static void main(String... args) throws Exception {

        final File dir = new File("/tmp/release/branch");
        Files.mkdir(dir);

        cd(dir);

        final String branch = Release.branches + Release.openejbVersionName;

        // Checkout the branch
        exec("svn", "co", branch);

        final File workingCopy = cd(new File(dir + "/" + Release.openejbVersionName));

        updateVersions(workingCopy);
        updateExamplesVersions(Files.file(workingCopy, "examples"));

//        exec("svn", "-m", "[release-tools] finalizing versions for " + Release.openejbVersionName, "ci");
        exec("svn", "-m", "finalizing versions in arquillian.xml files", "ci");
    }

    private static void updateVersions(File workingCopy) throws IOException {

        final List<File> files = collect(workingCopy, ".*pom.xml");
        files.addAll(collect(workingCopy, ".*build.xml"));
        files.addAll(collect(workingCopy, ".*arquillian.xml"));

        for (File file : files) {
            InputStream in = IO.read(file);

            in = new ReplaceStringInputStream(in, "1.0.0-beta-3-SNAPSHOT", "1.0.0");
            in = new ReplaceStringInputStream(in, "4.0.0-beta-3-SNAPSHOT", "4.0.0");

            update(file, in);
        }

    }

    private static void updateExamplesVersions(File workingCopy) throws IOException {

        final List<File> files = collect(workingCopy, ".*pom.xml");
        files.addAll(collect(workingCopy, ".*build.xml"));

        for (File file : files) {
            InputStream in = IO.read(file);

            in = new ReplaceStringInputStream(in, "1.1-SNAPSHOT", "1.0");
            in = new ReplaceStringInputStream(in, "1.1-SNAPSHOT", "1.0");
            in = new ReplaceStringInputStream(in, "0.0.1-SNAPSHOT", "1.0");

            update(file, in);
        }

    }

    private static void update(File dest, InputStream in) throws IOException {
        final File tmp = new File(dest.getAbsolutePath() + "~");

        final OutputStream out = IO.write(tmp);

        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
        out.close();
        if (!tmp.renameTo(dest)) throw new RuntimeException(String.format("Rename failed: mv \"%s\" \"%s\"", tmp.getAbsolutePath(), dest.getAbsolutePath()));
    }
}
