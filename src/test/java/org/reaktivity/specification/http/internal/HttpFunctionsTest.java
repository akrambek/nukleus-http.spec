/**
 * Copyright 2016-2019 The Reaktivity Project
 *
 * The Reaktivity Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.reaktivity.specification.http.internal;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.reaktivity.specification.http.internal.HttpFunctions.copyOfRange;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomAscii;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomBytes;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomBytesInvalidUTF8;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomBytesUTF8;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomBytesUnalignedUTF8;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomCaseNot;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomHeaderNot;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomInvalidVersion;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomMethodNot;
import static org.reaktivity.specification.http.internal.HttpFunctions.randomizeLetterCase;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.MalformedInputException;

import javax.el.ELContext;
import javax.el.FunctionMapper;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;
import org.kaazing.k3po.lang.internal.el.ExpressionContext;
import org.reaktivity.specification.http.internal.types.control.HttpRouteExFW;
import org.reaktivity.specification.http.internal.types.stream.HttpBeginExFW;
import org.reaktivity.specification.http.internal.types.stream.HttpChallengeExFW;
import org.reaktivity.specification.http.internal.types.stream.HttpDataExFW;
import org.reaktivity.specification.http.internal.types.stream.HttpEndExFW;

public class HttpFunctionsTest
{
    @Test
    public void shouldResolveFunction() throws Exception
    {
        final ELContext ctx = new ExpressionContext();
        final FunctionMapper mapper = ctx.getFunctionMapper();
        final Method function = mapper.resolveFunction("http", "randomInvalidVersion");

        assertNotNull(function);
        assertSame(HttpFunctions.class, function.getDeclaringClass());
    }

    @Test
    public void shouldRandomizeInvalidVersion() throws Exception
    {
        final String version = randomInvalidVersion();

        assertNotEquals("HTTP/1.1", version);
    }

    @Test
    public void shouldRandomizeMethodNotGet() throws Exception
    {
        final String method = randomMethodNot("GET");

        assertNotEquals("GET", method);
    }

    @Test
    public void shouldRandomizeHeaderNotAuthorization() throws Exception
    {
        final String header = randomHeaderNot("authorization");

        assertNotEquals("authorization", header);
    }

    @Test
    public void shouldRandomizeCase() throws Exception
    {
        final String randomizedCase = randomizeLetterCase("aBcdEfGHiJ");

        assertEquals("abcdefghij", randomizedCase.toLowerCase());
    }

    @Test
    public void shouldRandomizeCaseNotIdentical() throws Exception
    {
        final String randomizedCase = randomCaseNot("aBcdEfGHiJ");

        assertNotEquals("aBcdEfGHiJ", randomizedCase);
        assertEquals("abcdefghij", randomizedCase.toLowerCase());
    }

    @Test
    public void shouldRandomizeBytes() throws Exception
    {
        final byte[] bytes = randomBytes(42);

        assertNotNull(bytes);
        assertEquals(42, bytes.length);
    }

    @Test
    public void shouldCopyRangeOfBytes() throws Exception
    {
        final byte[] bytes = new byte[42];
        for (int i = 0; i < bytes.length; i++)
        {
            bytes[i] = (byte) i;
        }

        final byte[] range = copyOfRange(bytes, 5, 10);

        assertNotNull(range);
        assertEquals(5, range.length);

        for (int i = 0; i < range.length; i++)
        {
            assertEquals(i + 5, range[i]);
        }
    }

    @Test
    public void shouldRandomizeBytesAscii() throws Exception
    {
        final byte[] ascii = randomAscii(42);

        assertNotNull(ascii);
        assertEquals(42, ascii.length);

        US_ASCII.newDecoder().decode(ByteBuffer.wrap(ascii));
    }

    @Test
    public void shouldRandomizeBytesUTF8() throws Exception
    {
        final byte[] bytes = randomBytesUTF8(42);

        assertNotNull(bytes);
        assertEquals(42, bytes.length);

        UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes));
    }

    @Test
    public void shouldRandomizeBytesUnalignedUTF8() throws Exception
    {
        final byte[] bytes = randomBytesUnalignedUTF8(42, 20);

        assertNotNull(bytes);
        assertEquals(42, bytes.length);

        UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes));
    }

    @Test(expected = AssertionError.class)
    public void shouldRandomizeBytesUnalignedUTF8OutOfBounds() throws Exception
    {
        randomBytesUnalignedUTF8(42, 43);
    }

    @Test(expected = AssertionError.class)
    public void shouldRandomizeBytesUnalignedUTF8Negative() throws Exception
    {
        randomBytesUnalignedUTF8(42, -1);
    }

    @Test(expected = MalformedInputException.class)
    public void shouldRandomizeBytesInvalidUTF8() throws Exception
    {
        final byte[] bytes = randomBytesInvalidUTF8(42);

        assertNotNull(bytes);
        assertEquals(42, bytes.length);

        UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes));
    }

    @Test
    public void shouldGenerateRouteExtension()
    {
        byte[] build = HttpFunctions.routeEx()
                                    .header("name", "value")
                                    .override("name", "override")
                                    .build();
        DirectBuffer buffer = new UnsafeBuffer(build);
        HttpRouteExFW routeEx = new HttpRouteExFW().wrap(buffer, 0, buffer.capacity());
        routeEx.headers().forEach(onlyHeader ->
        {
            assertEquals("name", onlyHeader.name().asString());
            assertEquals("value", onlyHeader.value().asString());
        });
        routeEx.overrides().forEach(onlyOverride ->
        {
            assertEquals("name", onlyOverride.name().asString());
            assertEquals("override", onlyOverride.value().asString());
        });
        assertTrue(!routeEx.headers().isEmpty());
        assertTrue(!routeEx.overrides().isEmpty());
    }

    @Test
    public void shouldGenerateBeginExtension()
    {
        byte[] build = HttpFunctions.beginEx()
                                    .typeId(0x01)
                                    .header("name", "value")
                                    .build();
        DirectBuffer buffer = new UnsafeBuffer(build);
        HttpBeginExFW beginEx = new HttpBeginExFW().wrap(buffer, 0, buffer.capacity());
        assertEquals(0x01, beginEx.typeId());
        beginEx.headers().forEach(onlyHeader ->
        {
            assertEquals("name", onlyHeader.name().asString());
            assertEquals("value", onlyHeader.value().asString());
        });
        assertTrue(beginEx.headers().sizeof() > 0);
    }

    @Test
    public void shouldGenerateDataExtension()
    {
        byte[] build = HttpFunctions.dataEx()
                                    .typeId(0x01)
                                    .promise("name", "value")
                                    .build();
        DirectBuffer buffer = new UnsafeBuffer(build);
        HttpDataExFW dataEx = new HttpDataExFW().wrap(buffer, 0, buffer.capacity());
        assertEquals(0x01, dataEx.typeId());
        dataEx.promise().forEach(onlyHeader ->
        {
            assertEquals("name", onlyHeader.name().asString());
            assertEquals("value", onlyHeader.value().asString());
        });
        assertTrue(dataEx.promise().sizeof() > 0);
    }

    @Test
    public void shouldGenerateEndExtension()
    {
        byte[] build = HttpFunctions.endEx()
                                    .typeId(0x01)
                                    .trailer("name", "value")
                                    .build();
        DirectBuffer buffer = new UnsafeBuffer(build);
        HttpEndExFW endEx = new HttpEndExFW().wrap(buffer, 0, buffer.capacity());
        assertEquals(0x01, endEx.typeId());
        endEx.trailers().forEach(onlyHeader ->
        {
            assertEquals("name", onlyHeader.name().asString());
            assertEquals("value", onlyHeader.value().asString());
        });
        assertTrue(endEx.trailers().sizeof() > 0);
    }

    @Test
    public void shouldGenerateChallengeExtension()
    {
        byte[] build = HttpFunctions.challengeEx()
                                    .typeId(0x01)
                                    .header("name", "value")
                                    .build();
        DirectBuffer buffer = new UnsafeBuffer(build);
        HttpChallengeExFW challengeEx = new HttpChallengeExFW().wrap(buffer, 0, buffer.capacity());
        assertEquals(0x01, challengeEx.typeId());
        challengeEx.headers().forEach(onlyHeader ->
        {
            assertEquals("name", onlyHeader.name().asString());
            assertEquals("value", onlyHeader.value().asString());
        });
        assertTrue(challengeEx.headers().sizeof() > 0);
    }
}
