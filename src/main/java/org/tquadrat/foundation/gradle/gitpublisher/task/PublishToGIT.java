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

package org.tquadrat.foundation.gradle.gitpublisher.task;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.out;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.tquadrat.foundation.gradle.gitpublisher.GITPublisherPlugin.ALWAYS_FALSE_SPEC;
import static org.tquadrat.foundation.gradle.gitpublisher.GITPublisherPlugin.BASE_FOLDER_DEFAULT;
import static org.tquadrat.foundation.gradle.gitpublisher.GITPublisherPlugin.GROUP;
import static org.tquadrat.foundation.gradle.gitpublisher.GITPublisherPlugin.META_DIR_NAME;
import static org.tquadrat.foundation.gradle.gitpublisher.GITPublisherPlugin.TARGET_NAME;
import static org.tquadrat.foundation.gradle.gitpublisher.GITPublisherPlugin.alwaysIgnored;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.tquadrat.foundation.gradle.gitpublisher.GITPublisherPlugin;
import org.tquadrat.foundation.gradle.gitpublisher.GitPublisherException;

/**
 *  The definition of the task that does the work for the plugin
 *  {@link org.tquadrat.foundation.gradle.gitpublisher.GITPublisherPlugin}.
 *
 *  @version $Id: PublishToGIT.java 959 2022-01-02 23:09:45Z tquadrat $
 *  @author Thomas Thrien - thomas.thrien@tquadrat.org
 */
