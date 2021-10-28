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

import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.IssueLinkType;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import lombok.Getter;
import org.apache.openejb.tools.release.Release;
import org.tomitribe.crest.api.Command;
import org.tomitribe.crest.api.Default;
import org.tomitribe.crest.api.Option;
import org.tomitribe.crest.api.PrintOutput;
import org.tomitribe.jamira.core.Account;
import org.tomitribe.jamira.core.Client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atlassian.jira.rest.client.api.domain.IssueLinkType.Direction.OUTBOUND;

/**
 * @version $Rev$ $Date$
 */
@Command("release-notes")
public class ReleaseNotes {

    /**
     * Generate asciidoc release notes for the specified TomEE version.  The resulting
     * asciidoc can be piped to a file in the `tomee-site-generator` repo.  For example:
     *
     *     release release-notes generate 8.0.7 > tomee-site-generator/src/main/jbake/content/tomee-8.0.7-release-notes.html
     *
     * In situations like TomEE 9, we can include release notes for the related TomEE 8
     * version via the --include-versions flag as follows:
     *
     *     release release-notes generate 9.0.0-M7 --include-versions=8.0.7
     *
     * This tool leverages the Jamira command-line tool and library for logging into
     * and talking with JIRA.  It is expected Jamira has been installed and the setup
     * commands run so there is an account available for accessing JIRA.  See the
     * description of --account for more details.
     *
     * @param version The TomEE version as specified in JIRA.  Example "8.0.7"
     * @param includeVersions Any additional versions that should also be represented in these release notes.
     * @param account The account to use to log into JIRA.  See https://github.com/tomitribe/jamira#setup
     */
    @Command
    public PrintOutput generate(final String version,
                                @Option("include-versions") final String includeVersions,
                                @Option("account") @Default("default") final Account account) throws ExecutionException, InterruptedException {

        final Set<String> versions = new HashSet<>();
        versions.add(version);
        if (includeVersions != null) {
            versions.addAll(Arrays.asList(includeVersions.split(" *, *| ")));
        }

        final Client client = account.getClient();
        final SearchRestClient searchClient = client.getSearchClient();

        final Map<String, Issue> issuesByKey = new HashMap<>();
        for (final String ver : versions) {
            final String s = "project = TOMEE AND status = Resolved AND fixVersion = " + ver;
            final SearchResult result = searchClient.searchJql(s).get();

            for (final Issue issue : result.getIssues()) {
                issuesByKey.put(issue.getKey(), issue);
            }
        }

        final List<IssueType> sections = Arrays.asList(
                client.getIssueType("Dependency upgrade"),
                client.getIssueType("New Feature"),
                client.getIssueType("Bug"),
                client.getIssueType("Improvement"),
                client.getIssueType("Task"),
                client.getIssueType("Sub-task")
        );

        return out -> {
            out.println("= Apache TomEE " + version + " Release Notes\n" +
                    ":index-group: Release Notes\n" +
                    ":jbake-type: page\n" +
                    ":jbake-status: published");

            final List<Issue> cveIssues = new ArrayList<>();

            for (final IssueType section : sections) {

                final Map<Boolean, List<Issue>> issuesPartitionedByCve = issuesByKey.values()
                        .stream().filter(issue -> issue.getIssueType().getName().equals(section.getName()))
                        .collect(Collectors.partitioningBy(issue ->
                                issue.getLabels().stream().anyMatch(label -> "cve".equals(label.toLowerCase(Locale.ROOT)))));

                final List<Issue> issues = Stream.of(issuesPartitionedByCve.values())
                        .flatMap(Collection::stream).flatMap(Collection::stream)
                        .collect(Collectors.toList());

                if (issues.size() <= 0) continue;

                cveIssues.addAll(issuesPartitionedByCve.get(true));

                out.println();
                out.printf("== %s%n", section.getName());
                out.println();
                out.println("[.compact]");

                if (section.getName().equals("Dependency upgrade")) {
                    removeSuperseded(issues).stream()
                            .map(Upgrade::new)
                            .sorted(Comparator.comparing(Upgrade::getSummary))
                            .forEach(upgrade -> {
                                out.printf(" - link:https://issues.apache.org/jira/browse/%s[%s] %s%n",
                                        upgrade.getKey(),
                                        upgrade.getKey(),
                                        upgrade.getSummary());

                            });
                } else {
                    for (final Issue issue : issues) {
                        out.printf(" - link:https://issues.apache.org/jira/browse/%s[%s] %s%n",
                                issue.getKey(),
                                issue.getKey(),
                                issue.getSummary());
                    }
                }

            }

            if(cveIssues.size() > 0) {
                //CVE section
                out.println();
                out.printf("== %s%n", "Fixed Common Vulnerabilities and Exposures (CVEs)");
                out.println();
                out.println("[.compact]");

                for (final Issue issue : cveIssues) {
                    out.printf(" - link:https://issues.apache.org/jira/browse/%s[%s] %s%n",
                            issue.getKey(),
                            issue.getKey(),
                            issue.getSummary());
                }
            }
        };
    }

    public static Collection<Issue> removeSuperseded(final List<Issue> issues) {
        final Map<String, Issue> map = new HashMap<>();
        issues.forEach(issue -> map.put(issue.getKey(), issue));

        for (final Issue issue : issues) {
            for (final IssueLink link : issue.getIssueLinks()) {
                final IssueLinkType linkType = link.getIssueLinkType();
                if (!"Supercedes".equalsIgnoreCase(linkType.getName())) continue;
                if (!OUTBOUND.equals(linkType.getDirection())) continue;

                map.remove(link.getTargetIssueKey());
            }
        }

        return map.values();
    }

    public static void main(final String[] args) throws Throwable {
        final List<String> argsList = new ArrayList<String>();

        // lets add the template as the parameter
        argsList.add("release-notes-html.vm");

        // then add system properties to get values replaced in the template
        for (final Field field : Release.class.getFields()) {
            try {
                argsList.add("-D" + field.getName() + "=" + field.get(null));
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        org.codehaus.swizzle.jirareport.Main.main((String[]) argsList.toArray(new String[]{}));
    }

    @Getter
    public static class Upgrade {
        private final Issue issue;
        private final String summary;

        public Upgrade(final Issue issue) {
            this.issue = issue;
            this.summary = normalize(issue.getSummary());
        }

        public String getKey() {
            return issue.getKey();
        }

        public static String normalize(final String summary) {
            return Replace.string(summary)
                    .first("^(upgrade to|upgrade|update to|update) (.*)", "$2")
                    .all(" to ", " ")
                    .all(" in tomee.*", "")
                    .first("apache ", "")
                    .toString();
        }


        private static class Replace {
            private String string;

            public Replace(final String string) {
                this.string = string;
            }

            public Replace all(final String regex, final String replacement) {
                string = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(string).replaceAll(replacement);
                return this;
            }

            public Replace first(final String regex, final String replacement) {
                string = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(string).replaceFirst(replacement);
                return this;
            }

            public static Replace string(final String s) {
                return new Replace(s);
            }

            @Override
            public String toString() {
                return string;
            }
        }
    }
}
