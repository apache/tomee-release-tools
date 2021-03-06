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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @version $Rev$ $Date$
 */
public class Files {

    public static File file(final String... parts) {
        File dir = null;
        for (final String part : parts) {
            if (dir == null) {
                dir = new File(part);
            } else {
                dir = new File(dir, part);
            }
        }

        return dir;
    }

    public static File file(File dir, final String... parts) {
        for (final String part : parts) {
            dir = new File(dir, part);
        }

        return dir;
    }

    public static List<File> collect(final File dir, final String regex) {
        return collect(dir, Pattern.compile(regex));
    }

    public static List<File> collect(final File dir, final Pattern pattern) {
        return collect(dir, new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return pattern.matcher(file.getAbsolutePath()).matches();
            }
        });
    }


    public static List<File> collect(final File dir, final FileFilter filter) {
        final List<File> accepted = new ArrayList<File>();
        if (filter.accept(dir)) accepted.add(dir);

        final File[] files = dir.listFiles();
        if (files != null) for (final File file : files) {
            accepted.addAll(collect(file, filter));
        }

        return accepted;
    }

    public static void remove(final File file) {
        if (file == null) return;
        if (!file.exists()) return;

        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files != null) {
                for (final File child : files) {
                    remove(child);
                }
            }
        }
        if (!file.delete()) {
            throw new IllegalStateException("Could not delete file: " + file.getAbsolutePath());
        }
    }

    public static void exists(final File file, final String s) {
        if (!file.exists()) throw new RuntimeException(s + " does not exist: " + file.getAbsolutePath());
    }

    public static void dir(final File file) {
        if (!file.isDirectory()) throw new RuntimeException("Not a directory: " + file.getAbsolutePath());
    }

    public static void file(final File file) {
        if (!file.isFile()) throw new RuntimeException("Not a file: " + file.getAbsolutePath());
    }

    public static void writable(final File file) {
        if (!file.canWrite()) throw new RuntimeException("Not writable: " + file.getAbsolutePath());
    }

    public static void readable(final File file) {
        if (!file.canRead()) throw new RuntimeException("Not readable: " + file.getAbsolutePath());
    }

    public static void mkdir(final File file) {
        if (file.exists()) return;
        if (!file.mkdirs()) throw new RuntimeException("Cannot mkdir: " + file.getAbsolutePath());
    }

    public static File tmpdir() {
        try {
            final File file = File.createTempFile("temp", "dir");
            if (!file.delete()) throw new IllegalStateException("Cannot make temp dir.  Delete failed");
            mkdir(file);
            return file;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void mkparent(final File file) {
        mkdirs(file.getParentFile());
    }

    public static void mkdirs(final File file) {

        if (!file.exists()) {

            assert file.mkdirs() : "mkdirs " + file;

            return;
        }

        assert file.isDirectory() : "not a directory" + file;
    }
}
