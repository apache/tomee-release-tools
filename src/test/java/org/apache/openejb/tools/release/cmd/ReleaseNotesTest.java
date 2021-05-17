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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReleaseNotesTest {

    @Test
    public void normalizeDependencySummary() {
        assertNormalize("BatchEE 0.6", "Upgrade BatchEE to 0.6");
        assertNormalize("CXF 3.3.10 / 3.4.3", "Upgrade CXF to 3.3.10 / 3.4.3 in TomEE");
        assertNormalize("CXF 3.3.8", "Update CXF 3.3.8");
        assertNormalize("CXF 3.4.x (Java 16 support)", "Upgrade CXF 3.4.x (Java 16 support)");
        assertNormalize("EclipseLink 2.7.7", "Update EclipseLink to 2.7.7");
        assertNormalize("Implement JAX-RS SSE and add example", "Implement JAX-RS SSE and add example");
        assertNormalize("Johnzon 1.2.9", "Apache Johnzon 1.2.9");
        assertNormalize("Johnzon 1.2.9", "Update Johnzon 1.2.9");
        assertNormalize("MyFaces 2.3.8", "Upgrade MyFaces 2.3.8");
        assertNormalize("MyFaces 2.3.9", "Upgrade MyFaces to 2.3.9");
        assertNormalize("OWB 2.0.22", "Update OWB 2.0.22");
        assertNormalize("OpenSAML V3.4.6", "Update OpenSAML to V3.4.6");
        assertNormalize("Tomcat 9.0.41", "Upgrade Tomcat 9.0.41");
        assertNormalize("Tomcat 9.0.43", "Update Tomcat to 9.0.43");
        assertNormalize("Tomcat 9.0.44", "Upgrade Tomcat to 9.0.44");
        assertNormalize("Tomcat 9.0.45", "Update Tomcat to 9.0.45");
        assertNormalize("bcprov-jdk15on 1.67", "Update bcprov-jdk15on to 1.67");
        assertNormalize("quartz-openejb-shade", "Upgrade quartz-openejb-shade in TomEE 8/9");
        assertNormalize("xbean 4.18+ (Java 16 support)", "Upgrade xbean to 4.18+ (Java 16 support)");
    }

    public static void assertNormalize(final String expected, final String input) {
        assertEquals(expected, ReleaseNotes.Upgrade.normalize(input));
    }

}
