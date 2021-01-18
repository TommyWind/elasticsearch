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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.smile.SmileConstants;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentGenerator;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Set;

/**
 * A Smile based content implementation using Jackson.
 */
public class SmileXContent implements XContent {

    public static XContentBuilder contentBuilder() throws IOException {
        return XContentBuilder.builder(smileXContent);
    }

    static final SmileFactory smileFactory;
    public static final SmileXContent smileXContent;

    static {
        smileFactory = new SmileFactory();
        // for now, this is an overhead, might make sense for web sockets
        smileFactory.configure(SmileGenerator.Feature.ENCODE_BINARY_AS_7BIT, false);
        smileFactory.configure(SmileFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW, false); // this trips on many mappings now...
        // Do not automatically close unclosed objects/arrays in com.fasterxml.jackson.dataformat.smile.SmileGenerator#close() method
        smileFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false);
        smileFactory.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
        smileXContent = new SmileXContent();
    }

    private SmileXContent() {
    }

    @Override
    public XContentType type() {
        return XContentType.SMILE;
    }

    @Override
    public byte streamSeparator() {
        return (byte) 0xFF;
    }

    @Override
    public XContentGenerator createGenerator(OutputStream os, Set<String> includes, Set<String> excludes) throws IOException {
        return new SmileXContentGenerator(smileFactory.createGenerator(os, JsonEncoding.UTF8), os, includes, excludes);
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry,
            DeprecationHandler deprecationHandler, String content) throws IOException {
        return new SmileXContentParser(xContentRegistry, deprecationHandler, smileFactory.createParser(content));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry,
            DeprecationHandler deprecationHandler, InputStream is) throws IOException {
        checkSmileHeader(is);
        return new SmileXContentParser(xContentRegistry, deprecationHandler, smileFactory.createParser(is));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry,
            DeprecationHandler deprecationHandler, byte[] data) throws IOException {
        return new SmileXContentParser(xContentRegistry, deprecationHandler, smileFactory.createParser(data));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry,
            DeprecationHandler deprecationHandler, byte[] data, int offset, int length) throws IOException {
        return new SmileXContentParser(xContentRegistry, deprecationHandler, smileFactory.createParser(data, offset, length));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry,
            DeprecationHandler deprecationHandler, Reader reader) throws IOException {
        return new SmileXContentParser(xContentRegistry, deprecationHandler, smileFactory.createParser(reader));
    }

    private void checkSmileHeader(InputStream is) throws IOException {
        if (is.markSupported() == false) {
            throw new IllegalArgumentException("Cannot check smile header without mark/reset support on " + is.getClass());
        }
        is.mark(Integer.MAX_VALUE);
        try {
            // scan until we find the first non-whitespace character or the end of the stream
            int current;
            do {
                current = is.read();
                if (current == -1) {
                    throw new IllegalArgumentException("Input does not start with Smile format header -- can not parse");
                }
            } while (Character.isWhitespace((char) current));
            byte firstByte = (byte) current;
            if(firstByte != SmileConstants.HEADER_BYTE_1){
                throw new IllegalArgumentException("Input does not start with Smile format header (first byte = 0x"
                    +Integer.toHexString(firstByte & 0xFF)+") -- rather, it starts with '"+((char) firstByte)
                    +"' (plain JSON input?) -- can not parse");
            }
            byte secondByte = (byte) is.read();
            if(secondByte != SmileConstants.HEADER_BYTE_2){
                throw new IllegalArgumentException("Input does not start with Smile format header (first byte = 0x"
                    +Integer.toHexString(secondByte & 0xFF)+") -- rather, it starts with '"+((char) secondByte)
                    +"' (plain JSON input?) -- can not parse"); //todo: fix message?
            }
            byte thirdByte = (byte) is.read();
            if(thirdByte != SmileConstants.HEADER_BYTE_3){
                throw new IllegalArgumentException("Input does not start with Smile format header (first byte = 0x"
                    +Integer.toHexString(thirdByte & 0xFF)+") -- rather, it starts with '"+((char) thirdByte)
                    +"' (plain JSON input?) -- can not parse");//todo: fix message?
            }
        } finally {
            is.reset();
        }
    }
}
