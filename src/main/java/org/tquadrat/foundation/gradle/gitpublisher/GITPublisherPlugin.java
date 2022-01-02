/*
 * ============================================================================
 *  Copyright © 2002-2021 by Thomas Thrien.
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

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.specs.Spec;

/**
 *  Publishes a project to GitHub.
 *
 *  @version $Id: GITPublisherPlugin.java 956 2022-01-02 19:29:01Z tquadrat $
 *  @author Thomas Thrien - thomas.thrien@tquadrat.org
 */
@SuppressWarnings( "unused" )
public final class GITPublisherPlugin implements Plugin<Project>
{
        /*---------------*\
    ====** Inner Classes **====================================================
        \*---------------*/
    /**
     *  An implementation of
     *  {@link Spec}
     *  that evaluates to {@code false} always.
     *
     *  @author Thomas Thrien - thomas.thrien@tquadrat.org
     *  @author Etienne Studer - etienne@studer.nu
     */
    public static final class AlwaysFalseSpec implements Spec<Task>
    {
            /*--------------*\
        ====** Constructors **=================================================
            \*--------------*/
        /**
         *  Creates a new instance of {@code AlwaysFalseSpec},
         */
        public AlwaysFalseSpec() { /* Just exists */ }

            /*---------*\
        ====** Methods **======================================================
            \*---------*/

        /**
         * {@inheritDoc}
         */
        @Override
        public final boolean isSatisfiedBy( Task element ) { return false; }
    }
    //  class AlwaysFalseSpec

        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  <p>{@summary The name for the default base folder.} It is relative to
     *  the project folder: {@value}.</p>
     *  <p>This folder will be created if it does not exist, but it will not
     *  be deleted on cleanup.</p>
     */
    @SuppressWarnings( "SpellCheckingInspection" )
    public static final String BASE_FOLDER_DEFAULT = "gitpublishwork";

    /**
     *  The group for the tasks of this plugin.
     */
    public static final String GROUP = "GITPublisher";

    /**
     *  The default name for the folder mit the Git metadata files: {@value}.
     *  The folder is relative to the
     *  {@linkplain Project#getProjectDir() project folder}.
     */
    public static final String META_DIR_NAME = "gitMeta";

    /**
     *  The prefix for the default name of the folder for the working copy of
     *  the Git repository.
     */
    public static final String TARGET_NAME = "tempGit";

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/

        /*------------------------*\
    ====** Static Initialisations **===========================================
        \*------------------------*/
    /**
     *  An instance of
     *  {@link Spec}
     *  that always evaluates to {@code false}.
     */
    public static final Spec<Task> ALWAYS_FALSE_SPEC = new AlwaysFalseSpec();

    /**
     *  <p>{@summary The list of name patterns for files and folders that are
     *  always excluded from being published.}</p>
     *  <p>For the syntax of these patterns, refer to
     *  {@link java.nio.file.FileSystem#getPathMatcher};
     *  if the <code><i>syntax</i>:</code> prefix is missing, it will be set to
     *  &quot;{@code glob:}&quot;.</p>
     */
    private static final Collection<String> ALWAYS_IGNORED_FILES_AND_FOLDER;

    static
    {
        ALWAYS_IGNORED_FILES_AND_FOLDER = List.of( ".git/**" );
    }

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  <p>{@summary Returns a list of name patterns for files and folders that
     *  are always excluded from being published.}</p>
     *  <p>For the syntax of these patterns, refer to
     *  {@link java.nio.file.FileSystem#getPathMatcher};
     *  if the <code><i>syntax</i>:</code> prefix is missing, it will be set to
     *  &quot;{@code glob:}&quot;.</p>
     *
     *  @return The patterns for the always excluded files and folders.
     */
    public static final Collection<PathMatcher> alwaysIgnored()
    {
        final var fileSystem = FileSystems.getDefault();
        final var retValue = ALWAYS_IGNORED_FILES_AND_FOLDER.stream()
            .filter( Objects::nonNull )
            .filter( p -> !p.isBlank() )
            .map( p -> p.startsWith( "glob:" ) || p.startsWith( "regex:" ) ? p : "glob:%s".formatted( p ) )
            .map( fileSystem::getPathMatcher )
            .toList();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  alwaysIgnored()

    /**
     *  {@inheritDoc}
     */
    public final void apply( final Project project )
    {
        //---* Apply the base plugin *-----------------------------------------
        project.getPlugins().apply( GITPublisherBasePlugin.class );
    }   //  apply()
}
//  class GITPublisherPlugin

/*
 *  End of File
 */