/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.osgi.bundle.shell;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import org.apache.osgi.service.shell.CdCommand;
import org.apache.osgi.service.shell.Command;
import org.osgi.framework.*;

public class InstallCommandImpl implements Command
{
    private BundleContext m_context = null;

    public InstallCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "install";
    }

    public String getUsage()
    {
        return "install <URL> [<URL> ...]";
    }

    public String getShortDescription()
    {
        return "install bundle(s).";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        // There should be at least one URL.
        if (st.countTokens() >= 1)
        {
            while (st.hasMoreTokens())
            {
                String location = st.nextToken().trim();
                install(location, out, err);
            }
        }
        else
        {
            err.println("Incorrect number of arguments");
        }
    }

    protected Bundle install(String location, PrintStream out, PrintStream err)
    {
        String abs = absoluteLocation(location);
        if (abs == null)
        {
            err.println("Malformed location: " + location);
        }
        else
        {
            try
            {
                return m_context.installBundle(abs, null);
            }
            catch (IllegalStateException ex)
            {
                err.println(ex.toString());
            }
            catch (BundleException ex)
            {
                if (ex.getNestedException() != null)
                {
                    err.println(ex.getNestedException().toString());
                }
                else
                {
                    err.println(ex.toString());
                }
            }
            catch (Exception ex)
            {
                err.println(ex.toString());
            }
        }
        return null;
    }

    private String absoluteLocation(String location)
    {
        if (!location.endsWith(".jar"))
        {
            location = location + ".jar";
        }
        try
        {
            new URL(location);
        }
        catch (MalformedURLException ex)
        {
            // Try to create a valid URL using the base URL
            // contained in the "cd" command service.
            String baseURL = "";

            try
            {
                // Get a reference to the "cd" command service.
                ServiceReference ref = m_context.getServiceReference(
                    org.apache.osgi.service.shell.CdCommand.class.getName());

                if (ref != null)
                {
                    CdCommand cd = (CdCommand) m_context.getService(ref);
                    baseURL = cd.getBaseURL();
                    baseURL = (baseURL == null) ? "" : baseURL;
                    m_context.ungetService(ref);
                }

                String theURL = baseURL + location;
                new URL(theURL);

            }
            catch (Exception ex2)
            {
                return null;
            }
            location = baseURL + location;
        }
        return location;
    }
}