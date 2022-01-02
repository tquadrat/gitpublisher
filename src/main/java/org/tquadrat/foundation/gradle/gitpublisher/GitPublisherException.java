/*
 * ============================================================================
 * Copyright © 2002-2022 by Thomas Thrien.
 * All Rights Reserved.
 * ============================================================================
 * Licensed to the public under the agreements of the GNU Lesser General Public
 * License, version 3.0 (the "License"). You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/lgpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.tquadrat.foundation.gradle.gitpublisher;

import org.eclipse.jgit.api.errors.GitAPIException;

/**
 *  An implementation of
 *  {@link org.eclipse.jgit.api.errors.GitAPIException}
 *  for this plugin.
 *
 *  @version $Id: GitPublisherException.java 959 2022-01-02 23:09:45Z tquadrat $
 *  @author Thomas Thrien - thomas.thrien@tquadrat.org
 *  @since 0.1.0
 */
public final class GitPublisherException extends GitAPIException
{
        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Constructs a new instance of {@code GitPublisherException} with the
     *  specified detail message and <i>no</i> cause.
     *
     *  @param  message The detail message.
     */
    public GitPublisherException( final String message ) { super( message ); }

    /**
     *  Constructs a new instance of {@code GitPublisherException} with the
     *  specified detail message and cause.
     *
     *  @param  message The detail message.
     *  @param  cause   The cause.
     */
    @SuppressWarnings( "unused" )
    public GitPublisherException( final String message, final Throwable cause ) { super( message, cause ); }
}
//  class GitPublisherException

/*
 *  End of File
 */