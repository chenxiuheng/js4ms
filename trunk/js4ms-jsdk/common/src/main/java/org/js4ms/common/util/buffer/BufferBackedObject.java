package org.js4ms.common.util.buffer;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * BufferBackedObject.java [org.js4ms.jsdk:common]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.LoggableBase;



/**
 * @author Gregory Bumgardner
 */
public abstract class BufferBackedObject
                extends LoggableBase {

    final ByteBuffer buffer;

    /**
     * Creates a slice of the remaining bytes within the buffer, sets the
     * limit in the slice to the value specified for the size argument,
     * and advances the input buffer position by the same amount.
     */
    public static ByteBuffer consume(final ByteBuffer buffer, final int size) {
        ByteBuffer slice = buffer.slice();
        buffer.position(buffer.position() + size);
        slice.limit(size);
        return slice;
    }

    /**
     * Creates a buffer containing bytes read from an InputStream.
     * 
     * @throws IOException
     */
    public static ByteBuffer consume(final InputStream is, final int size) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        is.read(buffer.array(), 0, size);
        return buffer;
    }

    /**
     * @param size
     */
    public BufferBackedObject(final int size) {
        this.buffer = ByteBuffer.allocate(size);
    }

    /**
     * @param buffer
     */
    public BufferBackedObject(final ByteBuffer buffer) {
        this.buffer = buffer.slice();
    }

    /**
     * @param buffer
     * @param size
     */
    public BufferBackedObject(final ByteBuffer buffer, final int size) {
        this.buffer = consume(buffer, size);
    }

    /**
     * @param buffer
     * @param size
     * @throws IOException
     */
    public BufferBackedObject(final InputStream is, final int size) throws IOException {
        this.buffer = consume(is, size);
    }

    /**
     * Returns a duplicate of the ByteBuffer instance referenced by this object.
     * 
     * @return
     */
    public final ByteBuffer getBuffer() {
        return this.buffer.duplicate();
    }

    /**
     * Returns the actual ByteBuffer instance referenced by this object.
     * 
     * @return
     */
    protected final ByteBuffer getBufferInternal() {
        return this.buffer;
    }

    /**
     * @param buffer
     */
    public void writeTo(final ByteBuffer buffer) {
        this.buffer.rewind();
        buffer.put(this.buffer);
        this.buffer.rewind();
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * Logs member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": buffer array-offset=" + this.buffer.arrayOffset() +
                                      ", position=" + this.buffer.position() +
                                      ", remaining=" + this.buffer.remaining() +
                                      ", limit=" + this.buffer.limit() +
                                      ", capacity=" + this.buffer.capacity()));
    }
}
