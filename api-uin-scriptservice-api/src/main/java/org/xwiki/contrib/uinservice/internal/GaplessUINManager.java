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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
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
    private static final String QUERY_BY_NAME_PREFIX = "from doc.object("
        + GaplessUINSequenceClassInitializer.CONFIG_CLASS_FULLNAME
        + ") as sequence where doc.space = :space ";
    private static final String QUERY_NAME_PARAM = "space";

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queries;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> refToString;

    @Inject
    @Named("default")
    private DocumentReferenceResolver<String> stringToRef;

    @Inject
    @Named("default")
    private EntityReferenceProvider defaultProvider;

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
        return getNext("generic");
    }

    @Override
    public long getNext(String name) throws Exception
    {
        UINResult next = getNext(name, null, null, null, false, false);
        if (next.isError()) {
            throw new IllegalArgumentException(next.getErrorMesssage());
        }
        return next.getUin();
    }

    @Override
    public synchronized UINResult getNext(String name, String server, String client, Long id,
        boolean force, boolean simulate)
        throws Exception
    {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalStateException(
                String.format("name [%s] should not be empty", name));
        }
        String clientId = (client == null) ? "" : client;

        // using force to book the id works completely different
        if (force && id != null) {
            return forceUseOfIdInSequence(name, id, client, server, simulate);
        }

        UINResult result = null;
        if (id == null) {
            result = createNewIdIfAvailable(name, clientId, server, simulate, force);
        } else {
            result = checkIdMatches(name, clientId, id);
        }
        return result;
    }

    private UINResult createNewIdIfAvailable(String name, String clientId,
        String server, boolean force, boolean simulate) throws QueryException, XWikiException
    {
        UINResult result;
        // no id given; return existing id if present
        BaseObject sequence = loadSequence(name, clientId);

        if (sequence != null) {
            long newId = sequence.getLongValue(GaplessUINSequenceClassInitializer.PROPERTY_UIN);
            result = new UINResult(newId, null);
        } else {
            // otherwise create new id
            result = createNextInSequence(name, server, clientId, force, simulate);
        }

        return result;
    }

    @Override
    public List<? extends Object> getUIDsForName(String name) throws QueryException
    {
        List<Map<? extends Object, ? extends Object>> result = new ArrayList<>();

        XWikiContext context = getContext();
        SpaceReference dataSpaceRef = new SpaceReference(context.getWikiId(), DATA_SPACE_NAME, name);

        String hqlQuery = "select uin.value, clientid.value, server.value"
            + " from BaseObject obj, XWikiDocument doc, LongProperty uin,"
            + " StringProperty clientid, StringProperty server where doc.space = :dataSpace2"
            + " and doc.fullName = obj.name and obj.className = :className"
            + " and obj.id = uin.id.id and uin.id.name = :uinName"
            + " and obj.id = clientid.id.id and clientid.id.name = :clientidName"
            + " and obj.id = server.id.id and server.id.name = :serverName"
            + " order by uin.value";

        Query query = queries.createQuery(hqlQuery, Query.HQL)
            .bindValue("dataSpace2", refToString.serialize(dataSpaceRef))
            .bindValue("className", GaplessUINSequenceClassInitializer.CONFIG_CLASS_FULLNAME)
            .bindValue("uinName", GaplessUINSequenceClassInitializer.PROPERTY_UIN)
            .bindValue("clientidName", GaplessUINSequenceClassInitializer.PROPERTY_CLIENT)
            .bindValue("serverName", GaplessUINSequenceClassInitializer.PROPERTY_SERVER);

        for (Object res : query.execute()) {
            Object[] results = (Object[]) res;
            Map<String, Object> data = new HashMap<>();
            data.put("uin", (Long) results[0]);
            data.put("clientId", results[1]);
            data.put("server", results[2]);

            result.add(data);
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

        // load data from document UIN.$(name).$(id) with matching client id
        String queryStr = QUERY_BY_NAME_PREFIX + "and sequence."
            + GaplessUINSequenceClassInitializer.PROPERTY_CLIENT + " = :client";
        SpaceReference dataSpaceRef = new SpaceReference(context.getWikiId(), DATA_SPACE_NAME, name);

        Query query = queries.createQuery(queryStr, Query.XWQL)
            .bindValue(QUERY_NAME_PARAM, refToString.serialize(dataSpaceRef))
            .bindValue("client", clientId);

        return singleResult(query);
    }

    /**
     * Same as {@link GaplessUINManager#loadSequence(String, String)} but for the given id.
     * @param name the name of the sequence
     * @param id the id in the sequence
     * @return the corresponding object, or null of not found
     */
    private BaseObject loadSequenceEntry(String name, long id) throws QueryException, XWikiException
    {
        logger.debug("load sequence for name [{}] and id [{}]", name, id);

        XWikiContext context = getContext();

        // load data from document UIN.$(name).% with matching id
        String queryStr = QUERY_BY_NAME_PREFIX + " and sequence."
            + GaplessUINSequenceClassInitializer.PROPERTY_UIN + " = :id";
        SpaceReference dataSpaceRef = new SpaceReference(context.getWikiId(), DATA_SPACE_NAME, name);

        Query query = queries.createQuery(queryStr, Query.XWQL)
            .bindValue(QUERY_NAME_PARAM, refToString.serialize(dataSpaceRef))
            .bindValue("id", id);

        return singleResult(query);
    }

    private BaseObject singleResult(Query query) throws QueryException, XWikiException
    {
        List<Object> results = query.setLimit(1).execute();

        BaseObject sequence;
        if (results.isEmpty()) {
            sequence = null;
        } else {
            XWikiContext context = getContext();
            String docFullName = (String) results.get(0);
            DocumentReference docRef = stringToRef.resolve(docFullName);
            XWikiDocument sequenceDoc = context.getWiki().getDocument(docRef, context);
            sequence = sequenceDoc.getXObject(GaplessUINSequenceClassInitializer.CLASS_REF);
        }

        return sequence;
    }

    private UINResult checkIdMatches(String name, String client, long id) throws QueryException, XWikiException
    {
        // check if expected sequence id matches
        BaseObject sequence = loadSequence(name, client);
        if (sequence == null) {
            throw new IllegalStateException(
                String.format("no sequence for name [%s] and client [%s] found", name, client));
        }
        long newId = sequence.getLongValue(GaplessUINSequenceClassInitializer.PROPERTY_UIN);
        if (newId != id) {
            throw new IllegalStateException(
                String.format("the id [%d] differs from the expected id [%d]", newId, id));
        }
        return new UINResult(id, null);
    }

    /**
     * Create a new uid for the given parameters.
     *
     * @param name the sequence name
     * @param server the server sending the request
     * @param clientId the client id
     * @param simulate if true, do not save the new id
     * @return the next id, might be an error if there is no sequence for the given name
     * @throws QueryException
     * @throws XWikiException
     */
    private UINResult createNextInSequence(String name, String server, String clientId, boolean force, boolean simulate)
        throws QueryException, XWikiException
    {
        List<Object> results = loadAllUINsForName(name);
        Long nextId = null;
        String error = null;
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

        if (needsCreate) {
            error = createNewSequence(name, simulate, force);
        }
        if (!simulate) {
            createSequenceObject(name, server, clientId, nextId);
        }

        return new UINResult(nextId, error);
    }

    private String createNewSequence(String name, boolean simulate, boolean force) throws XWikiException
    {
        BaseObject config = loadConfig();
        String message = null;
        boolean cannotCreate = config.getIntValue(GaplessUINManagerConfigClassInitializer.PROPERTY_AUTOCREATE) == 0;
        if (cannotCreate) {
            if (force) {
                message = String.format("use force to create new sequence with name [%s]", name);
            } else {
                throw new IllegalStateException(
                    String.format("cannot create new sequence for name [%s]", name));
            }
        }
        if (!simulate) {
            createSequenceHomePage(name);
        }
        return message;
    }

    /**
     * Force using the given id in the sequence for the given client and server.
     * If simulate, return the proper value but do not change anything.
     * @param name the sequence name
     * @param id the id to be used
     * @param client the client forcing the id, not null
     * @param server optional information about the server
     * @param simulate do not really change anything
     * @return the result of the operation
     * @throws QueryException
     * @throws XWikiException
     */
    private UINResult forceUseOfIdInSequence(String name, long id, String client, String server,
        boolean simulate) throws QueryException, XWikiException
    {
        UINResult result;
        BaseObject uinObjectForClient = loadSequence(name, client);
        if (uinObjectForClient != null
            && id == uinObjectForClient.getLongValue(GaplessUINSequenceClassInitializer.PROPERTY_UIN)) {
            // all is as wanted; just return
            // Note: we do not update the server here. Maybe we should?
            result = new UINResult(id, null);
        } else {
            BaseObject uinObjectForId = loadSequenceEntry(name, id);
            String error = null;

            if (!simulate && uinObjectForClient != null) {
                long oldId = uinObjectForClient.getLongValue(GaplessUINSequenceClassInitializer.PROPERTY_UIN);
                deleteSequenceObject(uinObjectForClient);
                logger.info("Client [{}] used force to delete its own sequence [{}] on [{}]", client, oldId, name);
            }
            if (uinObjectForId == null) {
                // check if we need to create a sequence home page
                List<Object> results = loadAllUINsForName(name);
                if (results.isEmpty()) {
                    error = createNewSequence(name, simulate, true);
                }
                if (!simulate) {
                    createSequenceObject(name, server, client, id);
                    logger.info("Client [{}] used force to create a new sequence [{}] on [{}]", client, id, name);
                }
            } else {
                String originalClient = updateClientInSequenceObject(uinObjectForId, client, simulate);
                if (!client.equals(originalClient)) {
                    error = String.format("unregistered uin [%d] for client [%s]", id, originalClient);
                    logger.warn("Client [{}] used force on sequence [{}] and {}", client, name, error);
                }
            }
            result = new UINResult(id, error);
        }

        return result;
    }

    // separate helper method to silence checkstyle
    private String updateClientInSequenceObject(BaseObject uinObjectForId, String client, boolean simulate)
        throws XWikiException
    {
        // first check if we are not another id for the same client
        // then overwrite the client
        String originalClient = uinObjectForId
            .getStringValue(GaplessUINSequenceClassInitializer.PROPERTY_CLIENT);
        if (!client.equals(originalClient) && !simulate) {
            uinObjectForId.setStringValue(GaplessUINSequenceClassInitializer.PROPERTY_CLIENT, client);
            saveSequenceObject(uinObjectForId);
        }

        return originalClient;
    }

    private void saveSequenceObject(BaseObject uinObjectForId) throws XWikiException
    {
        XWikiContext context = getContext();
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = uinObjectForId.getOwnerDocument();
        xwiki.saveDocument(doc, "overwrite client", true, context);
    }

    private void deleteSequenceObject(BaseObject uinObject) throws XWikiException
    {
        XWikiContext context = getContext();
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = uinObject.getOwnerDocument();
        xwiki.deleteDocument(doc, context);
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

    private void createSequenceHomePage(String name) throws XWikiException
    {
        XWikiContext context = getContext();
        XWiki wiki = context.getWiki();
        EntityReference defaultRef = defaultProvider.getDefaultReference(EntityType.DOCUMENT);

        SpaceReference dataSpaceRef = new SpaceReference(context.getWikiId(), DATA_SPACE_NAME, name);
        DocumentReference newSequenceHomeRef = new DocumentReference(defaultRef.getName(), dataSpaceRef);
        XWikiDocument newSequenceHomeDoc = wiki.getDocument(newSequenceHomeRef, context);
        EntityReference sheetClassRef = new LocalDocumentReference("XWiki", "DocumentSheetBinding");
        int index = newSequenceHomeDoc.createXObject(sheetClassRef, context);
        BaseObject sequence = newSequenceHomeDoc.getXObject(sheetClassRef, index);
        sequence.setStringValue("sheet", "UINCode.GaplessUINSheet");
        newSequenceHomeDoc.setHidden(true);
        wiki.saveDocument(newSequenceHomeDoc, context);
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
