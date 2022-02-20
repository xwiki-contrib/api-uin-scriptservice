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

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.uinservice.DuplicateKeyException;
import org.xwiki.contrib.uinservice.UINManager;
import org.xwiki.contrib.uinservice.internal.listeners.GaplessUINManagerConfigClassInitializer;
import org.xwiki.contrib.uinservice.internal.listeners.GaplessUINSequenceClassInitializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Create UINs in a gap-less manner, if a clientId is provided.
 *
 * @version $Id$
 * @since 2.3
 */
@Component
@Named(GaplessUINManager.NAME)
@Singleton
public class GaplessUINManager extends AbstractUINManager implements UINManager
{
    static final String NAME = "org.xwiki.contrib.uinservice.GaplessUINManager";
    private static final String DATA_SPACE_NAME = "UIN";

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queries;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> refToString;

    @Override
    public void createConfig(String name, long currentUIN, long increment, String token)
        throws DuplicateKeyException, XWikiException
    {
        throw new UnsupportedOperationException("createConfig");
    }

    @Override
    public void updateConfig(String previousName, String newName, long currentUIN, long newUIN,
        long increment, String token) throws DuplicateKeyException, Exception
    {
        throw new UnsupportedOperationException("updateConfig");
    }

    @Override
    public void removeConfig(String name) throws XWikiException
    {
        throw new UnsupportedOperationException("removeConfig");
    }

    @Override
    public boolean isTokenValid(String name, String token) throws Exception
    {
        BaseObject config = loadConfig();
        String storedToken = config.getStringValue(GaplessUINManagerConfigClassInitializer.PROPERTY_TOKEN);
        return (StringUtils.isEmpty(storedToken) || Objects.equals(token, storedToken));
    }

    @Override
    public long getNext() throws Exception
    {
        return getNext("");
    }

    @Override
    public long getNext(String name) throws Exception
    {
        return getNext(name, null, null, null, false);
    }

    @Override
    public synchronized long getNext(String name, String server, String clientId, Long id, boolean simulate)
        throws Exception
    {
        long result = -1;
        BaseObject sequence = loadSequence(name, clientId);
        if (id != null) {
            // check if expected sequence id matches
            if (sequence == null) {
                throw new IllegalStateException(
                    String.format("no sequence for name [%s] abd client [%s] found", name, clientId));
            }
            long actualId = sequence.getLongValue(GaplessUINSequenceClassInitializer.PROPERTY_UIN);
            if (actualId != id) {
                throw new IllegalStateException(
                    String.format("the id [%d] differs from the expected id [%d]", actualId, id));
            }
            result = actualId;
        } else {
            // no id given; return existing id if present
            if (sequence != null) {
                result = sequence.getLongValue(GaplessUINSequenceClassInitializer.PROPERTY_UIN);
            } else {
                // otherwise create new id
                Long next = createNextInSequence(name, server, clientId, simulate);
                if (next == null) {
                    throw new IllegalStateException(
                        String.format("sequence [%s] for client [%s] not found", name, clientId));
                } else {
                    result = next;
                }
            }
        }
        return result;
    }

    /**
     * load existing uin object for the given sequence name and client id.
     *
     * @return null if not found
     */
    private BaseObject loadSequence(String name, String clientId) throws QueryException, XWikiException
    {
        logger.debug("load sequence for name [{}] and client [{}]", name, clientId);

        XWikiContext context = getContext();
        DocumentReference dataClassRef = new DocumentReference(GaplessUINSequenceClassInitializer.CLASS_REF,
            context.getWikiReference());

        // load data from document UIN.$(name).$(id) with matching client id
        String queryStr = "from doc.object(" + GaplessUINSequenceClassInitializer.CONFIG_CLASS_FULLNAME
            + ") as sequence where doc.space = :space and sequence."
            + GaplessUINSequenceClassInitializer.PROPERTY_CLIENT + " = :client";
        SpaceReference dataSpaceRef = new SpaceReference(context.getWikiId(), DATA_SPACE_NAME, name);

        Query query = queries.createQuery(queryStr, Query.XWQL)
            .bindValue("space", refToString.serialize(dataSpaceRef))
            .bindValue("client", clientId)
            .setLimit(1);

        List<Object> results = query.execute();

        BaseObject sequence;
        if (results.isEmpty()) {
            sequence = null;
        } else {
            String docFullName = (String) results.get(0);
            XWikiDocument sequenceDoc = context.getWiki().getDocument(docFullName, context);
            sequence = sequenceDoc.getXObject(dataClassRef);
        }

        return sequence;
    }

