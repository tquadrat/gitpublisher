/*
 * ============================================================================
 *  Copyright © 2002-2022 by Thomas Thrien.
 *  All Rights Reserved.
 * ============================================================================
 *  Licensed to the public under the agreements of the GNU Lesser General Public
 *  License, version 3.0 (the "License"). You may obtain a copy of the License at
 *
 *       http://www.gnu.org/licenses/lgpl.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */

package org.tquadrat.foundation.gradle.gitpublisher;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.tquadrat.foundation.gradle.gitpublisher.extension.GITPublisherExtension;
import org.tquadrat.foundation.gradle.gitpublisher.task.PublishToGIT;

/**
 *  Provides the capabilities for the plugin
 *  {@link GITPublisherPlugin}
 *  that publishes a project to GitHub.
 *
 *  @version $Id: GITPublisherBasePlugin.java 964 2022-01-03 12:06:13Z tquadrat $
 *  @author Thomas Thrien - thomas.thrien@tquadrat.org
 */
public final class GITPublisherBasePlugin implements Plugin<Project>
{
        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  The name of the &quot;publish&quot; task: {@value}.
     */
    public static final String PUBLISH_TASK = PublishToGIT.TASK_NAME;

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  {@inheritDoc}
     */
    public final void apply( final Project project )
    {
        //---* Create the extension *------------------------------------------
        var extension = project.getExtensions().create( PUBLISH_TASK, GITPublisherExtension.class );

        //---* Define the tasks *----------------------------------------------
        project.getExtensions().findByName( PUBLISH_TASK );
        project.getTasks().register( PUBLISH_TASK, PublishToGIT.class, task -> {
            task.getCommitMessage().set( extension.getCommitMessage() );
            task.getCredentials().set( extension.getCredentials() );
            task.getDebugFlag().set( extension.getDebugFlag() );
            task.getDryRunFlag().set( extension.getDryRunFlag() );
            task.getIgnoresList().set( extension.getIgnoresList() );
            task.getJavadocLocation().set( extension.getJavadocLocation() );
            task.getLocalRepositoryFolder().set( extension.getLocalRepositoryFolder() );
            task.getMetaDir().set( extension.getMetaDir() );
            task.getMustCleanupFlag().set( extension.getMustCleanupFlag() );
            task.getPassword().set( extension.getUsername() );
            task.getRemoteRepositoryURI().set( extension.getRemoteRepositoryURI() );
            task.getSourcesList().set( extension.getSourcesList() );
            task.getUsername().set( extension.getPassword() );
            task.getWorkFolder().set( extension.getWorkFolder() );
        } );

    }   //  apply()
}
//  class GITPublisherBasePlugin

/*
 *  End of File
 */