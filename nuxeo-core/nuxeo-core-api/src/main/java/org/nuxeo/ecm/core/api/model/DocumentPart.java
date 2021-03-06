/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model;

import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * A document part is the root of a property tree which is specified by a schema
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentPart extends Property {

    /**
     * The document schema.
     *
     * @return the schema
     */
    @Override
    Schema getSchema();

    Property createProperty(Property parent, Field field);

    Property createProperty(Property parent, Field field, int flags);

    PropertyDiff exportDiff() throws PropertyException;

    void importDiff(PropertyDiff diff) throws PropertyException;

    //
    // public void setContextData(String key, Object value);
    //
    // public Object getContextData(String key);

}
