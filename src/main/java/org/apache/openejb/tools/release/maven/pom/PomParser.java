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

import org.apache.openejb.tools.release.util.JsonMarshalling;
import org.tomitribe.swizzle.stream.StreamBuilder;
import org.tomitribe.util.IO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class PomParser {

    public static Project parse(final URL url) {
        try {
            final String xml = IO.slurp(url);
            return parse(xml);
        } catch (IOException e) {
            throw new MavenPomException(e);
        }
    }

    public static Project parse(final File file) {
        try {
            final String xml = IO.slurp(file);
            return parse(xml);
        } catch (IOException e) {
            throw new MavenPomException(e);
        }
    }

    public static Project parse(final String xml) {
        try {
            final JAXBContext context = JAXBContext.newInstance(Project.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();

            final Project project = (Project) unmarshaller.unmarshal(IO.read(trimPomXml(xml)));

            return interpolate(project);
        } catch (Exception e) {
            throw new MavenPomException(e);
        }
    }

    /**
     * JAXB is unforgiving if you do not model the xml schema in 100% entirety.
     *
     * We only want a few key elements and do not need all the rest, so we use this method
     * to strip out all the stuff we do not need, leaving only what we can unmarshal.
     */
    private static String trimPomXml(final String rawXml) throws IOException, ParserConfigurationException, SAXException, TransformerException {

        /*
         * Read the xml into a dom and strip out the parts we do not need
         */
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();

        final Document document = builder.parse(IO.read(rawXml.trim()));
        final Element documentElement = document.getDocumentElement();

        /*
         * Strip off the namespace.  Some people use it, some do not.  JAXB is incapable of
         * being flexible on this so we normalize everything to not have the namespace.
         */
        final NamedNodeMap attributes = documentElement.getAttributes();
        while (attributes.getLength() > 0) {
            final Node item = attributes.item(0);
            attributes.removeNamedItem(item.getNodeName());
        }

        /*
         * These are the only elements we care about
         */
        final Predicate<Node> wanted = element("parent")
                .or(element("groupId"))
                .or(element("artifactId"))
                .or(element("version"))
                .or(element("properties"))
                .or(element("dependencies"));

        /*
         * Remove anything but the wanted elements
         */
        nodes(documentElement).stream()
                .filter(wanted.negate())
                .forEach(documentElement::removeChild);

        /*
         * Long-winded boilerplate code to write the DOM back out as xml
         */
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Transformer trans = tf.newTransformer();
        final StringWriter sw = new StringWriter();
        trans.transform(new DOMSource(document), new StreamResult(sw));
        return sw.toString();
    }

    private static Predicate<Node> element(final String name) {
        return node -> node.getNodeName().equals(name);
    }

    private static List<Node> nodes(final Element documentElement) {
        final List<Node> nodes = new ArrayList<Node>();
        final NodeList childNodes = documentElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node item = childNodes.item(i);
            nodes.add(item);
        }
        return nodes;
    }

    private static Project interpolate(final Project project) throws IOException {
        if (project.getProperties() == null) return project;
        final String actualJson = JsonMarshalling.toFormattedJson(project);
        final Map<String, String> properties = project.getProperties();

        final String interpolated = interpolate(actualJson, properties);
        return JsonMarshalling.unmarshal(Project.class, interpolated);
    }

    public static String interpolate(final String content, final Map<String, String> properties) throws IOException {
        String previous, current = content;

        do {
            previous = current;
            final InputStream inputStream = StreamBuilder.create(IO.read(current))
                    .replace("${", "}", s -> {
                        final String value = properties.get(s);
                        if (value == null) return "${" + s + "}";
                        return value;
                    }).get();
            current = IO.slurp(inputStream);
        } while (!current.equals(previous));

        return current;
    }
}
