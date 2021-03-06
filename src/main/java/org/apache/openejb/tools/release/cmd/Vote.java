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

import org.apache.creadur.tentacles.Platform;
import org.apache.creadur.tentacles.TemplateBuilder;
import org.apache.creadur.tentacles.Templates;
import org.apache.openejb.tools.release.Command;
import org.apache.openejb.tools.release.Release;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @version $Rev$ $Date$
 */
@Command(dependsOn = {Legal.class, Binaries.class})
public class Vote {

    public static void main(final String[] args) throws IOException {

        final TemplateBuilder template = new Templates(Platform.aPlatform()).template("vote.vm");

        for (final Field field : Release.class.getFields()) {
            try {
                template.add(field.getName(), field.get(null));
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        final Date end = new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(72));
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E hh:mma z");
        template.add("endDateUS", simpleDateFormat.format(end));

        final String email = template.apply();

        System.out.println(email);

//        final OutputStream out = Exec.write("ssh", "people.apache.org", "/usr/sbin/sendmail -it");
//        out.write(email.getBytes());
//        out.flush();
//        out.close();
    }
}
