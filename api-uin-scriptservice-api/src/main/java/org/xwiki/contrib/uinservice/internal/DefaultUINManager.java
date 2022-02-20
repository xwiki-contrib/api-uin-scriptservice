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
package org.xwiki.contrib.uinservice.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.uinservice.DuplicateKeyException;
import org.xwiki.contrib.uinservice.UINManager;
import org.xwiki.contrib.uinservice.internal.listeners.DefaultUINManagerConfigClassInitializer;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The default UIN manager, delegating everything to the actually selected manager.
 *
 * @version $Id$
 * @since 2.2
 */
@Component
@Named(DefaultUINManager.NAME)
@Singleton
public class DefaultUINManager extends AbstractUINManager implements UINManager
{
    static final String NAME = "default";

    @Inject
    private Logger logger;

    @Inject
    private ComponentManager cm;

    private Map<String, UINManager> uinManagers;

    @Override
    public void createConfig(String name, long currentUIN, long increment, String token)
        throws DuplicateKeyException, XWikiException
    {
        getManager().createConfig(name, currentUIN, increment, token);
    }

    @Override
    public synchronized void updateConfig(String previousName, String newName, long currentUIN, long newUIN,
        long increment, String token) throws Exception
    {
        getManager().updateConfig(previousName, newName, currentUIN, newUIN, increment, token);
    }

    @Override
    public synchronized void removeConfig(String name) throws XWikiException
    {
        getManager().removeConfig(name);
    }

    @Override
    public synchronized long getNext() throws Exception
    {
        return getManager().getNext();
    }

    @Override
    public synchronized long getNext(String name) throws Exception
    {
        return getManager().getNext(name);
    }

    @Override
    public synchronized long getNext(String name, String server, String clientId, Long id, boolean simulate)
        throws Exception
    {
        return getManager().getNext(name, server, clientId, id, simulate);
    }

    @Override
    public boolean isTokenValid(String name, String token) throws Exception
    {
        return getManager().isTokenValid(name, token);
    }

    /**
     * The list of available ways to create unique numbers.
     *
     * The "strategy name" is actually the name of the corresponding component,
     * and might need translations to be user friendly.
     * @return a list of strategy names, never null
     */
    public List<String> getAvailableStrategies()
    {
        List<String> names = new ArrayList<String>(getUINManagers().keySet());
        names.sort(null);
        return names;
    }

    /**
     * The strategy currently used.
     * Per default this is the one using {@link SimpleUINManager}
     * @return the strategy name, never null
     */
    public String getActiveStrategy()
    {
        String active = null;

        try {
            BaseObject configObject = loadConfigObject(false);
            if (configObject != null) {
                active = configObject
                    .getStringValue(DefaultUINManagerConfigClassInitializer.SELECTED_STRATEGY);
            }
        } catch (XWikiException e) {
            logger.warn("could not load config; reason " + ExceptionUtils.getRootCauseMessage(e));
        }
        if (StringUtils.isEmpty(active)) {
            active = SimpleUINManager.NAME;
        }
        return active;
    }

    /**
     * Set the strategy to be used/
     * The input value is not validated, so if there is not component for this strategy,
     * the UIN generation will stop working.
     * @param name the name for the strategy to be used
     * @throws XWikiException if setting the new name fails
     */
    public void setActiveStrategy(String name) throws XWikiException
    {
        BaseObject configObject = loadConfigObject(true);
        configObject.setStringValue(DefaultUINManagerConfigClassInitializer.SELECTED_STRATEGY, name);
        saveDocWithoutHistory(configObject.getOwnerDocument());
    }

    private UINManager getManager()
    {
        final String name = getActiveStrategy();
        UINManager uinManager = getUINManagers().get(name);
        if (uinManager == null) {
            throw new IllegalStateException(String.format("configured uin manager [%s] not found", name));
        }
        logger.debug("delegate to UINManager class [{}]", uinManager.getClass().getName());
        return uinManager;
    }

    /**
     * (Re-)Load the UINManager implementations from the component manager.
     * When the extension is installed in the root name space, (e.g. if the REST interface needs to be used)
     * only those managers are found. Especially managers defined in wiki pages are not included.
     *
     * @see #getAvailableStrategies()
     */
    public void loadUINManagers()
    {
        // we do not synchronize, as in the worst case we do the lookup
        // multiple times at startup; not worth a synchronization lock every time
        // XXX: should we cache the managers at all?
        Map<String, UINManager> uinCollector = new HashMap<>();
        try {
            Map<String, Object> currentManagers = cm.getInstanceMap(UINManager.class);
            if (currentManagers != null) {
                for (Map.Entry<String, Object> entry : currentManagers.entrySet()) {
                    if (NAME.equals(entry.getKey())) {
                        continue;
                    }
                    Object managerInstance = entry.getValue();
                    if (managerInstance instanceof UINManager) {
                        uinCollector.put(entry.getKey(), (UINManager) entry.getValue());
                    } else {
                        logger.warn("cannot add UINManager [{}]; maybe classloader problem?",
                            managerInstance.getClass().getName());
                    }
                }

                logger.debug("found all UIN managers as [{}]", uinCollector);
            }
        } catch (ComponentLookupException e) {
            logger.error("cannot look up the UINManager components", e);
        }
        uinManagers = uinCollector;
    }

    /**
     * @return the known UINManagers by name
     */
    private Map<String, UINManager> getUINManagers()
    {
        if (uinManagers == null) {
            loadUINManagers();
        }
        return uinManagers;
    }

    private BaseObject loadConfigObject(boolean create) throws XWikiException
    {
        XWikiDocument configDoc = getConfigDoc();
        DocumentReference classReference = new DocumentReference(
            DefaultUINManagerConfigClassInitializer.CLASS_REF, getContext().getWikiReference());
        BaseObject obj = configDoc.getXObject(classReference);
        if (obj == null && create) {
            int index = configDoc.createXObject(classReference, getContext());
            obj = configDoc.getXObject(classReference, index);
        }
        return obj;
    }

}
