package org.apache.maven.plugins.javadoc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Helper class to compute and write data used to detect a
 * stale javadoc.
 */
public class StaleHelper
{

    /**
     * Compute the data used to detect a stale javadoc
     *
     * @param cmd the command line
     * @return the stale data
     * @throws MavenReportException if an error occurs
     */
    public static String getStaleData( Commandline cmd )
            throws MavenReportException
    {
        try
        {
            List<String> ignored = new ArrayList<>();
            List<String> options = new ArrayList<>();
            Path dir = cmd.getWorkingDirectory().toPath().toAbsolutePath().normalize();
            String[] args = cmd.getCommandline();
            Collections.addAll( options, args );
            for ( String arg : args )
            {
                if ( arg.startsWith( "@" ) )
                {
                    String name = arg.substring( 1 );
                    Files.lines( dir.resolve( name ) ).forEachOrdered( options::add );
                    ignored.add( name );
                }
            }
            List<String> state = new ArrayList<>( options );
            boolean cp = false;
            boolean sp = false;
            for ( String arg : options )
            {
                if ( cp )
                {
                    String s = unquote( arg );
                    Stream.of( s.split( File.pathSeparator ) )
                            .map( dir::resolve )
                            .map( p -> p + " = " + lastmod( p ) )
                            .forEachOrdered( state::add );
                }
                else if ( sp )
                {
                    String s = unquote( arg );
                    Stream.of( s.split( File.pathSeparator ) )
                            .map( dir::resolve )
                            .flatMap( StaleHelper::walk )
                            .filter( Files::isRegularFile )
                            .map( p -> p + " = " + lastmod( p ) )
                            .forEachOrdered( state::add );
                }
                cp = "-classpath".equals( arg );
                sp = "-sourcepath".equals( arg );
            }
            walk( dir )
                    .filter( Files::isRegularFile )
                    .filter( p -> !ignored.contains( p.getFileName().toString() ) )
                    .map( p -> p + " = " + lastmod( p ) )
                    .forEachOrdered( state::add );

            return String.join( SystemUtils.LINE_SEPARATOR, state );
        }
        catch ( Exception e )
        {
            throw new MavenReportException( "Unable to compute stale date", e );
        }
    }

    /**
     * Write the data used to detect a stale javadoc
     *
     * @param cmd the command line
     * @param path the stale data path
     * @throws MavenReportException if an error occurs
     */
    public static void writeStaleData( Commandline cmd, Path path )
            throws MavenReportException
    {
        try
        {
            String curdata = getStaleData( cmd );
            Files.createDirectories( path.getParent() );
            try ( Writer w = Files.newBufferedWriter( path ) )
            {
                w.append( curdata );
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Error checking stale data", e );
        }
    }

    private static Stream<Path> walk( Path dir )
    {
        try
        {
            return Files.walk( dir );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static String unquote( String s )
    {
        if ( s.startsWith( "'" ) && s.endsWith( "'" ) )
        {
            return s.substring( 1, s.length() - 1 ).replaceAll( "\\\\'", "'" );
        }
        else
        {
            return s;
        }
    }

    private static long lastmod( Path p )
    {
        try
        {
            return Files.getLastModifiedTime( p ).toMillis();
        }
        catch ( IOException e )
        {
            return 0;
        }
    }

}
