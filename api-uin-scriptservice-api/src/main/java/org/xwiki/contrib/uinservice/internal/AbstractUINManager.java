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

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.contrib.uinservice.UINConfiguration;
import org.xwiki.contrib.uinservice.UINManager;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A simple base class to make the token check reusable.
 *
 * @version $Id$
 * @since 2.3
 */
public abstract class AbstractUINManager implements UINManager
{

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    protected XWikiContext getContext()
    {
        return xcontextProvider.get();
    }

    protected XWikiDocument getConfigDoc() throws XWikiException
    {
        XWikiContext xcontext = getContext();
        DocumentReference configRef = new DocumentReference(xcontext.getWikiId(),
            UINConfiguration.CONFIG_SPACE, UINConfiguration.CONFIG_DOCNAME);
        return xcontext.getWiki().getDocument(configRef, xcontext);
    }

    // note: copy & paste from UINConfiguration
    // @param configDoc should be the one from getConfigDoc(), but would work on any other
    protected void saveDocWithoutHistory(XWikiDocument configDoc) throws XWikiException
    {
        XWikiContext context = getContext();
        if (configDoc.isNew()) {
            context.getWiki().saveDocument(configDoc, context);
        } else {
            configDoc.setContentUpdateDate(new Date());
            configDoc.setMetaDataDirty(false);
            configDoc.setContentDirty(false);
            context.getWiki().getStore().saveXWikiDoc(configDoc, context);
        }
    }
}
