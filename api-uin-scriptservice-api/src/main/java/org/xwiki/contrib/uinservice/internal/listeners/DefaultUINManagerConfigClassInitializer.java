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
package org.xwiki.contrib.uinservice.internal.listeners;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Creates the configuration class to store which UIN manager is active.
 *
 * @version $Id$
 * @since 2.3
 */
@Component
@Named(DefaultUINManagerConfigClassInitializer.CONFIG_CLASS_FULLNAME)
@Singleton
public class DefaultUINManagerConfigClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * reference to the created class.
     */
    public static final LocalDocumentReference CLASS_REF = new LocalDocumentReference("UINCode",
        "UINDefaultConfigClass");
    /**
     * property of the manager to use.
     */
    public static final String SELECTED_STRATEGY = "selectedManager";
    static final String CONFIG_CLASS_FULLNAME = "UINCode.UINDefaultConfigClass";

    /**
     * default constructor.
     */
    public DefaultUINManagerConfigClassInitializer()
    {
        super(CLASS_REF);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(SELECTED_STRATEGY, "Selected UIN Manager Class", 30);
    }
}
