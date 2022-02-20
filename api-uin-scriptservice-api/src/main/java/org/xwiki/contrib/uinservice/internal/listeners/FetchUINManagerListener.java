/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.uinservice.internal.listeners;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.uinservice.UINManager;
import org.xwiki.contrib.uinservice.internal.DefaultUINManager;
import org.xwiki.extension.event.ExtensionInitializedEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * When the wiki starts or an extension is (re-)installed, fetch the available {@link UINManager} components.
 *
 * @version $Id$
 * @since 2.3
 */
@Component
@Named(FetchUINManagerListener.NAME)
@Singleton
public class FetchUINManagerListener implements EventListener
{
    static final String NAME = "org.xwiki.contrib.uinservice.internal.FetchUINManagerListener";

    @Inject
    private Logger logger;

    @Inject
    @Named("default")
    private UINManager uinManager;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.asList(
            new ApplicationReadyEvent(),
            new ExtensionInitializedEvent(),
            new ExtensionUpgradedEvent());
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        logger.info("got event [{}]); reload UINManagers", event);
        ((DefaultUINManager) uinManager).loadUINManagers();
    }

}
