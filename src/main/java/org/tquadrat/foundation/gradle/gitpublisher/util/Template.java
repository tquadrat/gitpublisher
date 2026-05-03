/*
 * ============================================================================
 *  Copyright © 2002-2025 by Thomas Thrien.
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

package org.tquadrat.foundation.gradle.gitpublisher.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.compile;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *  <p>{@summary An instance of this class is basically a wrapper around a
 *  String that contains placeholders (&quot;Variables&quot;) in the form
 *  <code>${&lt;<i>name</i>&gt;}</code>, where &lt;<i>name</i> is the variable
 *  name.}</p>
 *  <p>The variables names are case-sensitive.</p>
 *  <p>Valid variable names may not contain other characters than the letters
 *  from 'a' to 'z' (upper case and lower case), the digits from '0' to '9' and
 *  the special characters underscore ('_') and dot ('.'), after an optional
 *  prefix character.</p>
 *  <p>Allowed prefixes are the tilde ('~'), the slash ('/'), the equal sign
 *  ('='), the colon (':'), the per cent sign ('%'), and the ampersand
 *  ('&amp;').</p>
 *  <p>The prefix character is part of the name.</p>
 *  <p>Finally, there is the single underscore that is allowed as a special
 *  variable.</p>
 *
 *  @author Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: Template.java 1221 2026-05-03 12:20:32Z tquadrat $
 *
 *  @since 0.25.0
 */
public class Template implements Serializable
{
        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  The variable name for the current date and time: {@value}.
     *
     *  @see Instant#now()
     */
    public static final String VARNAME_Now = "now";

    /**
     *  The variable name for the version: {@value}.
     */
    public static final String VARNAME_Version = "version";

    /**
     *  The regular expression to identify a variable in a char sequence:
     *  {@value}.
     *
     *  @see #replaceVariable(Map...)
     */
    @SuppressWarnings( "RegExpUnnecessaryNonCapturingGroup" )
    public static final String VARIABLE_PATTERN = "\\$\\{((?:_)|(?:[~/=%:&]?\\p{IsAlphabetic}(?:\\p{IsAlphabetic}|\\d|_|.)*?))}";

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The template text.
     *
     *  @serial
     */
    private final String m_TemplateText;

        /*------------------------*\
    ====** Static Initialisations **===========================================
        \*------------------------*/
    /**
     *  The pattern that is used to identify a variable in a char sequence.
     *
     *  @see #replaceVariable(Map...)
     *  @see #VARIABLE_PATTERN
     */
    private static final Pattern m_VariablePattern;

    /**
     *  The serial version UID for objects of this class: {@value}.
     *
     *  @hidden
     */
    @Serial
    private static final long serialVersionUID = 1L;

