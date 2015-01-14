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
package org.xwiki.extension.index;

import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.ExtensionRepository;

/**
 * A simple class used to facilitate the handling of the indexed extensions, using the
 * {@link org.xwiki.extension.Extension Extension} interface.
 * 
 * @version $Id$
 */
public class IndexedExtension extends AbstractExtension
{
    /**
     * Constructor.
     * 
     * @param repository the repository where this extension comes from
     * @param id the id of the extension
     * @param type the extension type (i.e. jar, xar, etc)
     */
    public IndexedExtension(ExtensionRepository repository, ExtensionId id, String type)
    {
        super(repository, id, type);
    }
}