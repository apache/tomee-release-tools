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
import org.apache.openejb.tools.release.util.IO;
import org.apache.openejb.tools.release.util.ObjectList;
import org.apache.openejb.tools.release.util.Options;
import org.codehaus.swizzle.jira.Issue;
import org.codehaus.swizzle.jira.Jira;
import org.codehaus.swizzle.jira.Status;
import org.codehaus.swizzle.jira.Version;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @version $Rev$ $Date$
 */
@Command
public class UpdateJiras {

    private static SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");

    public static void _main(String... args) throws Exception {

        final String tag = Release.tags + Release.openejbVersionName;

        updateJiraFixVersions(tag, "HEAD", "{" + Release.lastReleaseDate + "}", Release.tomeeVersion, Release.openejbVersion);
    }

    public static void main(String[] args) throws Exception {

//        final List<String> jiraKeys = getJiraKeys("TOMEE-1TOMEE-2TOMEE-3TOMEE-4");
//        for (String jiraKey : jiraKeys) {
//            System.out.println(jiraKey);
//        }

        updateJiraFixVersions("http://svn.apache.org/repos/asf/tomee/tomee/branches/tomee-1.5.2", "1417791","HEAD", "1.5.2" ,"4.5.2");
    }

    static final Pattern pattern = Pattern.compile("((OPENEJB|TOMEE)-[0-9]+)");

    private static void updateJiraFixVersions(String repo, final String start, final String end, final String tomeeVersion, final String openejbVersion) throws Exception {
        final InputStream in = Exec.read("svn", "log", "--verbose", "--xml", "-r" + start + ":" + end, repo);

        final String content = IO.slurp(in).toUpperCase();
        System.out.println(content);
        final Set<String> keys = new HashSet<String>(getJiraKeys(content));

        for (String key : keys) {
            System.out.println(key);
        }

        final State state = new State();
        final Version tomee = state.jira.getVersion("TOMEE", tomeeVersion);
        final Version openejb = state.jira.getVersion("OPENEJB", openejbVersion);


        jiras: for (String key : keys) {
            if ("TOMEE-1".equals(key)) continue;

            final Issue issue = state.jira.getIssue(key);
            if (issue == null) continue;

            final List<Version> fixVersions = issue.getFixVersions();

            for (Version fixVersion : fixVersions) {
                if (fixVersion.getReleased()) continue jiras;
            }

            Version version = null;
            if (issue.getKey().startsWith("TOMEE")) {
                version = tomee;
            }

            if (issue.getKey().startsWith("OPENEJB")) {
                version = openejb;
            }

            System.out.println("Updating " + key);
            final Set<String> ids = new HashSet<String>();
            for (Version v : issue.getFixVersions()) {
                if (v.getName().equals("1.6.0.beta1")) continue;
                ids.add(v.getId() + "");
            }

            final int versions = ids.size();
            ids.add(version.getId() + "");

            if (versions != ids.size()) {
                try {
                    System.out.printf("Adding version to %s\n", issue.getKey());

                    final Hashtable map = new Hashtable();
                    map.put("fixVersions", new Vector(ids));
                    call(state.jira, "updateIssue", issue.getKey(), map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Close those jiras with links to the commits
        if (false) for (IssueCommits ic : state.map.values()) {
            final Issue issue = ic.getIssue();

            final Version version;
            if (issue.getKey().startsWith("TOMEE-")) {
                version = state.jira.getVersion("TOMEE", tomeeVersion);
            } else if (issue.getKey().startsWith("OPENEJB-")) {
                version = state.jira.getVersion("OPENEJB", openejbVersion);
            } else {
                continue;
            }

            final Set<String> ids = new HashSet<String>();
            for (Version v : issue.getFixVersions()) {
                ids.add(v.getId() + "");
            }

            final int versions = ids.size();
            ids.add(version.getId() + "");

            if (versions != ids.size()) {
                try {
                    System.out.printf("Adding version to %s\n", issue.getKey());

                    final Hashtable map = new Hashtable();
                    map.put("fixVersions", new Vector(ids));
                    call(state.jira, "updateIssue", issue.getKey(), map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean isClosed(Issue issue) {
        final String name = issue.getStatus().getName();
        if ("Closed".equals(name)) return true;
        if ("Resolved".equals(name)) return true;

        return false;
    }

    private static List<String> getJiraKeys(String message) {
        final Matcher matcher = pattern.matcher(message);

        final List<String> list = new ArrayList<String>();

        while (matcher.find()) {
            list.add(matcher.group());
        }

        return list;
    }

    private static void call(Jira jira, String command, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Method method = Jira.class.getDeclaredMethod("call", String.class, Object[].class);
        method.setAccessible(true);
        method.invoke(jira, command, args);
    }

    public static class State {

        private Jira jira;
        private Map<String, IssueCommits> map = new HashMap<String, IssueCommits>();

        public State() throws Exception {
            Server server = Maven.settings.getServer("apache.jira");
            final String username = server.getUsername();
            final String password = server.getPassword();

            final Options options = new Options(System.getProperties());
            jira = new Jira("http://issues.apache.org/jira/rpc/xmlrpc");
            jira.login(username, password);
        }

        public synchronized IssueCommits get(String key) {
            final IssueCommits commits = map.get(key);
            if (commits != null) return commits;

            final IssueCommits issueCommits = new IssueCommits(jira.getIssue(key));
            map.put(issueCommits.getKey(), issueCommits);

            return issueCommits;
        }
    }

    public static class IssueCommits {
        private final String key;
        private final Issue issue;
        Set<Commit> commits = new LinkedHashSet<Commit>();

        public IssueCommits(Issue issue) {
            this.key = issue.getKey();
            this.issue = issue;
        }

        public Issue getIssue() {
            return issue;
        }

        public String getKey() {
            return key;
        }

        public Set<Commit> getCommits() {
            return commits;
        }

        public synchronized void add(Commit commit) {
            this.commits.add(commit);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IssueCommits that = (IssueCommits) o;

            if (!key.equals(that.key)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}
