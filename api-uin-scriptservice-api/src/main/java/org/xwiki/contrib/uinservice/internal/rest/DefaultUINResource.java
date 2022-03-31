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
package org.xwiki.contrib.uinservice.internal.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.uinservice.UINManager;
import org.xwiki.contrib.uinservice.rest.UINResource;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Default implementation of {@link UINResource}.
 *
 * @version $Id$
 * @since 2.2
 */
@Component
@Named("org.xwiki.contrib.uinservice.internal.rest.DefaultUINResource")
@Singleton
public class DefaultUINResource extends XWikiResource implements UINResource
{
    private static final String INVALID_TOKEN_MESSAGE = "Invalid token for configuration name [%s]";

    @Inject
    @Named("default")
    private UINManager manager;

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public Object getUIN(String xwikiName, String name, String token,
        String clientId, String server, String idStr, String force, String simulate)
        throws XWikiRestException
    {
        logger.trace("getUIN called with name [{}], clientid [{}], server [{}], id [{}], force [{}]. simulate [{}]",
            name, clientId, server, idStr, force, simulate);
        final Map<String, Object> result = new HashMap<String, Object>();
        final XWikiContext context = contextProvider.get();
        final String currentWiki = context.getWikiId();

        try {
            WikiDescriptor wiki = wikiManager.getById(xwikiName);
            if (wiki == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            context.setWikiId(wiki.getId());

            if (manager.isTokenValid(name, token)) {
                Long givenId = null;
                if (idStr != null) {
                    givenId = Long.parseLong(idStr, 10);
                }
                UINManager.UINResult newId = manager.getNext(name, server, clientId, givenId,
                    StringUtils.isNotEmpty(force),
                    StringUtils.isNotEmpty(simulate));
                result.put(UINResource.RESULT_KEY_UIN, newId.getUin());
                if (newId.isError()) {
                    result.put(UINResource.RESULT_KEY_ERROR, newId.getErrorMesssage());
                }
            } else {
                result.put(UINResource.RESULT_KEY_ERROR, String.format(INVALID_TOKEN_MESSAGE, name));
            }
        } catch (IllegalStateException ie) {
            logger.info("send client error [{}]", ie.getMessage());
            result.put(UINResource.RESULT_KEY_ERROR, ie.getMessage());
        } catch (WebApplicationException we) {
            throw we;
        } catch (Exception e) {
            logger.info("client [{}] caused error [{}]", server, ExceptionUtils.getRootCause(e).toString());
            throw new XWikiRestException(e.getMessage());
        } finally {
            context.setWikiId(currentWiki);
        }
        logger.trace("result is [{}]", result);
        return result;
    }

    @Override
    public Object getUINList(String xwikiName, String name, String token)
        throws XWikiRestException
    {
        logger.trace("getUINList called with name [{}]", name);
        final Map<String, Object> result = new HashMap<String, Object>();
        final XWikiContext context = contextProvider.get();
        final String currentWiki = context.getWikiId();

        try {
            WikiDescriptor wiki = wikiManager.getById(xwikiName);
            if (wiki == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            context.setWikiId(wiki.getId());

            if (manager.isTokenValid(name, token)) {
                List<? extends Object> uinList = manager.getUIDsForName(name);
                result.put(UINResource.RESULT_KEY_UIN_VALUES, uinList);
            } else {
                result.put(UINResource.RESULT_KEY_ERROR, String.format(INVALID_TOKEN_MESSAGE, name));
            }

        } catch (WebApplicationException we) {
            throw we;
        } catch (XWikiException xe) {
            logger.info("fetching ids for [{}] caused XWikiException [{}]",
                name, ExceptionUtils.getRootCause(xe).toString());
            result.put(UINResource.RESULT_KEY_ERROR, xe.getMessage());
        } catch (Exception e) {
            logger.info("fetching ids for [{}] caused error [{}]", name, ExceptionUtils.getRootCause(e).toString());
            throw new XWikiRestException(e.getMessage());
        } finally {
            context.setWikiId(currentWiki);
        }
        return result;
    }
}