public abstract class PublishToGIT extends DefaultTask
{
        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  The task name: {@value}.
     */
    public static final String TASK_NAME = "publishToGIT";

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The debug flag.
     */
    private boolean m_IsDebug = false;

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance for {@code PublishToGIT}.
     */
    @Inject
    public PublishToGIT()
    {
        setGroup( GROUP );
        setDescription( "Publishes the files from a project to a GIT repository" );
        getOutputs().upToDateWhen( ALWAYS_FALSE_SPEC );
    }   //  PublishTOGIT()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  <p>{@summary Checks the status of the current working copy.} The method
     *  will return
     *  {@linkplain Optional#empty() an empty result}
     *  in case the status is clean.</p>
     *  <p>The status is dirty if at one of the methods</p>
     *  <ul>
     *      <li>{@link Status#getAdded()}</li>
     *      <li>{@link Status#getChanged()}</li>
     *      <li>{@link Status#getConflicting()}</li>
     *      <li>{@link Status#getIgnoredNotInIndex()}</li>
     *      <li>{@link Status#getMissing()}</li>
     *      <li>{@link Status#getModified()}</li>
     *      <li>{@link Status#getRemoved()}</li>
     *      <li>{@link Status#getUncommittedChanges()}</li>
     *      <li>{@link Status#getUntracked()}</li>
     *      <li>{@link Status#getUntrackedFolders()}</li>
     *  </ul>
     *  <p>returns a non-empty collection.</p>
     *
     *  @param  git The local repository.
     *  @return An instance of
     *      {@link Optional}
     *      that holds the dirty status.
     *  @throws GitAPIException Something went wrong when accessing the status.
     */
    private final Optional<Status> checkStatus( final Git git ) throws GitAPIException
    {
        //---* Obtain the status *---------------------------------------------
        final var status = git.status()
            .call();

        final Stream<Supplier<Set<String>>> suppliers =
            Stream.of( status::getAdded, status::getChanged, status::getConflicting,
                status::getIgnoredNotInIndex, status::getMissing, status::getModified,
                status::getRemoved, status::getUncommittedChanges, status::getUntracked,
                status::getUntrackedFolders );

        final Optional<Status> retValue = suppliers.allMatch( s -> s.get().isEmpty() ) ? Optional.empty() : Optional.of( status );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  checkStatus()

    /**
     *  <p>{@summary Creates a list of the relevant files in the {@code source}
     *  folder.} The file names are relative to the {@code source} folder.</p>
     *
     *  @param  source  The folder to scan.
     *  @param  includes    The patterns for the names of files to include.
     *  @param  excludes    The patterns for the names of files to ignore.
     *  @return The relevant files.
     *  @throws IOException Cannot collect the files from the given source
     *      folder.
     */
    private final Collection<Path> createFileList( final Path source, final Collection<PathMatcher> includes, final Collection<PathMatcher> excludes ) throws IOException
    {
        final var builder = Stream.<Path>builder();
        createFileList( builder, source, source );

        final var retValue = builder.build()
            .filter( p -> isIncluded( p, includes, excludes ) )
            .toList();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createFileList()

    /**
     *  <p>{@summary Creates a list of the relevant files in the {@code source}
     *  folder.} The file names are relative to the {@code baseFolder}
     *  folder.</p>
     *
     *  @param  builder The stream builder that takes the files.
     *  @param  baseFolder  The base folder.
     *  @param  source  The folder to scan.
     *  @throws IOException Cannot collect the files from the given source
     *      folder.
     */
    private static void createFileList( final Builder<Path> builder, final Path baseFolder, final Path source)  throws IOException
    {

        //---* Get the folder's contents first *-------------------------------
        final var moreSources = Files.list( source )
            .filter( Files::isDirectory )
            .toList();
        for( final var folder : moreSources ) createFileList( builder, baseFolder, folder ) ;

        //---* Add the files *-------------------------------------------------
        Files.list( source ).filter( p -> !Files.isDirectory( p ) )
            .map( baseFolder::relativize )
            .forEach( builder::add );
    }   //  createFileList()

    /**
     *  Deletes the given folder and all containing files; calls itself
     *  recursively on contained folders.
     *
     *  @param  folder  The folder to delete.
     *  @throws IOException A problem occurred on deleting the folder or its
     *      contents.
     */
    private final void deleteFolder( final Path folder ) throws IOException
    {
        if( Files.isDirectory( requireNonNull( folder, "folder is null" ) ) )
        {
            //---* Delete the folder contents *--------------------------------
            for( final var file : Files.list( folder ).toList() )
            {
                if( !Files.isDirectory( file ) )
                {
                    //---* Plain files will be deleted immediately *-----------
                    Files.deleteIfExists( file );
                }
                else
                {
                    //---* Get rid of the folders … *--------------------------
                    deleteFolder( file );
                }
            }
        }

        //---* Delete the folder itself *--------------------------------------
        Files.deleteIfExists( folder );
    }   //  deleteFolder()

    /**
     *  The commit message.
     *
     *  @return The
     *      {@link Property}
     *      for the commit message.
     */
    @Input
    public abstract Property<String> getCommitMessage();

    /**
     *  The credentials for the access to the remote repository.
     *
     *  @return The
     *      {@link Property}
     *      for the credentials.
     */
    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<CredentialsProvider> getCredentials();

    /**
     *  Returns the debug flag.
     *
     *  @return If the
     *      {@link Property}
     *      is {@code true}, a lot of output is generated and written to
     *      {@link System#out}.
     */
    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<Boolean> getDebugFlag();

    /**
     *  {@summary The "DryRun" flag.} No cleanup is performed after a dry-run.
     *
     *  @return If the
     *      {@link Property}
     *      is {@code true}, the
     *      {@link org.eclipse.jgit.api.Git#push()}
     *      will be executed only as a dry-run.
     *
     *  @see org.eclipse.jgit.api.PushCommand#setDryRun(boolean)
     */
    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<Boolean> getDryRunFlag();

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
    @Input
    @org.gradle.api.tasks.Optional
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
    @InputDirectory
    @org.gradle.api.tasks.Optional
    public abstract Property<File> getJavadocLocation();

    /**
     *  The name of the target folder for the temporary local Git repository.
     *
     *  @return The
     *      {@link Property}
     *      for the name of the target folder.
     */
    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getLocalRepositoryFolder();

    /**
     *  The folder with the files that are published in the root of the Git
     *  repository (for example the {@code README.MD}).
     *
     *  @return The
     *      {@link Property}
     *      for the meta folder.
     */
    @InputDirectory
    @org.gradle.api.tasks.Optional
    public abstract Property<File> getMetaDir();

    /**
     *  The password for the access to the Git repository.
     *
     *  @return The
     *      {@link Property}
     *      for the Git repository password.
     */
    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getPassword();

    /**
     *  The URI for the remote repository.
     *
     *  @return The
     *      {@link Property}
     *      for the remote repository URI.
     */
    @Input
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
    @Input
    public abstract ListProperty<String> getSourcesList();

    /**
     *  The username for the access to the Git repository.
     *
     *  @return The
     *      {@link Property}
     *      for the Git repository username.
     */
    @Input
    @org.gradle.api.tasks.Optional
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
    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<File> getWorkFolder();

    /**
     *  The flag that indicates whether the temporary repository has to be
     *  removed in the end.
     *
     * @return The
     *      {@link Property}
     *      for the flag.
     */
    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<Boolean> getMustCleanupFlag();

    /**
     *  <p>{@summary Checks whether the given file is included in the
     *  transfer.}</p>
     *  <p>A file is included if either the argument {@code includedFiles}
     *  represents an empty list, or at least one pattern in that list
     *  matches the name of the given file, <i>and</i> none of the patterns in
     *  {@code excludedFiles} matches the name.</p>
     *
     *  @param  file    The file to assess.
     *  @param  includedFiles   The patterns for the names of the files to
     *      publish.
     *  @param  excludedFiles   The patterns for the names of files to ignore.
     *  @return {@code true} if the file has to be published, {@code false}
     *      otherwise.
     */
    private static final boolean isIncluded( final Path file, final Collection<PathMatcher> includedFiles, final Collection<PathMatcher> excludedFiles )
    {
        final var retValue = (includedFiles.isEmpty() || includedFiles.stream()
            .anyMatch( m -> m.matches( file ) ) )
            && isNotIgnored( file, excludedFiles );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  isIncluded()

    /**
     *  <p>{@summary Checks whether the given file is not ignored for the the
     *  transfer.} That does <i>not mean automatically</i> that the file is
     *  included, it just means that it is not listed as ignored.
     *
     *  @param  file    The file to assess.
     *  @param  excludedFiles   The patterns for the names of files to ignore.
     *  @return {@code true} if the file has to be published, {@code false}
     *      otherwise.
     */
    private static final boolean isNotIgnored( final Path file, final Collection<PathMatcher> excludedFiles )
    {
        final var retValue = excludedFiles.stream()
            .noneMatch( m -> m.matches( file ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  isNotIgnored()

    /**
     *  Scans the given folder for files and lists their names, relative to the
     *  base folder.
     *
     *  @param  baseFolder  The base folder.
     *  @param  source  The folder to scan.
     *  @throws IOException Cannot collect the files from the given source
     *      folder.
     */
    private static final void listFiles( final Path baseFolder, final Path source ) throws IOException
    {
        final List<Path> moreSources = new ArrayList<>();
        Files.list( source ).peek( entry ->
            {
                if( Files.isDirectory( entry ) ) moreSources.add( entry );
            } )
            .map( entry ->
            {
                final var format = Files.isDirectory( entry )
                                   ? "    D %s"
                                   : "    F %s";
                return format.formatted( baseFolder.relativize( entry ).toString() );
            } )
            .forEach( out::println );
        for( final var folder : moreSources ) listFiles( baseFolder, folder );
    }   //  listFiles()

    /**
     *  Dumps a status.
     *
     *  @param  status  The status to dump.
     */
    private final void printStatus( final Status status )
    {
        for( final var s : status.getAdded() )              out.printf( "    | A %s%n", s );
        for( final var s : status.getChanged() )            out.printf( "    | C %s%n", s );
        for( final var s : status.getConflicting() )        out.printf( "    | X %s%n", s );
        for( final var s : status.getIgnoredNotInIndex() )  out.printf( "    | i %s%n", s );
        for( final var s : status.getMissing() )            out.printf( "    | # %s%n", s );
        for( final var s : status.getModified() )           out.printf( "    | M %s%n", s );
        for( final var s : status.getRemoved() )            out.printf( "    | R %s%n", s );
        for( final var s : status.getUncommittedChanges() ) out.printf( "    | U %s%n", s );
        for( final var s : status.getUntracked() )          out.printf( "    | u %s%n", s );
        for( final var s : status.getUntrackedFolders() )   out.printf( "    | f %s%n", s );
    }   //  printStatus()

    /**
     *  Publishes the artifacts to the specified Git repository.
     *
     *  @throws GitAPIException A Git operation failed
     *  @throws IOException A file operation failed.
     */
    @TaskAction
    public final void publish() throws GitAPIException, IOException
    {
        m_IsDebug = getDebugFlag().getOrElse( FALSE ).booleanValue();

        final var project = getProject();

        if( m_IsDebug )
        {
            out.printf( "-> The Contents of the Project Folder: %s%n", project.getProjectDir().getAbsolutePath() );
            listFiles( project.getProjectDir().toPath(), project.getProjectDir().toPath() );
        }

        final var dryRun = getDryRunFlag().getOrElse( FALSE ).booleanValue();
        final var mustCleanup = getMustCleanupFlag().getOrElse( TRUE ).booleanValue() & !dryRun;

        //---* Prepare the target folder *-------------------------------------
        final var baseFolder = getWorkFolder()
            .getOrElse( new File( project.getProjectDir(), BASE_FOLDER_DEFAULT ) )
            .getAbsoluteFile()
            .toPath();

        if( m_IsDebug ) out.printf( "-> Work Folder: %s%n", baseFolder );

        if( !Files.exists( baseFolder ) )
        {
            Files.createDirectories( baseFolder );
        }
        if( !Files.isDirectory( baseFolder ) ) throw new IOException( "Not a directory: %s".formatted( baseFolder.toString() ) );

        final var targetFolder = getLocalRepositoryFolder()
            .map( baseFolder::resolve )
            .getOrElse( Files.createTempDirectory( baseFolder, TARGET_NAME ) );
        if( !Files.exists( targetFolder ) )
        {
            Files.createDirectories( targetFolder );
        }
        else
        {
            if( !Files.isDirectory( targetFolder ) ) throw new IOException( "Not a directory: %s".formatted( targetFolder.toString() ) );
        }

        //---* Prepare the credentials *---------------------------------------
        final CredentialsProvider credentialsProvider;
        if( getCredentials().isPresent() )
        {
            credentialsProvider = getCredentials().get();
        }
        else
        {
            final var username = getUsername().get();
            final var password = getPassword().get();
            credentialsProvider = new UsernamePasswordCredentialsProvider( username, password );
        }

        //---* Clone the remote repository *-----------------------------------
        try
        {
            if( m_IsDebug ) out.println( "-> PULL" );
            final var remoteRepositoryURI = getRemoteRepositoryURI().get();
            Git git = Git.cloneRepository()
                .setURI( remoteRepositoryURI.toString() )
                .setCredentialsProvider( credentialsProvider )
                .setDirectory( targetFolder.toFile() )
                .call();
            try( git )
            {
                /*
                 * We expect that immediately after the clone, the status is
                 * clean. If not, something is wrong.
                 */
                var status = checkStatus( git );
                if( status.isPresent() )
                {
                    printStatus( status.get() );
                    throw new GitPublisherException( "Dirty Status" );
                }

                //---* Move the files from the project to the repository *-----
                if( m_IsDebug ) out.println( "-> Transfer Project Files" );
                transferFiles( project, targetFolder );

                if( m_IsDebug )
                {
                    out.printf( "-> The Contents of the Repository Folder: %s%n", targetFolder );
                    listFiles( targetFolder, targetFolder );
                }

                /*
                 * There is nothing to do if the status is still clean here.
                 * Otherwise, we need to check what to do.
                 */
                status = checkStatus( git );
                if( status.isPresent() )
                {
                    //---* Inspect the status *--------------------------------
                    final var s = status.get();
                    if( m_IsDebug )
                    {
                        out.println( "-> Status before ADD/RM" );
                        printStatus( s );
                    }

                    //---* Add new and modified files *------------------------
                    Set<String> patterns = new HashSet<>( s.getUntracked() );
                    patterns.addAll( s.getModified() );
                    if( !patterns.isEmpty() )
                    {
                        final var addCommand = git.add()
                            .setUpdate( false );
                        for( final var file : patterns )
                        {
                            addCommand.addFilepattern( file );
                        }
                        addCommand.call();
                    }

                    //---* Remove deleted files *------------------------------
                    patterns = s.getMissing();
                    if( !patterns.isEmpty() )
                    {
                        final var deleteCommand = git.rm();
                        for( final var file : patterns )
                        {
                            deleteCommand.addFilepattern( file );
                        }
                        deleteCommand.call();
                    }

                    if( m_IsDebug )
                    {
                        status = checkStatus( git );
                        if( status.isPresent() )
                        {
                            out.println( "-> Status before COMMIT" );
                            printStatus( status.get() );
                        }
                    }

                    //---* Commit the changes *--------------------------------
                    if( m_IsDebug ) out.println( "-> COMMIT" );
                    @SuppressWarnings( "unused" )
                    final var commit = git.commit()
                        .setMessage( getCommitMessage().get() )
                        .call();

                    //---* Get the status again and inspect it *---------------
                    status = checkStatus( git );
                    if( m_IsDebug )
                    {
                        out.println( "-> Status after COMMIT" );
                        status.ifPresent( this::printStatus );
                    }

                    //---* Push the project *----------------------------------
                    if( m_IsDebug ) out.println( "-> PUSH" );
                    @SuppressWarnings( "unused" )
                    final var pushResult = git.push()
                        .setDryRun( dryRun )
                        .setCredentialsProvider( credentialsProvider )
                        .call();

                    //---* Get the status again *------------------------------
                    status = checkStatus( git );
                    if( m_IsDebug )
                    {
                        out.println( "-> Status after PUSH" );
                        status.ifPresent( this::printStatus );
                    }
                }
            }
        }
        finally
        {
            if( mustCleanup ) deleteFolder( targetFolder );
        }
    }   //  publish()

    /**
     *  Parses the given list with the file name patterns.
     *
     *  @param  sourcesList The
     *      {@link Provider}
     *      instance for the list to parse.
     *  @return The patterns.
     */
    private final Collection<PathMatcher> parseFilePatternList( final Provider<List<String>> sourcesList )
    {
        var retValue = List.<PathMatcher>of();
        if( sourcesList.isPresent() )
        {
            final var fileSystem = FileSystems.getDefault();
            retValue = sourcesList.get().stream()
                .filter( l -> !l.isBlank() )
                .map( String::stripLeading )
                .filter( l -> !l.startsWith( "#" ) )
                .map( p -> p.startsWith( "glob:" ) || p.startsWith( "regex:" ) ? p : "glob:%s".formatted( p ) )
                .map( fileSystem::getPathMatcher )
                .toList();
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  parseFilePatternList()

    /**
     *  <p>{@summary Transfers the project files to the target folder.}</p>
     *  <p>The method calls
     *  {@link #getSourcesList()},
     *  {@link #getMetaDir()}
     *  and
     *  {@link #getIgnoresList()}
     *  to determine the files to move.</p>
     *
     *  @param  project The project, providing the information for the sources
     *      of the files to transfer.
     *  @param  targetFolder    The target folder.
     *  @throws IOException Cannot transfer a file.
     */
    private void transferFiles( final Project project, final Path targetFolder ) throws IOException
    {
        //---* Load the names for the included files and folders *-------------
        final var includes = parseFilePatternList( getSourcesList() );

        //---* Load the names for the excluded files and folders *-------------
        Collection<PathMatcher> excludes = new ArrayList<>( parseFilePatternList( getIgnoresList() ) );
        excludes.addAll( alwaysIgnored() );
        excludes = List.copyOf( excludes );

        //---* Copy the project files *----------------------------------------
        final var sourceFolder = project.getProjectDir().toPath();
        final Set<Path> filesToCopy = new HashSet<>();
        if( !includes.isEmpty() ) filesToCopy.addAll(  createFileList( sourceFolder, includes, excludes )  );
        for( final var path : filesToCopy )
        {
            final var source = sourceFolder.resolve( path );
            final var target = targetFolder.resolve( path );
            Files.createDirectories( target.getParent() );
            Files.copy( source, target, REPLACE_EXISTING, COPY_ATTRIBUTES );
            Files.setLastModifiedTime( target, Files.getLastModifiedTime( source ) );
        }

        //---* Copy the files from the meta folder *---------------------------
        final var metaFolder = getMetaDir().getOrElse( new File( project.getProjectDir(), META_DIR_NAME ) ).toPath();
        if( Files.exists( metaFolder ) )
        {
            for( final var path : createFileList( metaFolder, List.of(), excludes ) )
            {
                final var source = metaFolder.resolve( path );
                final var target = targetFolder.resolve( path );
                Files.createDirectories( target.getParent() );
                Files.copy( source, target, REPLACE_EXISTING, COPY_ATTRIBUTES );
                Files.setLastModifiedTime( target, Files.getLastModifiedTime( source ) );
                filesToCopy.add( path );
            }
        }

        //---* Copy the Javadoc files *----------------------------------------
        if( getJavadocLocation().isPresent() )
        {
            final var directory = getJavadocLocation().get().toPath();
            final var javadocSource = directory.isAbsolute() ? directory : sourceFolder.resolve( directory );
            if( Files.isDirectory( javadocSource ) )
            {
                final var javadocTarget = targetFolder.resolve( "javadoc" );
                Files.createDirectories( javadocTarget );
                for( final var path : createFileList( javadocSource, List.of(), excludes ) )
                {
                    final var source = javadocSource.resolve( path );
                    final var target = javadocTarget.resolve( path );
                    Files.createDirectories( target.getParent() );
                    Files.copy( source, target, REPLACE_EXISTING, COPY_ATTRIBUTES );
                    Files.setLastModifiedTime( target, Files.getLastModifiedTime( source ) );
                    filesToCopy.add( targetFolder.relativize( target ) );
                }
            }
        }

        //---* Take care of the dangling files *-------------------------------

        for( final var path : createFileList( targetFolder, List.of(), excludes ) )
        {
            if( !filesToCopy.contains( path ) )
            {
                if( m_IsDebug ) out.printf( "    --- Deleted dangling File: %s%n", path.toString() );
                Files.deleteIfExists( targetFolder.resolve( path ) );
            }
        }
    }   //  transferFiles()
}
//  class PublishToGIT

/*
 *  End of File
 */