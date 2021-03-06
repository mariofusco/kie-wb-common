/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.services.backend.config.attribute;

import org.kie.commons.data.Pair;
import org.kie.commons.java.nio.IOException;
import org.kie.commons.java.nio.base.AbstractBasicFileAttributeView;
import org.kie.commons.java.nio.base.AbstractPath;
import org.kie.commons.java.nio.base.NeedsPreloadedAttrs;
import org.kie.commons.java.nio.file.attribute.BasicFileAttributeView;
import org.kie.commons.java.nio.file.attribute.BasicFileAttributes;
import org.kie.commons.java.nio.file.attribute.FileTime;

import java.util.Map;

import static org.kie.commons.data.Pair.newPair;
import static org.kie.workbench.common.services.backend.config.attribute.ConfigAttributesUtil.toMap;

/**
 *
 */
public class ConfigView extends AbstractBasicFileAttributeView<AbstractPath>
        implements NeedsPreloadedAttrs {

    private final ConfigAttributes attrs;

    public ConfigView( final AbstractPath path ) {
        super( path );

        final BasicFileAttributes fileAttrs = path.getFileSystem().provider().getFileAttributeView( path, BasicFileAttributeView.class ).readAttributes();

        this.attrs = new ConfigAttributes() {

            @Override
            public FileTime lastModifiedTime() {
                return fileAttrs.lastModifiedTime();
            }

            @Override
            public FileTime lastAccessTime() {
                return fileAttrs.lastAccessTime();
            }

            @Override
            public FileTime creationTime() {
                return fileAttrs.creationTime();
            }

            @Override
            public boolean isRegularFile() {
                return fileAttrs.isRegularFile();
            }

            @Override
            public boolean isDirectory() {
                return fileAttrs.isDirectory();
            }

            @Override
            public boolean isSymbolicLink() {
                return fileAttrs.isSymbolicLink();
            }

            @Override
            public boolean isOther() {
                return fileAttrs.isOther();
            }

            @Override
            public long size() {
                return fileAttrs.size();
            }

            @Override
            public Object fileKey() {
                return fileAttrs.fileKey();
            }
        };
    }

    private Pair<Integer, String> extractValue( final Map.Entry<String, Object> entry ) {
        int start = entry.getKey().indexOf( '[' );
        if ( start < 0 ) {
            return newPair( 0, entry.getValue().toString() );
        }
        int end = entry.getKey().indexOf( ']' );

        return newPair( Integer.valueOf( entry.getKey().substring( start + 1, end ) ), entry.getValue().toString() );
    }

    @Override
    public String name() {
        return "config";
    }

    @Override
    public ConfigAttributes readAttributes() throws IOException {
        return attrs;
    }

    @Override
    public Map<String, Object> readAttributes( final String... attributes ) {
        return toMap( readAttributes(), attributes );
    }

    @Override
    public Class<? extends BasicFileAttributeView>[] viewTypes() {
        return new Class[]{ ConfigView.class };
    }

}
