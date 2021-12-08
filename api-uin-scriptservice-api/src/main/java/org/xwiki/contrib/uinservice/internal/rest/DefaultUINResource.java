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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.uinservice.UINManager;
import org.xwiki.contrib.uinservice.rest.UINResource;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;

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
    @Inject
    private UINManager manager;

    @Override
    public String getUIN(String xwikiName, String spaceName, String pageName, String name, String token)
        throws XWikiRestException
    {
        try {
            if (manager.isTokenValid(name, token)) {
                return String.format("{\"uin\": %s}", manager.getNext(name));
            }
        } catch (Exception e) {
            throw new XWikiRestException(e.getMessage());
        }
        throw new XWikiRestException("Failed to get UIN!");
    }
}
