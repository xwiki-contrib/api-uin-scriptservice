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
 * Creates the configuration class to store settings for the {@link GaplessUINManager}.
 *
 * @version $Id$
 * @since 2.3
 */
@Component
@Named(GaplessUINManagerConfigClassInitializer.CONFIG_CLASS_FULLNAME)
@Singleton
public class GaplessUINManagerConfigClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Reference to the created configuration class.
     */
    public static final LocalDocumentReference CLASS_REF = new LocalDocumentReference("UINCode",
        "GaplessUINConfigClass");

    /**
     * Auto-create property name.
     */
    public static final String PROPERTY_AUTOCREATE = "create";

    /**
     * Property name to store the token.
     */
    public static final String PROPERTY_TOKEN = "token";


    static final String CONFIG_CLASS_FULLNAME = "UINCode.GaplessUINConfigClass";

    /**
     * default constructor.
     */
    public GaplessUINManagerConfigClassInitializer()
    {
        super(CLASS_REF);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addBooleanField(PROPERTY_AUTOCREATE, "Can create new sequences automatically", "checkbox", Boolean.TRUE);
        xclass.addPasswordField(PROPERTY_TOKEN, "Token",  30, "Clear");
    }
}
