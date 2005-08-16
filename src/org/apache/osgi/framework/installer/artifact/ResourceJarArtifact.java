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
package org.apache.osgi.framework.installer.artifact;

import java.io.IOException;
import java.io.InputStream;

import org.apache.osgi.framework.installer.Status;
import org.apache.osgi.framework.installer.StringProperty;
import org.apache.osgi.framework.installer.resource.ResourceLoader;

public class ResourceJarArtifact extends AbstractJarArtifact
{
    public ResourceJarArtifact(StringProperty sourceName)
    {
        this(sourceName, null);
    }

    public ResourceJarArtifact(StringProperty sourceName, StringProperty destDir)
    {
        this(sourceName, destDir, false);
    }

    public ResourceJarArtifact(
        StringProperty sourceName, StringProperty destDir, boolean localize)
    {
        super(sourceName, destDir, localize);
    }

    public InputStream getInputStream(Status status)
        throws IOException
    {
        return ResourceLoader.getResourceAsStream(getSourceName().getStringValue());
    }

    public String toString()
    {
        return "RESOURCE JAR: " + getSourceName().getStringValue();
    }
}