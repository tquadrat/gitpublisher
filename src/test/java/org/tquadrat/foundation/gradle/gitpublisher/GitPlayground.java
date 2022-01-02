/*
 * ============================================================================
 * Copyright © 2002-2021 by Thomas Thrien.
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

import static java.lang.System.err;
import static java.lang.System.out;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.lines;
import static java.nio.file.Files.writeString;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 *  A playground for experiments with JGit.
 *
 *  @version $Id: GitPlayground.java 959 2022-01-02 23:09:45Z tquadrat $
 *  @author Thomas Thrien - thomas.thrien@tquadrat.org
 *  @since 0.1.0
 */
public class GitPlayground
{
        /*---------------*\
    ====** Inner Classes **====================================================
        \*---------------*/

        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  The formatter for timestamps on filenames.
     */
    public static final DateTimeFormatter TIMESTAMP_FORMATTER;

    static
    {
        TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue( INSTANT_SECONDS )
            .toFormatter();
    }

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/

        /*------------------------*\
    ====** Static Initialisations **===========================================
        \*------------------------*/
    /**
     *  The file name patterns for files that are not of interest.
     */
    private static final List<PathMatcher> m_IgnoredFiles;

    static
    {
        final var fileSystem = FileSystems.getDefault();
        //noinspection ConstantConditions
        m_IgnoredFiles = Stream.of( ".git", "dummy" )
            .filter( Objects::nonNull )
            .filter( p -> !p.isBlank() )
            .map( p -> p.startsWith( "glob:" ) || p.startsWith( "regex:" ) ? p : "glob:%s".formatted( p ) )
            .map( fileSystem::getPathMatcher )
            .toList();
    }

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Scans the given folder for files that should be removed on program
     *  termination.
     *
     *  @param  source  The folder to scan.
     *  @param  registry    The registry for the files to remove.
     *  @throws IOException Cannot collect the files from the given source
     *      folder.
     */
    private static final void collectFilesForRemoval( final Path source, final Stack<Path> registry ) throws IOException
    {
        final List<Path> moreSources = new ArrayList<>();
        Files.list( source ).forEach( entry ->
        {
            registry.push( entry );
            if( Files.isDirectory( entry ) ) moreSources.add( entry );
        } );
        for( final var folder : moreSources ) collectFilesForRemoval( folder, registry );
    }   //  collectFilesForRemoval()

    /**
     *  Creates a list of the relevant files in the {@code source} folder.
     *
     *  @param  source  The folder to scan.
     *  @param  ignoredFiles    The patterns for the names of files to ignore.
     *  @return The relevant files.
     *  @throws IOException Cannot collect the files from the given source
     *      folder.
     */
    @SuppressWarnings( "SameParameterValue" )
    private static Collection<Path> getFileList( final Path source, final List<PathMatcher> ignoredFiles ) throws IOException
    {
        return getFileList( source, source, ignoredFiles );
    }   //  getFileList()

