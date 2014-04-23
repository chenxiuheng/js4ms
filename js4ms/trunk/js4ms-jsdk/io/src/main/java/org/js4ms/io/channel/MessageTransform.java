package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageTransform.java [org.js4ms.jsdk:io]
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

/**
 * Interface exposed by objects that transform or modify messages.
 * 
 * @param <InputMessageType>
 *            The input or upstream message type.
 * @param <OutputMessageType>
 *            The output or downstream message type.
 * @author Greg Bumgardner (gbumgard)
 */
public interface MessageTransform<InputMessageType, OutputMessageType> {

    /**
     * Transforms or modifies the input message to produce an output message.
     * 
     * @param message
     *            The message to transform or modify.
     * @return The new or modified message.
     * @throws Exception
     *             The transformation failed.
     */
    OutputMessageType transform(InputMessageType message) throws IOException;

}
