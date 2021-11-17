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
package org.xwiki.contrib.uinservice;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.google.common.base.Objects;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * A UIN configuration instance.
 *
 * @version $Id$
 * @since 2.2
 */
public class UINConfiguration
{
    /**
     * Code space.
     */
    public static final String CONFIG_SPACE = "UINCode";

    /**
     * Configuration document.
     */
    public static final String CONFIG_DOCNAME = "UINConfig";

    /**
     * Configuration class name.
     */
    public static final String CONFIG_CLASSNAME = "UINConfigClass";

    /**
     * Current UIN property name.
     */
    public static final String CURRENT_UIN_PROPERTY = "currentUIN";

    /**
     * Incremental step property name.
     */
    public static final String INCREMENT_PROPERTY = "increment";

    /**
     * Configuration name.
     */
    public static final String NAME_PROPERTY = "name";

    /**
     * Configuration class reference.
     */
    public static final LocalDocumentReference CONFIGCLASS_REF =
        new LocalDocumentReference(CONFIG_SPACE, CONFIG_CLASSNAME);

    /**
     * Logging tool.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UINConfiguration.class);

    private XWikiContext xcontext;

    private XWikiDocument configDoc;

    private BaseObject configObj;

    /**
     * @param xcontext the current context
     */
    public UINConfiguration(XWikiContext xcontext)
    {
        this(xcontext, "");
    }

    /**
     * @param xcontext the current context
     * @param name the configuration name
     */
    public UINConfiguration(XWikiContext xcontext, String name)
    {
        this.xcontext = xcontext;
        DocumentReference configRef = new DocumentReference(xcontext.getWikiId(), CONFIG_SPACE, CONFIG_DOCNAME);
        try {
            this.configDoc = xcontext.getWiki().getDocument(configRef, xcontext);
            if (this.configDoc.isNew()) {
                throw new RuntimeException("UIN configuration cannot be found");
            }
            for (BaseObject obj : this.configDoc.getXObjects(CONFIGCLASS_REF)) {
                if (obj != null) {
                    String uinName = obj.getStringValue(NAME_PROPERTY);
                    if (Objects.equal(uinName, name)) {
                        this.configObj = obj;
                    }
                }
            }
            if (this.configObj == null) {
                throw new RuntimeException(
                    String.format("UIN configuration object for name [{}] cannot be found.", name));
            }
        } catch (XWikiException e) {
            LOGGER.error("Failed to get configuration document for reference [{}]", configRef, e);
        }
    }

    /**
     * @return the current UIN
     */
    public long getCurrentUIN()
    {
        return this.configObj.getLongValue(CURRENT_UIN_PROPERTY);
    }

    /**
     * @param uin the new UIN
     */
    public void setCurrentUIN(long uin)
    {
        this.configObj.setLongValue(CURRENT_UIN_PROPERTY, uin);
    }

    /**
     * @return the incremental step
     */
    public long getIncrement()
    {
        return this.configObj.getLongValue(INCREMENT_PROPERTY);
    }

    /**
     * @param increment the incremental step
     */
    public void setIncrement(long increment)
    {
        this.configObj.setLongValue(INCREMENT_PROPERTY, increment);
    }

    /**
     * @return the configuration name
     */
    public String getName()
    {
        return this.configObj.getStringValue(NAME_PROPERTY);
    }

    /**
     * @param name the configuration name
     */
    public void setName(String name)
    {
        this.configObj.setStringValue(NAME_PROPERTY, name);
    }

    /**
     * @return the new UIN
     * @throws XWikiException in case of exceptions
     */
    public long incrementCurrentUIN() throws XWikiException
    {
        long currentUIN = getCurrentUIN() + getIncrement();
        this.configObj.setLongValue(CURRENT_UIN_PROPERTY, currentUIN);
        saveWithoutHistory(this.configDoc, this.xcontext);
        return currentUIN;
    }

    private void saveWithoutHistory(XWikiDocument configDoc, XWikiContext xcontext) throws XWikiException
    {
        this.configDoc.setContentUpdateDate(new Date());
        this.configDoc.setMetaDataDirty(false);
        this.configDoc.setContentDirty(false);
        this.xcontext.getWiki().getStore().saveXWikiDoc(this.configDoc, this.xcontext);

    }

    /**
     * @param comment the save comment
     * @throws XWikiException in case of exceptions
     */
    public void save(String comment) throws XWikiException
    {
        this.xcontext.getWiki().saveDocument(this.configDoc, this.xcontext);
    }
}