    /**
     *  Creates a list of the relevant files in the {@code source} folder.
     *
     *  @param  baseFolder  The base folder.
     *  @param  source  The folder to scan.
     *  @param  ignoredFiles    The patterns for the names of files to ignore.
     *  @return The relevant files.
     *  @throws IOException Cannot collect the files from the given source
     *      folder.
     */
    private static Collection<Path> getFileList( final Path baseFolder, final Path source, final List<PathMatcher> ignoredFiles )  throws IOException
    {
        final List<Path> retValue = new ArrayList<>();
        if( !isIgnored( baseFolder.relativize( source ), ignoredFiles ) )
        {
            final var moreSources = Files.list( source )
                .filter( Files::isDirectory )
                .toList();
            for( final var folder : moreSources ) retValue.addAll( getFileList( baseFolder, folder, ignoredFiles ) );

            retValue.addAll( Files.list( source ).filter( p -> !Files.isDirectory( p ) )
                .map( baseFolder::relativize )
                .filter( p -> !isIgnored( p, ignoredFiles ) )
                .toList() );

            retValue.sort( comparing( Path::toString ) );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getFileList()

    /**
     *  Checks whether the given file has to be ignored.
     *
     *  @param  file    The file to assess.
     *  @param  ignoredFiles    The patterns for the names of files to ignore.
     *  @return {@code true} if the file has to be ignored, {@code false}
     *      otherwise.
     */
    @SuppressWarnings( "BooleanMethodIsAlwaysInverted" )
    private static final boolean isIgnored( final Path file, final List<PathMatcher> ignoredFiles )
    {
        final var retValue = ignoredFiles.stream()
            .anyMatch( m -> m.matches( file ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  isIgnored()

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
     *  The program entry point.
     *
     *  @param  args    The command line parameters.
     */
    public static final void main( final String... args )
    {
        final Stack<Path> forRemoval = new Stack<>();
        final var cleanup = true;

        Path baseFolder;
        Path targetFolder = null;
        try
        {
            baseFolder = Paths.get( "scratch" ).normalize();
            out.printf( "Base folder (URI)        : %s%n", baseFolder.toUri() );
            out.printf( "Base folder (File name)  : %s%n", baseFolder.toFile().getAbsolutePath() );

            targetFolder = createTempDirectory( baseFolder, "JGitTemp" );
            out.printf( "Target folder (URI)      : %s%n", targetFolder.toUri() );
            out.printf( "Target folder (File name): %s%n", targetFolder.toFile().getAbsolutePath() );
            out.printf( "Target folder exists?    : %s%n", exists( targetFolder ) ? "yes" : "no" );
            forRemoval.push( targetFolder );

            final var changeLog = targetFolder.resolve( "ChangeLog" );
            out.printf( "Change Log (URI)         : %s%n", changeLog.toUri() );
            out.printf( "Change Log (File name)   : %s%n", changeLog.toFile().getAbsolutePath() );

            final var remoteRepositoryURI = new URI( "https://github.com/tquadrat/Playground.git" );
            out.printf( "Remote Repository URI    : %s%n", remoteRepositoryURI );

            final var userName = "tquadrat";
            final var password = "github_#Tan88go";
            out.printf( "User name                 : %s%n", userName );
            out.printf( "Password                  : %s%n", password );

            @SuppressWarnings( "SpellCheckingInspection" )
            final var accessToken = "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"; // expires 2022-01-26;
            out.printf( "Access Token              : %s%n", accessToken );

            final var emailAddress = "thomas.thrien@tquadrat.org";
            out.printf( "email Address             : %s%n", emailAddress );

            //---* Create the credentials provider *---------------------------
            final var credentialsProvider = new UsernamePasswordCredentialsProvider( "Token", accessToken );

            //---* Clone the remote repository *-------------------------------
            out.println( "\n-> Cloning the repository");
            Git git = Git.cloneRepository()
                .setURI( remoteRepositoryURI.toString() )
                .setDirectory( targetFolder.toFile() )
                .call();
            try( git )
            {
                out.println( "-> Success!" );

                //---* List the files *----------------------------------------
                listFiles( baseFolder, targetFolder );

                //---* Update the change log *---------------------------------
                if( exists( changeLog ) )
                {
                    out.println( "-> Update the change log" );
                    writeString( changeLog, "%s - Visited%n".formatted( ZonedDateTime.now().toString() ), APPEND );
                    lines( changeLog ).map( "    | %s"::formatted )
                        .forEach( out::println );
                }
                git.add()
                    .setUpdate( true )
                    .addFilepattern( changeLog.getName( changeLog.getNameCount() - 1 ).toString() )
                    .call();

                //---* Create a new file *-------------------------------------
                out.println( "-> Create a new file" );
                final var dummyFile = targetFolder.resolve( "dummy.txt" );
                writeString( dummyFile, "Created on %s%n".formatted( ZonedDateTime.now().toString() ), CREATE );

                //---* Create a new file and add it to Git *-------------------
                out.println( "-> Add a new file" );
                final var fileName = "dummy/File_%s.txt".formatted( TIMESTAMP_FORMATTER.format( Instant.now() ) );
                final var newFile = targetFolder.resolve( fileName );
                out.printf( "    | %s%n", fileName );
                writeString( newFile, "Created on %s%n".formatted( ZonedDateTime.now().toString() ), CREATE );
                git.add()
                    .setUpdate( false )
                    .addFilepattern( fileName )
                    .call();

                //---* Get the status *----------------------------------------
                out.println( "-> Status before COMMIT" );
                var status = git.status()
                    .call();
                printStatus( out, status );

                //---* Commit the changes *------------------------------------
                out.println( "-> Commit" );
                final var commit = git.commit()
                    .setMessage( "Updated the project" )
                    .call();
                printCommit( out, commit );

                //---* Get the status again *----------------------------------
                out.println( "-> Status after COMMIT" );
                status = git.status()
                    .call();
                printStatus( out, status );

                out.printf( "-> Relevant Files in %s%n", targetFolder );
                for( var file : getFileList( targetFolder, m_IgnoredFiles ) )
                {
                    out.printf( "    | %s%n", file.toString() );
                }
                out.println();

                //---* Push the project *--------------------------------------
                out.println( "-> Push" );
                final var pushResult = git.push()
                    .setDryRun( true )
                    .setCredentialsProvider( credentialsProvider )
                    .call();
                for( final var result : pushResult )
                {
                    final var messages = result.getMessages();
                    if( nonNull( messages ) && !messages.isBlank() ) out.printf( "    | %s%n", result.getMessages() );
                    for( final var update : result.getRemoteUpdates() )
                    {
                        out.printf( "    | -- Update: %s%n", update.getRemoteName() );
                        final var message = update.getMessage();
                        if( nonNull( message ) && !message.isBlank() ) out.printf( "    | %s%n", message );
                        out.printf( "    | Status %s%n", update.getStatus().toString() );
                    }
                }

                //---* Get the status again *----------------------------------
                out.println( "-> Status after PUSH" );
                status = git.status()
                    .call();
                printStatus( out, status );
            }
        }
        catch( final Throwable t )
        {
            //---* Deal with the previously unhandled exceptions *-------------
            t.printStackTrace( err );
        }
        finally
        {
            //noinspection ConstantConditions
            if( cleanup && nonNull( targetFolder ) )
            {
                out.println( "\n-> Housekeeping!" );
                try
                {
                    collectFilesForRemoval( targetFolder, forRemoval );
                    while( !forRemoval.empty() )
                    {
                        deleteIfExists( forRemoval.pop() );
                    }
                }
                catch( final IOException e )
                {
                    e.printStackTrace( err );
                }
            }
        }
    }   //  main()

    /**
     *  Dumps a commit.
     *
     *  @param  out The output target.
     *  @param  commit  The commit to dump.
     *  @throws IOException Cannot write to the output target.
     */
    @SuppressWarnings( "SameParameterValue" )
    private static void printCommit( final Appendable out, final RevCommit commit ) throws IOException
    {
        final var frame = "    | %s%n";
        out.append( frame.formatted( commit.getName() ) );
        out.append( frame.formatted( commit.getFullMessage() ) );
        final var commitTime = Instant.from( TIMESTAMP_FORMATTER.parse( Integer.toString( commit.getCommitTime() ) ) );
        out.append( frame.formatted( commitTime ) );

        for( var entry : Map.of( "Committer", commit.getCommitterIdent(), "Author", commit.getAuthorIdent() ).entrySet() )
        {
            out.append( frame.formatted( " -- "+ entry.getKey() ) );
            final var personId = entry.getValue();
            out.append( frame.formatted( personId.getName() ) );
            out.append( frame.formatted( personId.getEmailAddress() ) );
            out.append( frame.formatted( personId.getTimeZone().toZoneId() ) );
            out.append( frame.formatted( ZonedDateTime.ofInstant( personId.getWhen().toInstant(), ZoneId.systemDefault() ) ) );
        }

        final var revTree = commit.getTree();
        out.append( frame.formatted( revTree.toString() ) );

        final var footerLines = commit.getFooterLines();
        if( !footerLines.isEmpty() )
        {
            out.append( frame.formatted( " -- Footer lines" ) );
            for( final var footerLine : footerLines )
            {
                out.append( frame.formatted( footerLine.getKey() ) );
                out.append( frame.formatted( footerLine.getValue() ) );
            }
        }
    }   //  printCommit()

    /**
     *  Dumps a status.
     *
     *  @param  out The output target.
     *  @param  status  The status to dump.
     *  @throws IOException Cannot write to the output target.
     */
    @SuppressWarnings( "SameParameterValue" )
    private static void printStatus( final Appendable out, final Status status ) throws IOException
    {
        for( final var s : status.getAdded() )       out.append( "    | A %s%n".formatted( s ) );
        for( final var s : status.getChanged() )     out.append( "    | C %s%n".formatted( s ) );
        for( final var s : status.getConflicting() ) out.append( "    | X %s%n".formatted( s ) );
        for( final var s : status.getMissing() )     out.append( "    | # %s%n".formatted( s ) );
        for( final var s : status.getModified() )    out.append( "    | M %s%n".formatted( s ) );
        for( final var s : status.getRemoved() )     out.append( "    | R %s%n".formatted( s ) );
        for( final var s : status.getUntracked() )   out.append( "    | U %s%n".formatted( s ) );
    }   //  printStatus()
}
//  class GitPlayground

/*
 *  End of File
 */