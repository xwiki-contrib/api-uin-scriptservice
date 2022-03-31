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
package org.xwiki.contrib.uinservice.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since 2.2
 */
@Path("/wikis/{wikiName}/uin")
@Unstable
public interface UINResource
{
    /**
     * The key to store the retuned uin.
     */
    String RESULT_KEY_UIN = "uin";

    /**
     * The key to store error messages.
     */
    String RESULT_KEY_ERROR = "error";

    /**
     * The key to store values.
     * This is only used by the /uinlist endpoint yet.
     */
    String RESULT_KEY_UIN_VALUES = "values";

    /**
     * Get the next available UIN.
     * The return value has a {@link #RESULT_KEY_UIN} value of long in case of success
     * and a {@link #RESULT_KEY_ERROR} value of type string in case of failure.
     * If force is used, they might be both not be null; the error
     * reports overwritten uin information in that case.
     * 
     * @param xwikiName the name of the wiki where the uin is installed
     * @param name the name of the uin sequence
     * @param token the secret token
     * @param clientId optional (depending on the used uin manager) the resource requesting the id, e.g. the wiki page
     * @param server optional; an identifier for the calling side
     * @param id optional; if present, check if this id is valid
     * @param force optional; if present and not empty, and an id is given,s
     *    force using this id in the sequence for this client and server
     * @param simulate optional; if present and not empty, then only simulate the action, do not create a new id
     * @return the next UIN or an error message
     * @throws XWikiRestException in case of exceptions
     */
    @GET
    @Path("/{name}/next")
    @Produces(MediaType.APPLICATION_JSON)
    Object getUIN(
        @PathParam("wikiName") String xwikiName,
        @PathParam("name") String name,
        @QueryParam("token") String token,
        @QueryParam("clientid") String clientId,
        @QueryParam("server") String server,
        @QueryParam("id") String id,
        @QueryParam("force") String force,
        @QueryParam("simulate") String simulate
    ) throws XWikiRestException;

    /**
     * Return a list of all uins with their data for the given sequence name.
     * The returned value is a map with either an "errorMessage" key
     * of a "values" key containing a list of items,
     * which in turn have an 'uin', 'clientId' and 'server' entry,
     * sorted by id.
     *
     * @param xwikiName the name of the wiki where the uin is installed
     * @param name the name of the UID sequence
     * @param token (optional) the secret token, if necessary
     * @return a list of { 'uin', 'clientId', 'server' } under key 'values' or an error message in key 'error'
     * @throws XWikiRestException in case of unexpected exceptions
     * @since 2.3
     */
    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    Object getUINList(
        @PathParam("wikiName") String xwikiName,
        @PathParam("name") String name,
        @QueryParam("token") String token
    ) throws XWikiRestException;

}
