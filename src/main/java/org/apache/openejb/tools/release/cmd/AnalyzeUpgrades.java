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

import lombok.Data;
import org.apache.openejb.tools.release.maven.pom.Dependency;
import org.apache.openejb.tools.release.maven.pom.PomParser;
import org.apache.openejb.tools.release.maven.pom.Project;
import org.apache.openejb.tools.release.util.Exec;
import org.tomitribe.crest.api.Command;
import org.tomitribe.crest.api.table.Table;
import org.tomitribe.util.Files;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Command("upgrades")
public class AnalyzeUpgrades {

    /**
     * Compares the version upgrades from one version to another using
     * the bom files for tomee-webprofile, tomee-microprofile, tomee-plus,
     * and tomee-plume
     * @param from a previous TomEE version
     * @param to a newer TomEE version
     */
    @Table(fields = "artifactId from to")
    @Command("compare")
    public Stream<Upgrade> compare(final String from, final String to) {

        final Map<String, Dependency> previous = getDependencies(from);
        final Map<String, Dependency> current = getDependencies(to);

        return current.keySet().stream()
                .sorted()
                .filter(previous::containsKey)
                .filter(s -> !previous.get(s).getVersion().equals(current.get(s).getVersion()))
                .map(s -> new Upgrade(previous.get(s), current.get(s)));
    }

    private Map<String, Dependency> getDependencies(final String version) {
        final Map<String, Dependency> map = new HashMap<>();
        Stream.of("tomee-webprofile", "tomee-microprofile", "tomee-plus", "tomee-plume")
                .map(s -> getDependencies(s, version))
                .flatMap(Collection::stream)
                .forEach(dependency -> map.put(dependency.getGroupId() + ":" + dependency.getArtifactId(), dependency));
        return map;
    }

    private List<Dependency> getDependencies(final String artifactId, final String version) {
        final File pom = resolve("org.apache.tomee.bom", artifactId, version, "pom");

        final Project project = PomParser.parse(pom);
        return project.getDependencies();
    }

    private File resolve(final String groupId, final String artifactId, final String version, final String packaging) {
        try {
            return mvn(groupId, artifactId, version, packaging);
        } catch (IllegalStateException e) {
            Exec.exec("mvn", "org.apache.maven.plugins:maven-dependency-plugin:3.3.0:get",
                    "-DgroupId=" + groupId,
                    "-DartifactId=" + artifactId,
                    "-Dversion=" + version,
                    "-Dpackaging=" + packaging
            );
            return mvn(groupId, artifactId, version, packaging);
        }
    }

    public static File mvn(final String group, final String artifact, final String version, final String packaging) {
        final File repository = repository();

        // org/apache/tomee/tomee-util/7.1.0/tomee-util-7.1.0.jar
        final File archive = Files.file(
                repository,
                group.replace('.', '/'),
                artifact,
                version,
                String.format("%s-%s.%s", artifact, version, packaging));

        Files.exists(archive);
        Files.file(archive);
        Files.readable(archive);
        return archive;
    }

    private static File repository() {
        final List<String> path = Arrays.asList(System.getProperty("user.home"), ".m2", "repository");

        File file = null;
        ;
        for (final String part : path) {
            if (part == null) file = new File(part);
            else file = new File(file, part);
            Files.exists(file);
            Files.dir(file);
        }
        return file;
    }

    @Data
    public static class Upgrade {
        private final String groupId;
        private final String artifactId;
        private final String from;
        private final String to;

        public Upgrade(final Dependency from, final Dependency to) {
            this.groupId = to.getGroupId();
            this.artifactId = to.getArtifactId();
            this.from = from.getVersion();
            this.to = to.getVersion();
        }
    }

    public static void main(String[] args) {
        new AnalyzeUpgrades().compare("9.0.0-M8", "9.0.0-M9-SNAPSHOT").forEach(System.out::println);
    }
}
