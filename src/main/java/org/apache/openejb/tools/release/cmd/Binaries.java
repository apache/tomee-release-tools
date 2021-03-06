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

import org.apache.creadur.tentacles.NexusClient;
import org.apache.creadur.tentacles.Platform;
import org.apache.openejb.tools.release.Command;
import org.apache.openejb.tools.release.Release;
import org.apache.openejb.tools.release.util.Files;
import org.apache.openejb.tools.release.util.IO;
import org.apache.xbean.finder.UriSet;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.String.format;
import static org.apache.openejb.tools.release.util.Exec.exec;

/**
 * Little utility that downloads the binaries into
 */
@Command(dependsOn = Close.class)
public class Binaries {

    public static void main(final String[] args) throws Exception {

        final File dir = Files.file(Release.builddir, "staging-" + Release.build, Release.tomeeVersionName);

        { // Make and checkout the binaries dir in svn
            if (dir.exists()) {
                Files.remove(dir);
            }

            Files.mkdirs(dir);

            final String svnBinaryLocation = format("https://dist.apache.org/repos/dist/dev/tomee/staging-%s/%s", Release.build, Release.tomeeVersionName);
            exec("svn", "-m", format("[release-tools] staged binary dir for %s", Release.tomeeVersionName), "mkdir", "--parents", svnBinaryLocation);
            exec("svn", "co", svnBinaryLocation, dir.getAbsolutePath());
        }

        final URI repo = URI.create(Release.staging);

        System.out.println("Downloads: " + dir.getAbsolutePath());
        System.out.println("Repo: " + repo.toASCIIString());

        final NexusClient client = new NexusClient(Platform.aPlatform());
        final UriSet all = new UriSet(client.crawl(repo));

        UriSet binaries = all.include(".*\\.(zip|gz|war).*");
        binaries = binaries.exclude(".*\\.asc\\.(sha1|md5)");
        binaries = binaries.exclude(".*itests.*");
        binaries = binaries.exclude(".*karafee.*");

        for (final URI uri : binaries) {
            final File file = new File(dir, uri.getPath().replaceAll(".*/", "")).getAbsoluteFile();

            System.out.println("Downloading " + file.getName());
            client.download(uri, file);

            exec("svn", "add", file.getAbsolutePath());

            if (file.getName().endsWith(".zip")) {
                final PrintStream out = new PrintStream(IO.write(new File(file.getAbsolutePath() + ".txt")));

                list(file, out);
                out.close();
            }
        }

        exec("svn", "-m", format("[release-tools] staged binaries for %s", Release.tomeeVersionName), "ci", dir.getAbsolutePath());
    }

    private static void list(final File file, final PrintStream out) throws IOException {
        final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        final ZipFile zip = new ZipFile(file);
        final Enumeration<? extends ZipEntry> enumeration = zip.entries();
        while (enumeration.hasMoreElements()) {
            final ZipEntry entry = enumeration.nextElement();
            out.printf("%1$7s %2$2s %3$2s", entry.getSize(), format.format(entry.getTime()), entry.getName());
            out.println();
        }
    }

}