    static
    {
        //---* The regex patterns *--------------------------------------------
        try
        {
            m_VariablePattern = compile( VARIABLE_PATTERN );
        }
        catch( final PatternSyntaxException e )
        {
            throw new ExceptionInInitializerError( new Error( "The patterns are constant values that have been tested", e ) );
        }
    }

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code Template}.
     *
     *  @param  templateText    The template text, containing variable in the
     *      form <code>${&lt;<i>name</i>&gt;}</code>.
     */
    public Template( final CharSequence templateText )
    {
        if( isNull( templateText ) ) throw new IllegalArgumentException( "templateText is null" );
        m_TemplateText = templateText.toString();
    }   //  Template()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Escapes backslash ('\') and dollar sign ('$') for regex replacements.
     *
     *  @param  input   The source string.
     *  @return The string with the escaped characters.
     *
     *  @see java.util.regex.Matcher#appendReplacement(StringBuffer,String)
     */
    private static String escapeRegexReplacement( final CharSequence input )
    {
        assert nonNull( input ) : "input is null";

        //---* Escape the backslashes and dollar signs *-------------------
        final var len = input.length();
        final var retValue = new StringBuilder( (len * 12) / 10 );
        char c;
        EscapeLoop: for( var i = 0; i < len; ++i )
        {
            c = input.charAt( i );
            switch( c )
            {
                case '\\':
                case '$':
                    retValue.append( '\\' ); // The fall through is intended here!
                    //$FALL-THROUGH$
                default: // Do nothing ...
            }
            retValue.append( c );
        }   //  EscapeLoop:

        //---* Done *----------------------------------------------------------
        return retValue.toString();
    }   //  escapeRegexReplacement()

    /**
     *  <p>{@summary Replaces the variables of the form
     *  <code>${&lt;<i>name</i>&gt;}</code> in the adjusted template with
     *  values from the given maps and returns it after formatting the result.}
     *  The method will try the maps in the given sequence, it stops after the
     *  first match.</p>
     *  <p>If no replacement value could be found, the variable will not be
     *  replaced at all.</p>
     *  <p>If a value from one of the maps contains a variable itself, this
     *  will not be replaced.</p>
     *  <p>The variables names are case-sensitive.</p>
     *  <p>Valid variable names may not contain other characters than the
     *  letters from 'a' to 'z' (upper case and lower case), the digits from
     *  '0' to '9' and the special characters underscore ('_') and dot ('.'),
     *  after an optional prefix character.</p>
     *  <p>Allowed prefixes are the tilde ('~'), the slash ('/'), the equal
     *  sign ('='), the colon (':'), the per cent sign ('%'), and the ampersand
     *  ('&amp;').</p>
     *  <p>The prefix character is part of the name.</p>
     *  <p>Finally, there is the single underscore that is allowed as a
     *  special variable.</p>
     *
     *  @param  sources The maps with the replacement values.
     *  @return The new text, or {@code null} if the provided value for
     *      {@code text} was already {@code null}.
     *
     *  @see #VARIABLE_PATTERN
     */
    @SuppressWarnings( {"TypeParameterExplicitlyExtendsObject", "ExtractMethodRecommender"} )
    @SafeVarargs
    public final String replaceVariable( final Map<String,? extends Object>... sources )
    {
        if( isNull( sources ) ) throw new IllegalArgumentException( "sources is null" );

        final Function<? super String,Optional<String>> retriever = variable -> retrieveVariableValue( variable, sources );

        final Map<String,String> cache = new HashMap<>();

        final var matcher = m_VariablePattern.matcher( m_TemplateText );
        final var buffer = new StringBuilder();
        while( matcher.find() )
        {
            final var variable = matcher.group( 0 );
            final var replacement = cache.computeIfAbsent( variable, v -> escapeRegexReplacement( retriever.apply( matcher.group( 1 ) ).orElse( v ) ) );
            matcher.appendReplacement( buffer, replacement );
        }
        matcher.appendTail( buffer );
        final var retValue = buffer.toString();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  replaceVariable()

    /**
     *  Tries to obtain a value for the given key from one of the given
     *  sources that will be searched in the given sequence order.
     *
     *  @param  name    The name of the value.
     *  @param  sources The maps with the values.
     *  @return An instance of
     *      {@link Optional}
     *      that holds the value from one of the sources.
     */
    @SuppressWarnings( "TypeParameterExplicitlyExtendsObject" )
    @SafeVarargs
    private static final Optional<String> retrieveVariableValue( final String name, final Map<String,? extends Object>... sources )
    {
        assert nonNull( name ) : "name is null";
        assert nonNull( sources ) : "sources is null";

        Optional<String> retValue = Optional.empty();

        //---* Search the sources *--------------------------------------------
        Object value = null;
        SearchLoop: for( final var map : sources )
        {
            value = map.get( name );
            if( nonNull( value ) ) break SearchLoop;
        }   //  SearchLoop:

        if( nonNull( value ) )
        {
            //---* Escape the backslashes and dollar signs *-------------------
            retValue = Optional.of( value.toString() );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  retrieveVariableValue()
}
//  class Template

/*
 *  End of File
 */