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
     * @throws DuplicateKeyException in case of duplicate configuration name
     * @throws XWikiException  in case of exceptions
     */
    void createConfig(String name, long currentUIN, long increment) throws DuplicateKeyException, XWikiException;

    /**
     * Update a UIN configuration.
     *
     * @param previousName the old configuration name
     * @param newName the new configuration name
     * @param currentUIN the starting index value
     * @param newUIN the new starting index value
     * @param increment the incremental step
     * @throws DuplicateKeyException in case of duplicate configuration name
     * @throws XWikiException in case of exceptions
     */
    void updateConfig(String previousName, String newName, long currentUIN, long newUIN, long increment)
        throws DuplicateKeyException, XWikiException;

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
     * @throws XWikiException in case of exceptions
     */
    long getNext() throws XWikiException;

    /**
     * Get the next UIN.
     *
     * @param name the name of the configuration
     * @return the next UIN
     * @throws XWikiException in case of exceptions
     */
    long getNext(String name) throws XWikiException;
}
