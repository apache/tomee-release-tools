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
package org.apache.openejb.tools.release.util;

import java.io.*;
import java.util.*;

/**
 * @version $Rev$ $Date$
 */
public class Exec {

    public static File dir;
    public static Map<String, String> env = new HashMap<String, String>();


    public static File cd(final String string) {
        final File file = new File(string);
        return cd(file);
    }

    public static File cd(final File file) {
        System.out.println("cd " + file);

        System.setProperty("user.dir", file.getAbsolutePath());
        dir = file;
        return file;
    }

    public static void export(final String key, final String value) {
        env.put(key, value);
    }

    public static int exec(final String program, final String... args) throws RuntimeException {
        try {
            final Process process = call(program, args);
            Pipe.pipe(process);

            return process.waitFor();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final InterruptedException e) {
            Thread.interrupted();
            throw new RuntimeException(e);
        }
    }

    public static OutputStream write(final String program, final String... args) throws RuntimeException {
        try {
            final Process process = call(program, args);

            Pipe.pipe(process.getInputStream(), System.out);
            Pipe.pipe(process.getErrorStream(), System.err);

            return new BufferedOutputStream(process.getOutputStream());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream read(final String program, final String... args) throws RuntimeException {
        try {
            final Process process = call(program, args);

//            Pipe.pipe(System.in, process.getOutputStream());
            Pipe.pipe(process.getErrorStream(), System.err);

            return new BufferedInputStream(process.getInputStream());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Process call(final String program, final String... args) throws IOException {
        final List<String> command = new ArrayList<String>();
        command.add(program);
        command.addAll(Arrays.asList(args));

        final ProcessBuilder builder = new ProcessBuilder();
        if (dir != null) builder.directory(new File(dir.getAbsolutePath()));
        builder.command(command);
        builder.environment().put("PATH", "/opt/local/bin:/opt/local/sbin:/sw/bin:/sw/sbin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/X11/bin:/usr/X11R6/bin:/usr/local/bin:/Users/dblevins/bin");
        builder.environment().putAll(env);
        return builder.start();
    }


}
