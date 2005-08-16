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
package org.apache.osgi.moduleloader;

import java.util.EventListener;

/**
 * <p>
 * This interface is an event listener for <tt>ModuleEvent</tt> events.
 * To receive events, an implementation of this listener must be added
 * to the <tt>ModuleManager</tt> instance.
 * </p>
 * @see org.apache.osgi.moduleloader.ModuleManager
 * @see org.apache.osgi.moduleloader.ModuleEvent
**/
public interface ModuleListener extends EventListener
{
    /**
     * <p>
     * This method is called after a module is added to the
     * <tt>ModuleManager</tt>.
     * </p>
     * @param event the event object containing the event details.
    **/
    public void moduleAdded(ModuleEvent event);

    /**
     * <p>
     * This method is called after a module has been reset by the
     * <tt>ModuleManager</tt>.
     * </p>
     * @param event the event object containing the event details.
    **/
    public void moduleReset(ModuleEvent event);

    /**
     * <p>
     * This method is called after a module is remove from the
     * <tt>ModuleManager</tt>.
     * </p>
     * @param event the event object containing the event details.
    **/
    public void moduleRemoved(ModuleEvent event);
}