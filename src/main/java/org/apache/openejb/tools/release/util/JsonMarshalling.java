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

import org.tomitribe.util.IO;
import org.tomitribe.util.PrintString;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static javax.json.bind.JsonbConfig.FORMATTING;

public class JsonMarshalling {
    private JsonMarshalling() {
    }

    public static <JsonbType> JsonbType unmarshal(final Class<JsonbType> type, final File jsonFile) {
        try {
            final Jsonb jsonb = JsonbBuilder.create();
            try (final InputStream stream = IO.read(jsonFile)) {
                return jsonb.fromJson(stream, type);
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to unmarshal %s from file %s", type.getSimpleName(), jsonFile.getAbsolutePath()), e);
        }
    }

    public static <JsonbType> JsonbType unmarshal(final Class<JsonbType> type, final String json) {
        try {
            final Jsonb jsonb = JsonbBuilder.create();
            try (final InputStream stream = IO.read(json)) {
                return jsonb.fromJson(stream, type);
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to unmarshal %s", type.getSimpleName()), e);
        }
    }

    public static <JsonObject> File toFormattedJson(final JsonObject jsonObject, final File file) {
        try {
            final JsonbConfig config = new JsonbConfig();
            config.setProperty(FORMATTING, true);
            final Jsonb jsonb = JsonbBuilder.create(config);
            try (final OutputStream write = IO.write(file)) {
                jsonb.toJson(jsonObject, write);
            }
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public static <JsonObject> String toFormattedJson(final JsonObject jsonObject) {
        final JsonbConfig config = new JsonbConfig();
        config.setProperty(FORMATTING, true);
        final Jsonb jsonb = JsonbBuilder.create(config);
        final PrintString out = new PrintString();
        jsonb.toJson(jsonObject, out);
        return out.toString();
    }
}
