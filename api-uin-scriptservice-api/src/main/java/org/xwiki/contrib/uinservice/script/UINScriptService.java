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
package org.xwiki.contrib.uinservice.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.uinservice.DuplicateKeyException;
import org.xwiki.contrib.uinservice.UINManager;
import org.xwiki.contrib.uinservice.internal.DefaultUINManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 2.2
 */
@Component
@Named("uin")
@Singleton
public class UINScriptService implements ScriptService
{
    @Inject
    @Named("default")
    private UINManager uinManager;

    @Inject
    private AuthorizationManager authManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Create a new UIN configuration.
     *
     * @param name the configuration name
     * @param currentUIN the starting index value
     * @param increment the incremental step
     * @throws DuplicateKeyException in case of duplicate configuration name
     * @throws XWikiException in case of exceptions
     */
    public void createConfig(String name, long currentUIN, long increment) throws DuplicateKeyException, XWikiException
    {
        createConfig(name, currentUIN, increment, "");
    }

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
    public void createConfig(String name, long currentUIN, long increment, String token)
        throws DuplicateKeyException, XWikiException
    {
        uinManager.createConfig(name, currentUIN, increment, token);
    }

    /**
     * Update a UIN configuration.
     *
     * @param previousName the old configuration name
     * @param newName the new configuration name
     * @param currentUIN the starting index value
     * @param newUIN the new starting index value
     * @param increment the incremental step
     * @throws DuplicateKeyException in case of duplicate configuration name
     * @throws Exception in case of exceptions
     */
    public void updateConfig(String previousName, String newName, long currentUIN, long newUIN, long increment)
        throws DuplicateKeyException, Exception
    {
        updateConfig(previousName, newName, currentUIN, newUIN, increment, "");
    }

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
    public void updateConfig(String previousName, String newName, long currentUIN, long newUIN, long increment,
        String token) throws DuplicateKeyException, Exception
    {
        uinManager.updateConfig(previousName, newName, currentUIN, newUIN, increment, token);
    }

    /**
     * Remove a UIN configuration.
     *
     * @param name the configuration name
     * @throws XWikiException in case of exceptions
     */
    public void removeConfig(String name) throws XWikiException
    {
        uinManager.removeConfig(name);
    }

    /**
     * Get the next UIN.
     *
     * @return the next UIN
     * @throws Exception in case of exceptions
     */
    public long getNext() throws Exception
    {
        return uinManager.getNext();
    }

    /**
     * Get the next UIN.
     *
     * @param name the name of the configuration
     * @return the next UIN
     * @throws Exception in case of exceptions
     */
    public long getNext(String name) throws Exception
    {
        return uinManager.getNext(name);
    }

    /**
     * Get a list of available names for UIN generation strategies.
     * @return a list of strategy names
     */
    public List<String> getAvailableStrategies()
    {
        return ((DefaultUINManager) uinManager).getAvailableStrategies();
    }

    /**
     * Get the active UIN generation strategy name.
     * @return the name of the currently active strategy
     */
    public String getActiveStrategy()
    {
        return ((DefaultUINManager) uinManager).getActiveStrategy();
    }

    /**
     * Allow to change the given strategy.
     * @param strategyName the name of currently active strategy
     * @throws AccessDeniedException if the current user is not admin
     * @throws XWikiException if anything else with setting the strategy goes wrong
     */
    public void setActiveStrategy(String strategyName) throws AccessDeniedException, XWikiException
    {
        authManager.checkAccess(Right.ADMIN, contextProvider.get().getUserReference(),
            contextProvider.get().getWikiReference());
        ((DefaultUINManager) uinManager).setActiveStrategy(strategyName);
    }
}
