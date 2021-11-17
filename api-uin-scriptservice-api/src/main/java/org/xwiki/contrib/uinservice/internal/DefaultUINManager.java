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

import java.util.ConcurrentModificationException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.uinservice.DuplicateKeyException;
import org.xwiki.contrib.uinservice.UINConfiguration;
import org.xwiki.contrib.uinservice.UINManager;
import org.xwiki.model.reference.DocumentReference;
import com.google.common.base.Objects;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @since 2.2
 */
@Component
@Singleton
public class DefaultUINManager implements UINManager
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void createConfig(String name, long currentUIN, long increment) throws DuplicateKeyException, XWikiException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        DocumentReference configRef =
            new DocumentReference(xcontext.getWikiId(), UINConfiguration.CONFIG_SPACE, UINConfiguration.CONFIG_DOCNAME);

        XWikiDocument configDoc = xwiki.getDocument(configRef, xcontext);
        if (isUnique(name)) {
            BaseObject configObj = configDoc.newXObject(UINConfiguration.CONFIGCLASS_REF, xcontextProvider.get());
            configObj.setStringValue(UINConfiguration.NAME_PROPERTY, name);
            configObj.setLongValue(UINConfiguration.CURRENT_UIN_PROPERTY, currentUIN);
            configObj.setLongValue(UINConfiguration.INCREMENT_PROPERTY, increment);
            xwiki.saveDocument(configDoc, String.format("New UIN added: ", name), xcontext);
        }

    }

    @Override
    public synchronized void updateConfig(String previousName, String newName, long currentUIN, long newUIN,
        long increment) throws DuplicateKeyException, XWikiException
    {
        XWikiContext xcontext = xcontextProvider.get();
        UINConfiguration config = new UINConfiguration(xcontext, previousName);

        if (!Objects.equal(currentUIN, newUIN)) {
            if (!Objects.equal(currentUIN, config.getCurrentUIN())) {
                throw new ConcurrentModificationException(String.format(
                    "Concurrent modification of UIN detected, cannot proceed with update [{}] [{}] [{}] [{}]", config,
                    currentUIN, newUIN, increment));
            }
            config.setCurrentUIN(newUIN);
        }

        config.setIncrement(increment);

        if (Objects.equal(previousName, newName) || (!Objects.equal(previousName, newName) && isUnique(newName))) {
            config.setName(newName);
            config.save(String.format("Configuration updated for UIN [{}] Starts at [{}], increment by [{}]", newName,
                config.getCurrentUIN(), increment));
        } else {
            throw new DuplicateKeyException(
                String.format("A UIN with the name [{}] already exists. Please choose another name.", newName));
        }
    }

    @Override
    public synchronized void removeConfig(String name) throws XWikiException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        XWikiDocument configDoc = getConfigDoc();

        for (BaseObject configObj : configDoc.getXObjects(UINConfiguration.CONFIGCLASS_REF)) {
            if (configObj != null) {
                String uinName = configObj.getStringValue(UINConfiguration.NAME_PROPERTY);
                if (Objects.equal(uinName, name)) {
                    configDoc.removeXObject(configObj);
                    xwiki.saveDocument(configDoc, String.format("Removal of UIN [{}]", name), xcontext);
                }
            }
        }

    }

    private XWikiDocument getConfigDoc() throws XWikiException
    {
        XWikiContext xcontext = xcontextProvider.get();
        DocumentReference configRef =
            new DocumentReference(xcontext.getWikiId(), UINConfiguration.CONFIG_SPACE, UINConfiguration.CONFIG_DOCNAME);
        return xcontext.getWiki().getDocument(configRef, xcontext);
    }

    private boolean isUnique(String name) throws XWikiException, DuplicateKeyException
    {
        XWikiDocument configDoc = getConfigDoc();
        if (configDoc != null) {
            for (BaseObject configObj : configDoc.getXObjects(UINConfiguration.CONFIGCLASS_REF)) {
                if (configObj != null) {
                    String uinName = configObj.getStringValue(UINConfiguration.NAME_PROPERTY);
                    if (Objects.equal(uinName, name)) {
                        return false;
                    }
                }
            }
        }
        throw new DuplicateKeyException(
            String.format("A UIN with the name [%s] already exists. Please choose another name.", name));
    }

    @Override
    public synchronized long getNext() throws XWikiException
    {
        return new UINConfiguration(xcontextProvider.get()).incrementCurrentUIN();
    }

    @Override
    public synchronized long getNext(String name) throws XWikiException
    {
        return new UINConfiguration(xcontextProvider.get(), name).incrementCurrentUIN();
    }
}
