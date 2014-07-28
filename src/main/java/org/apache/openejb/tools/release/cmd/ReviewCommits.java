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

import org.apache.maven.settings.Server;
import org.apache.openejb.tools.release.Command;
import org.apache.openejb.tools.release.Commit;
import org.apache.openejb.tools.release.Maven;
import org.apache.openejb.tools.release.Release;
import org.apache.openejb.tools.release.util.Exec;
import org.apache.openejb.tools.release.util.Join;
import org.apache.openejb.tools.release.util.ObjectList;
import org.apache.openejb.tools.release.util.Options;
import org.codehaus.swizzle.jira.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
@Command
public class ReviewCommits {

    private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    public static void main(final String... args) throws Exception {

        final String tag = Release.tags + Release.tomeeVersionName;

        final InputStream in = Exec.read("svn", "log", "--verbose", "--xml", "-rHEAD:{" + Release.lastReleaseDate + "}", tag);

        final JAXBContext context = JAXBContext.newInstance(Commit.Log.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();

        final Commit.Log log = (Commit.Log) unmarshaller.unmarshal(in);

        ObjectList<Commit> commits = log.getCommits();
        commits = commits.ascending("revision");

        for (final Commit commit : commits) {
            final String[] tokens = commit.getMessage().split("[^A-Z0-9-]+");
            for (final String token : tokens) {
                if (token.matches("(OPENEJB|TOMEE)-[0-9]+")) {
                    try {
                        addIssue(getJira().getIssue(token));
                    } catch (final Exception e) {
                        System.out.printf("Invalid JIRA '%s'\n", token);
                    }
                }
            }
        }

        final Date reviewed = new SimpleDateFormat("yyyy-MM-dd").parse("2012-01-05");
        commits = commits.greater("date", reviewed);
        commits = commits.subtract(commits.contains("message", "OPENEJB-"));
        commits = commits.subtract(commits.contains("message", "TOMEE-"));

        System.out.printf("Are you ready to review %s commits?", commits.size());
        System.out.println();

        for (final Commit commit : commits) {
            handle(commit);
        }

//        for (Commit commit : commits) {
//            System.out.println(commit);
//        }
//

    }

    public static boolean handle(final Commit commit) {
        for (final Commit.Path path : commit.getPaths()) {
            System.out.printf(" %s %s", path.getAction(), path.getPath());
            System.out.println();
        }
        System.out.println(commit);

        System.out.printf("[%s]: ", Join.join(", ", Key.values()));

        final String line = readLine().toUpperCase();

        try {
            final Key key = Key.valueOf(line);
            if (!key.pressed(commit)) handle(commit);
        } catch (final IllegalArgumentException e) {
            return handle(commit);
        }

        return true;
    }

    private static String prompt(final String s) {
        System.out.printf("%s : ", s);
        final String value = readLine();
        return (value == null || value.length() == 0) ? s : value;
    }

    /**
     * Sort of a mini clipboard of recently seen issues
     */
    private static List<Issue> last = new ArrayList<Issue>();

    private static void addIssue(final Issue issue) {
        last.remove(issue);
        last.add(0, issue);
        while (last.size() > 20) {
            last.remove(last.size() - 1);
        }
    }

    private static Jira jira;
    private static List<IssueType> issueTypes = new ArrayList<IssueType>();

    public static Jira getJira() {
        final Server server = Maven.settings.getServer("apache.jira");
        final String username = server.getUsername();
        final String password = server.getPassword();

        if (jira == null) {
            try {
                final Options options = new Options(System.getProperties());
                final Jira jira = new Jira("http://issues.apache.org/jira/rpc/xmlrpc");
                jira.login(username, password);
                ReviewCommits.jira = jira;

                issueTypes.add(jira.getIssueType("Improvement"));
                issueTypes.add(jira.getIssueType("New Feature"));
                issueTypes.add(jira.getIssueType("Bug"));
                issueTypes.add(jira.getIssueType("Task"));
                issueTypes.add(jira.getIssueType("Dependency upgrade"));

            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return jira;
    }

    public static enum Key {
        V(new Action() {
            @Override
            public boolean perform(final Commit commit) {
                Exec.exec("open", String.format("http://svn.apache.org/viewvc?view=revision&revision=%s", commit.getRevision()));
                return false;
            }
        }),

        // ASSOCIATE with a JIRA issue
        A(new Action() {
            @Override
            public boolean perform(final Commit commit) {
                int i = 0;

                final List<Issue> issues = new ArrayList<Issue>(last);

                for (final Issue issue : issues) {
                    System.out.printf("%s) %s: %s\n", i++, issue.getKey(), issue.getSummary());
                }

                final String[] split = prompt("issues?").split(" +");
                for (final String key : split) {

                    final Issue issue = resolve(key, issues);
                    if (issue == null) {
                        System.out.println("No such issue " + key);
                        continue;
                    }

                    addIssue(issue);
                    System.out.printf("Associating %s", issue.getKey());
                    System.out.println();
                    updateCommitMessage(commit, issue);
                }

                return false;
            }

        }),

        // NEXT commit
        N(new Action() {
            @Override
            public boolean perform(final Commit commit) {
                return true;
            }
        }),

        // CREATE jira
        C(new Action() {

            @Override
            public boolean perform(final Commit commit) {

                try {
                    final Jira jira = getJira();

                    final String summary = prompt("summary");
                    final String project = prompt("TOMEE ('o' for OPENEJB else TOMEE)");
                    final String version = prompt("TOMEE".equals(project) ? Release.tomeeVersion : Release.openejbVersion);
                    final String type = prompt("Improvement (type first letters)").toLowerCase();

                    final Issue issue = new Issue();

                    if (project.equalsIgnoreCase("o")) {
                        issue.setProject(jira.getProject("OPENEJB"));
                    } else {
                        issue.setProject(jira.getProject(project));
                    }
                    issue.setSummary(summary);

                    // Set default to Improvement
                    issue.setType(issueTypes.get(0));
                    for (final IssueType issueType : issueTypes) {
                        if (issueType.getName().toLowerCase().startsWith(type)) {
                            issue.setType(issueType);
                            break;
                        }
                    }

                    final Version v = jira.getVersion(issue.getProject(), version);
                    issue.getFixVersions().add(v);

                    System.out.printf("%s %s\n%s %s\n", issue.getProject(), issue.getSummary(), issue.getType(), Join.join(",", issue.getFixVersions()));
                    final String prompt = prompt("create? (yes or no)");

                    if (prompt.equals("create?") || prompt.equals("yes")) {
                        try {
                            final Issue jiraIssue = createIssue(jira, issue);
                            addIssue(jiraIssue);

                            System.out.println(jiraIssue.getKey());

                            updateCommitMessage(commit, jiraIssue);
                        } catch (final Exception e) {
                            System.out.println("Could not create jira issue");
                            e.printStackTrace();
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                return false;
            }

        });

        private static Issue createIssue(final Jira jira, final Issue issue) throws Exception {
            trimIssue(issue);

            return jira.createIssue(issue);
        }


        private static Issue resolve(final String key, final List<Issue> issues) {
            try {
                return issues.get(new Integer(key));
            } catch (final Exception e) {
            }


            try {
                return getJira().getIssue(key);
            } catch (final Exception e) {
            }

            return null;
        }

        private static void updateCommitMessage(final Commit commit, final Issue issue) {
            final String oldMessage = commit.getMessage();

            if (oldMessage.contains(issue.getKey())) return;

            final String newMessage = String.format("%s\n%s: %s", oldMessage, issue.getKey(), issue.getSummary());

            Exec.exec("svn", "propset", "-r", commit.getRevision() + "", "--revprop", "svn:log", newMessage, "https://svn.apache.org/repos/asf");
        }


        private final Action action;

        Key(final Action action) {
            this.action = action;
        }

        public boolean pressed(final Commit commit) {
            return action.perform(commit);
        }
    }

    public static String v(final String version) {
        return version.replaceFirst("^[a-z]+-", "");
    }

    public static Issue trimIssue(final Issue issue) throws NoSuchFieldException, IllegalAccessException {
        toMap(issue).remove("votes");

        for (final Version version : issue.getFixVersions()) {
            toMap(version).remove("archived");
            toMap(version).remove("sequence");
            toMap(version).remove("released");
            toMap(version).remove("releaseDate");
        }

        return issue;
    }

    public static Map toMap(final MapObject issue) throws NoSuchFieldException, IllegalAccessException {
        final Field fields = MapObject.class.getDeclaredField("fields");
        fields.setAccessible(true);
        return (Map) fields.get(issue);
    }

    public static interface Action {

        boolean perform(Commit commit);
    }

    private static String readLine() {
        try {
            return in.readLine();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
