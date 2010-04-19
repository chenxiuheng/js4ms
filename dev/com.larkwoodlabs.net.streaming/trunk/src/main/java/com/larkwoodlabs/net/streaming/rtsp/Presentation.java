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

package com.larkwoodlabs.net.streaming.rtsp;

import java.util.logging.Level;
import java.util.logging.Logger;


import com.larkwoodlabs.util.logging.Logging;

/**
 * Represents an active media presentation consisting of one or more media streams (audio, video, data).
 *
 * @author Gregory Bumgardner
 */
abstract class Presentation {

    /*-- Static Constants ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(Presentation.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    private PresentationDescription presentationDescription;

    protected final String ObjectId = Logging.identify(this);



    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a presentation that is described by a {@link PresentationDescription}.
     * @param presentationDescription - The presentation description.
     */
    protected Presentation(PresentationDescription presentationDescription) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Presentation.Presentation", presentationDescription));
        }

        this.presentationDescription = presentationDescription;
    }

    /**
     * Gets the {@link PresentationDescription} that describes this presentation.
     */
    public final PresentationDescription getDescription() {
        return this.presentationDescription;
    }
    
}
