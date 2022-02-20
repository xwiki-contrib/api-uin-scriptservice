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
 * Creates the class to store UIN states for the {@link GaplessUINManager}.
 * @version $Id$
 * @since 2.3
 */
@Component
@Named(GaplessUINSequenceClassInitializer.CONFIG_CLASS_FULLNAME)
@Singleton
public class GaplessUINSequenceClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Reference to the created class.
     */
    public static final LocalDocumentReference CLASS_REF = new LocalDocumentReference("UINCode",
        "GaplessUINSequenceClass");

    /**
     * Property name of the stored UIN.
     */
    public static final String PROPERTY_UIN = "uin";

    /**
     * client id property name.
     */
    public static final String PROPERTY_CLIENT = "clientId";

    /**
     * server property name.
     */
    public static final String PROPERTY_SERVER = "server";

    /**
     * The full class name, as to be used in XWQL queries.
     */
    public static final String CONFIG_CLASS_FULLNAME = "UINCode.GaplessUINSequenceClass";

    /**
     * default constructor.
     */
    public GaplessUINSequenceClassInitializer()
    {
        super(CLASS_REF);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addNumberField(PROPERTY_UIN, "Current UIN", 10, "long");
        xclass.addTextField(PROPERTY_CLIENT, "Client", 50);
        xclass.addTextField(PROPERTY_SERVER, "Server", 10);
    }
}
