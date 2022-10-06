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
package org.apache.openejb.tools.release.maven.pom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tomitribe.util.IO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "project")
@XmlAccessorType(XmlAccessType.FIELD)
public class Project implements Coordinates {

    @XmlTransient
    private File file;

    @XmlElement(name = "parent")
    private Parent parent;

    @XmlElement(name = "groupId")
    private String groupId;

    @XmlElement(name = "artifactId")
    private String artifactId;

    @XmlElement(name = "version")
    private String version;

    @XmlElement(name = "properties")
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    private Map<String, String> properties;

    @XmlElementWrapper(name = "dependencies")
    @XmlElement(name = "dependency")
    private List<Dependency> dependencies = new ArrayList<>();


    public static Project parse(final File file) {
        try {
            final Project project = PomParser.parse(IO.slurp(file));
            project.setFile(file);
            return project;
        } catch (IOException e) {
            throw new MavenPomException("Unable to read file " + file.getAbsolutePath(), e);
        }
    }

}
