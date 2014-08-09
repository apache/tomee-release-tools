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
        final String legal = Files.file(Release.builddir, "staging-" + Release.build, "legal").getAbsolutePath();
        org.apache.creadur.tentacles.Main.main(new String[]{
                Release.staging,
                legal
        });

        //We no longer need repo
        final File repo = new File(legal, "repo");
        if (repo.exists() && repo.isDirectory()) {
            exec("rm", "-R", repo.getAbsolutePath());
        }

        //Clean all but required
        clean(new File(legal, "content").getAbsoluteFile());
    }

    private static void clean(final File f) {
        if (f == null) {
            return;
        }

        if (f.isDirectory()) {

            final File[] files = f.listFiles();

            if (files != null && files.length > 0) {
                for (final File file : files) {
                    if (file.isDirectory()) {
                        clean(file);
                    } else {
                        if (!isLicenceOrNotice(file) && !file.delete()) {
                            file.deleteOnExit();
                        }
                    }
                }
            }

            if (isEmpty(f) && !f.delete()) {
                f.deleteOnExit();
            }

        } else if (!isLicenceOrNotice(f) && !f.delete()) {
            f.deleteOnExit();
        }
    }

    private static boolean isEmpty(final File f) {
        if (!f.isDirectory()) {
            return false;
        }
        File[] list = f.listFiles();
        return (list == null || list.length == 0);
    }

    private static boolean isLicenceOrNotice(final File f) {
        if (f.isDirectory()) {
            return false;
        }

        final String name = f.getName().toLowerCase();
        return (name.contains("license") || name.contains("notice"));
    }
}
