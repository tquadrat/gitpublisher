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

package org.tquadrat.foundation.gradle.gitpublisher.extension;

import java.io.File;
import java.net.URI;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputDirectory;
import org.tquadrat.foundation.gradle.gitpublisher.GITPublisherPlugin;

/**
 *
 *  The extension for the Git publisher plugin.
 *
 *  @version $Id: GITPublisherExtension.java 959 2022-01-02 23:09:45Z tquadrat $
 *  @author Thomas Thrien - thomas.thrien@tquadrat.org
 *  @since 0.1.0
 */
public abstract class GITPublisherExtension
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  The commit message.
     *
     *  @return The
     *      {@link Property}
     *      for the commit message.
     */
    public abstract Property<String> getCommitMessage();

    /**
     *  The credentials for the access to the remote repository.
     *
     *  @return The
     *      {@link Property}
     *      for the credentials.
     */
    public abstract Property<CredentialsProvider> getCredentials();

    /**
     *  Returns the debug flag.
     *
     *  @return If the
     *      {@link Property}
     *      is {@code true}, a lot of output is generated and written to
     *      {@link System#out}.
     */
    public abstract Property<Boolean> getDebugFlag();

    /**
     *  <p>{@summary The "DryRun" flag.} No cleanup is performed after a
     *  dry-run.</p>
     *
     *  @return If the
     *      {@link Property}
     *      is {@code true}, the
     *      {@link org.eclipse.jgit.api.Git#push()}
     *      will be executed only as a dry-run.
     *
     *  @see org.eclipse.jgit.api.PushCommand#setDryRun(boolean)
     */
    public abstract Property<Boolean> getDryRunFlag();

    /**
     *  The name of the target folder for the temporary local Git repository.
     *
     *  @return The
     *      {@link Property}
     *      for the name of the target folder.
     */
    public abstract Property<String> getLocalRepositoryFolder();

    /**
     *  <p>{@summary The names of the files and folders that will not be
     *  published.}</p>
     *  <p>In fact each of this list is a <i>pattern</i> for a file or folder
     *  name, and these these patterns follow the syntax as for the argument
     *  for
     *  {@link java.nio.file.FileSystem#getPathMatcher(String)},
     *  only that the syntax prefix @quot;{@code glob:}&quot; can be
     *  omitted.</p>
     *  <p>All names are relative to the project's root folder.</p>
     *
     *  @return The
     *      {@link Property}
     *      for the ignores list.
     */
    public abstract ListProperty<String> getIgnoresList();

    /**
     *  <p>{@summary Returns the Javadoc location.} The contents of the given
     *  folder will be copied to the {@code javadoc} folder in the target
     *  repository.</p>
     *
     *  @return The
     *      {@link Property}
     *      for the location of the Javadoc location.
     */
    public abstract Property<File> getJavadocLocation();

    /**
     *  The folder with the files that are published in the root of the Git
     *  repository (for example the {@code README.md}).
     *
     *  @return The
     *      {@link Property}
     *      for the meta folder.
     */
    public abstract Property<File> getMetaDir();

    /**
     *  The password for the access to the Git repository.
     *
     *  @return The
     *      {@link Property}
     *      for the Git repository password.
     */
    public abstract Property<String> getPassword();

    /**
     *  The URI for the remote repository.
     *
     *  @return The
     *      {@link Property}
     *      for the remote repository URI.
     */
    public abstract Property<URI> getRemoteRepositoryURI();

    /**
     *  <p>{@summary The names of the files and folders to publish.}</p>
     *  <p>In fact each entry contains a <i>pattern</i> for a file or folder
     *  name, and these these patterns follow the syntax as for the
     *  argument for
     *  {@link java.nio.file.FileSystem#getPathMatcher(String)},
     *  only that the syntax prefix @quot;{@code glob:}&quot; can be
     *  omitted.</p>
     *  <p>All names are relative to the project's root folder.</p>
     *
     *  @return The
     *      {@link Property}
     *      for the sources list.
     */
    public abstract ListProperty<String> getSourcesList();

    /**
     *  The username for the access to the Git repository.
     *
     *  @return The
     *      {@link Property}
     *      for the Git repository username.
     */
    public abstract Property<String> getUsername();

    /**
     *  <p>{@summary The work folder that should contain the temporary local
     *  Git repository.}</p>
     *  <p>The default name is
     *  {@value GITPublisherPlugin#BASE_FOLDER_DEFAULT},
     *  the folder is relative to the
     *  {@linkplain Project#getRootDir() project root}.</p>
     *  <p>This folder will be created if it does not exist, but it will not
     *  be deleted on cleanup.</p>
     *
     *  @return The
     *      {@link Property}
     *      for the work folder.
     */
    public abstract Property<File> getWorkFolder();

    /**
     *  The flag that indicates whether the temporary repository has to be
     *  removed in the end.
     *
     * @return The
     *      {@link Property}
     *      for the flag.
     */
    public abstract Property<Boolean> getMustCleanupFlag();
}
//  class GITPublisherExtension

/*
 *  End of File
 */