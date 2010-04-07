/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.util.logging;

import java.util.logging.Logger;

/**
 * Abstract base class for classes that support logging.
 *
 * @author Gregory Bumgardner
 */
public abstract class LoggableBase implements Loggable {

    protected static final String STATIC = "[ static ]";
    
    public final String ClassId = this.getClass().getName();
    public final String ObjectId = Logging.identify(this);

    /**
     * Returns the Logger instance used by this object to generate log messages.
     */
    public abstract Logger getLogger();
    
    /**
     * Logs the internal state of this object using the logger returned by {@link #getLogger()}.
     */
    public final void log() {
        log(getLogger());
    }

    @Override
    public void log(final Logger logger) {
        logger.info(ObjectId + " + logging [" + ClassId + "]");
    }

}
