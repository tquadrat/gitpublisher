/*
 * ============================================================================
 * Copyright © 2002-2026 by Thomas Thrien.
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

import static java.lang.System.out;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;

/**
 *  <p>{@summary Having some fun with the filesystem.}</p>
 *
 *  @author Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: HexUtils.java 747 2020-12-01 12:40:38Z tquadrat $
 *  @since 0.25.0
 *
 */
public class FSPlayground
{
        /*---------------*\
    ====** Inner Classes **====================================================
        \*---------------*/

        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/

        /*------------------------*\
    ====** Static Initialisations **===========================================
        \*------------------------*/

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  The program entry point.
     *
     *  @param  args    The command line arguments.
     */
    public static final void main( final String... args )
    {
        try
        {
            final var file = Path.of( ".", "org.tquadrat.foundation.gradle.gitpublisher", "src", "test", "java", "org", "tquadrat", "foundation", "gradle", "gitpublisher", "FSPlayground.java" ).toAbsolutePath().normalize();
            if( Files.exists( file ) )
            {
                out.printf( "File: %s%n", file );
                final Map<String,Object> attributes = Files.readAttributes( file, "posix:owner,lastAccessTime,creationTime,permissions,group", NOFOLLOW_LINKS );
                final var buffer = new StringJoiner( ", ", "Attributes: ", "\n" );
                buffer.setEmptyValue( "No attributes!" );
                for( final var attribute : attributes.keySet() )
                {
                    buffer.add( attribute );
                }
                out.print( buffer.toString() );
                for( final var attribute : attributes.entrySet() )
                {
                    out.printf( "%s\t: %s%n", attribute.getKey(), attribute.getValue() );
                }
                for( final var attribute : attributes.entrySet() )
                {
                    Files.setAttribute( file, "posix:" + attribute.getKey(), attribute.getValue(), NOFOLLOW_LINKS );
                }
            }
            else
            {
                out.printf( "File does not exist: %s%n", file );
            }
        }
        catch( final Throwable t )
        {
            t.printStackTrace();
        }
    }   //  main()

}
//  class FSPlayground

/*
 *  End of File
 */