    /**
     * Create a new uid for the given parameters.
     *
     * @param name the sequence name
     * @param server the server sending the request
     * @param clientId the client id
     * @param simulate if true, do not save the new id
     * @return the next id, might be null if there is no sequence for the given name
     * @throws QueryException
     * @throws XWikiException
     */
    private Long createNextInSequence(String name, String server, String clientId, boolean simulate)
        throws QueryException, XWikiException
    {
        List<Object> results = loadAllUINsForName(name);

        Long nextId = null;
        boolean needsCreate;
        if (results.isEmpty()) {
            nextId = 1L;
            needsCreate = true;
            logger.debug("no value found for sequence [{}]", name);
        } else {
            // find first gap
            Long previousUin = null;
            for (Object result : results) {
                Long currentUin = (Long) result;
                if (previousUin != null && currentUin > previousUin + 1L) {
                    break;
                }
                previousUin = currentUin;
            }
            // now we either have found a gap, and previousUin is the last number before the gap
            // or we finished the loop without break, and previousUin is the highest number used so far
            logger.debug("first gap of [{}] found after [{}]", name, previousUin);
            nextId = previousUin + 1;
            needsCreate = false;
        }

        if (!simulate) {
            if (needsCreate) {
                BaseObject config = loadConfig();
                if (config.getIntValue(GaplessUINManagerConfigClassInitializer.PROPERTY_AUTOCREATE) == 0) {
                    throw new IllegalStateException(
                        String.format("cannot create new sequence for name [%s]", name));
                }
            }
            createSequenceObject(name, server, clientId, nextId);
        }

        return nextId;
    }

    private List<Object> loadAllUINsForName(String name) throws QueryException
    {
        XWikiContext context = getContext();

        String queryStr = "select sequence." + GaplessUINSequenceClassInitializer.PROPERTY_UIN
            + " from Document doc, doc.object(" + GaplessUINSequenceClassInitializer.CONFIG_CLASS_FULLNAME
            + ") as sequence where doc.space = :dataSpace order by sequence."
            + GaplessUINSequenceClassInitializer.PROPERTY_UIN + " asc";
        SpaceReference dataSpaceRef = new SpaceReference(context.getWikiId(), DATA_SPACE_NAME, name);

        Query query = queries.createQuery(queryStr, Query.XWQL)
            .bindValue("dataSpace", refToString.serialize(dataSpaceRef));

        return query.execute();
    }

    private void createSequenceObject(String name, String server, String clientId, Long nextId) throws XWikiException
    {
        XWikiContext context = getContext();
        XWiki wiki = context.getWiki();
        DocumentReference dataClassRef = new DocumentReference(GaplessUINSequenceClassInitializer.CLASS_REF,
            context.getWikiReference());

        SpaceReference dataSpaceRef = new SpaceReference(context.getWikiId(), DATA_SPACE_NAME, name);
        DocumentReference newSequenceDocRef = new DocumentReference(nameForId(nextId), dataSpaceRef);
        XWikiDocument newSequenceDoc = wiki.getDocument(newSequenceDocRef, context);
        int index = newSequenceDoc.createXObject(dataClassRef, context);
        BaseObject sequence = newSequenceDoc.getXObject(dataClassRef, index);
        sequence.setLongValue(GaplessUINSequenceClassInitializer.PROPERTY_UIN, nextId);
        sequence.setStringValue(GaplessUINSequenceClassInitializer.PROPERTY_CLIENT, clientId);
        sequence.setStringValue(GaplessUINSequenceClassInitializer.PROPERTY_SERVER, server);
        newSequenceDoc.setHidden(true);
        wiki.saveDocument(newSequenceDoc, context);
    }

    private static String nameForId(long id)
    {
        // XXX: arbitrary format, maybe make this configurable
        return String.format("%05d", id);
    }

    private BaseObject loadConfig() throws XWikiException
    {
        XWikiContext context = getContext();
        XWikiDocument configDoc = getConfigDoc();
        DocumentReference classReference = new DocumentReference(
            GaplessUINManagerConfigClassInitializer.CLASS_REF, context.getWikiReference());
        BaseObject config = configDoc.getXObject(classReference);
        if (config == null) {
            int index = configDoc.createXObject(classReference, context);
            config = configDoc.getXObject(classReference, index);
        }
        return config;
    }
}
