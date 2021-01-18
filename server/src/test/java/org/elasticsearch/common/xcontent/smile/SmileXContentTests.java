/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.xcontent.smile;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.BaseXContentTestCase;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class SmileXContentTests extends BaseXContentTestCase {

    @Override
    public XContentType xcontentType() {
        return XContentType.SMILE;
    }

    public void testBigInteger() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonGenerator generator = new SmileFactory().createGenerator(os);
        doTestBigInteger(generator, os);
    }

    public void testIncompatibleStreamTypes() throws Exception {
        final Map<String, Object> map = Map.of("foo", "bar");
        final BytesReference jsonBytes = BytesReference.bytes(JsonXContent.contentBuilder().map(map));
        expectThrows(IllegalArgumentException.class, () -> {
            SmileXContent.smileXContent.createParser(
                NamedXContentRegistry.EMPTY, DeprecationHandler.IGNORE_DEPRECATIONS, jsonBytes.streamInput()).map();
        });
        final BytesRef bytes = jsonBytes.toBytesRef();
        expectThrows(JsonParseException.class, () -> {
            SmileXContent.smileXContent.createParser(
                NamedXContentRegistry.EMPTY, DeprecationHandler.IGNORE_DEPRECATIONS, bytes.bytes, bytes.offset, bytes.length).map();
        });
    }
}
