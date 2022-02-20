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
    private static final String RESULT_KEY_UIN = "uin";

    private static final String RESULT_KEY_ERROR = "error";

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
    public Object getUIN(String xwikiName, String spaceName, String pageName, String name, String token,
        String clientId, String server, String idStr, String simulate)
        throws XWikiRestException
    {
        logger.trace("getUIN called with name [{}], clientid [{}], server [{}], id [{}], simulate [{}]",
            name, clientId, server, idStr, simulate);
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
                long newId = manager.getNext(name, server, clientId, givenId, StringUtils.isNotEmpty(simulate));
                result.put(RESULT_KEY_UIN, newId);
            } else {
                result.put(RESULT_KEY_ERROR, String.format("Invalid token for configuration name [%s]", name));
            }
        } catch (IllegalStateException ie) {
            logger.info("send client error [{}]", ie.getMessage());
            result.put(RESULT_KEY_ERROR, ie.getMessage());
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
}
