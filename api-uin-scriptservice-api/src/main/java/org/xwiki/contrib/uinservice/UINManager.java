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

import java.util.List;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 2.2
 */
@Role
public interface UINManager
{
    /**
     * Create a new UIN configuration.
     *
     * @param name the configuration name
     * @param currentUIN the starting index value
     * @param increment the incremental step
     * @param token the secret token
     * @throws DuplicateKeyException in case of duplicate configuration name
     * @throws XWikiException in case of exceptions
     */
    void createConfig(String name, long currentUIN, long increment, String token)
        throws DuplicateKeyException, XWikiException;

    /**
     * Update a UIN configuration.
     *
     * @param previousName the old configuration name
     * @param newName the new configuration name
     * @param currentUIN the starting index value
     * @param newUIN the new starting index value
     * @param increment the incremental step
     * @param token the secret token
     * @throws DuplicateKeyException in case of duplicate configuration name
     * @throws Exception in case of exceptions
     */
    void updateConfig(String previousName, String newName, long currentUIN, long newUIN, long increment, String token)
        throws DuplicateKeyException, Exception;

    /**
     * Remove a UIN configuration.
     *
     * @param name the configuration name
     * @throws XWikiException in case of exceptions
     */
    void removeConfig(String name) throws XWikiException;

    /**
     * Get the next UIN.
     *
     * @return the next UIN
     * @throws Exception in case of exceptions
     */
    long getNext() throws Exception;

    /**
     * Get the next UIN for the given name.
     *
     * @param name the name of the configuration
     * @return the next UIN
     * @throws Exception in case of exceptions
     */
    long getNext(String name) throws Exception;

    /**
     * Get the next UIN for the given name and other parameters.
     *
     * @param name the name of the configuration
     * @param server the server from which the id is requested
     * @param clientId the client requesting the id
     * @param id optional; if given, check if this id is valid / reserved
     *    and in that case return this id (and not a new one)
     * @param simulate generate a new id, but do not store it
     * @return the next UIN
     * @throws Exception in case of exceptions
     */
    default long getNext(String name, String server, String clientId, Long id, boolean simulate) throws Exception
    {
        return getNext(name);
    }

    /**
     * Check if the given <code>token</code> matches the internally stored token associated with the current UIN
     * configuration.
     *
     * @param name the configuration name
     * @param token the token from the request
     * @return {@code true} if the given token is correct, {@code false} otherwise
     * @throws XWikiException in case of exception
     * @throws Exception in case of exceptions
     */
    boolean isTokenValid(String name, String token) throws XWikiException, Exception;

    /**
     * Return information about all the generated UIDs so far. (Optional operation.)
     * @param name the name of the sequence.
     * @return a list of maps having keys 'uin', 'clientId' and 'server'.
     * @throws Exception in case of exceptions
     */
    List<? extends Object> getUIDsForName(String name) throws Exception;
}
