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
import org.apache.openejb.tools.release.util.Exec;
import org.apache.openejb.tools.release.util.Files;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @version $Rev$ $Date$
 */
@Command
public class CompareLibraries {

    public static void main(final String... args) throws IOException {
        final File repository = Files.file(System.getProperty("user.home"), ".m2", "repository", "org", "apache", "tomee");

        // /Users/dblevins/.m2/repository/org/apache/openejb/apache-tomee/1.0.0/apache-tomee-1.0.0-webprofile.zip

        diff(repository, "apache-tomee", "8.0.0-M3", "8.0.0", "webprofile");
        diff(repository, "apache-tomee", "8.0.0-M3", "8.0.0", "microprofile");
        diff(repository, "apache-tomee", "8.0.0-M3", "8.0.0", "plus");
        diff(repository, "apache-tomee", "8.0.0-M3", "8.0.0", "plume");

        diff(repository, "openejb-standalone", "8.0.0-M3", "8.0.0", null);


    }

    private static void diff(final File repository, final String artifactId, final String versionA, final String versionB, final String classifier) throws IOException {
        final String repo1Url = "http://repo1.maven.apache.org/maven2";
        final String stagingUrl = Release.mvnStaging;
        System.out.printf("\n%s %s %s\n\n", artifactId, versionB, (classifier == null) ? "" : classifier);

        final File previous = artifact(repository, artifactId, versionA, classifier, repo1Url);
        final File current = artifact(repository, artifactId, versionB, classifier, stagingUrl);

        final Map<String, FileData> a = libraries(previous);
        final Map<String, FileData> b = libraries(current);

        for (final String key : a.keySet()) {
            if (b.containsKey(key)) continue;
            System.out.printf("  DEL %s\n", path(a, key));
        }

        for (final String key : b.keySet()) {
            if (a.containsKey(key)) continue;
            System.out.printf("  ADD %s\n", path(b, key));
        }

        System.out.println();
        final long change = current.length() - previous.length();
        System.out.printf("  change: %s%.2f MB\n", change > 0 ? "+" : "", toMB(change));
        System.out.printf("  total : %.2f MB\n\n", toMB(current.length()));

    }

    private static double toMB(final double length) {
        return length / 1024 / 1024;
    }

    private static String path(final Map<String, FileData> b, final String key) {
        final FileData value = b.get(key);
        return value.file.getName() + " [" + String.format("%.2f", value.size) + " ko]";
//        return b.get(key).getAbsolutePath().replace(new File("").getAbsolutePath(),"");
    }

    private static Map<String, FileData> libraries(final File file) throws IOException {

        final Map<String, FileData> map = new TreeMap<String, FileData>();
        for (final FileData jar : list(file)) {
            final String name = jar.file.getName().replaceFirst("-[0-9].+", "");
            map.put(name, jar);
        }

        return map;
    }

    private static File artifact(final File repository, final String artifactId, final String version, final String classifier, final String repoUrl) {

        final String zip = classifier != null ? artifactId + "-" + version + "-" + classifier + ".zip" : artifactId + "-" + version + ".zip";

        final File file = Files.file(repository, artifactId, version, zip);

        if (file.exists()) return file;

        // download the artifact
        final String artifact;

        if (classifier != null) {
            artifact = "-Dartifact=" + String.format("org.apache.tomee:%s:%s:%s:%s", artifactId, version, "zip", classifier);
        } else {
            artifact = "-Dartifact=" + String.format("org.apache.tomee:%s:%s:%s", artifactId, version, "zip");
        }

        final int i = Exec.exec(mvn(), "-X", "org.apache.maven.plugins:maven-dependency-plugin:2.4:get", "-DrepoUrl=" + repoUrl, artifact);

        if (i != 0) {
            throw new IllegalStateException("Download failed: " + i);
        }

        return artifact(repository, artifactId, version, classifier, repoUrl);
    }

    private static String mvn() {
        final String[] paths = {
                System.getenv("M2_HOME") + "/bin/mvn",
                System.getenv("MAVEN_HOME") + "/bin/mvn",
                "/usr/bin/mvn",
                "/usr/local/bin/mvn"
        };

        for (final String path : paths) {
            final File file = new File(path);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }

        return "mvn";
    }

    private static List<FileData> list(final File previousFile) throws IOException {
        final List<FileData> files = new ArrayList<FileData>();

        final ZipFile previousZip = new ZipFile(previousFile);
        final ArrayList<? extends ZipEntry> list = Collections.list(previousZip.entries());
        for (final ZipEntry entry : list) {
            if (entry.isDirectory()) continue;
            if (!entry.getName().endsWith(".jar")) continue;
            files.add(new FileData(new File(entry.getName()), entry.getSize() * 0.001));
        }
        return files;
    }

    public static class FileData {
        public File file;
        public double size;

        private FileData(final File file, final double size) {
            this.file = file;
            this.size = size;
        }
    }
}
