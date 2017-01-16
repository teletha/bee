/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.util.List;

import org.apache.maven.model.Contributor;

/**
 * @version 2017/01/16 16:27:13
 */
public interface VersionControlSystem {

    /** The name. */
    String name();

    /** The uri. */
    String uri();

    /** The uri for read access. */
    String uriForRead();

    /** The uri for write access. */
    String uriForWrite();

    /** The issue tracker uri. */
    String issue();

    /**
     * <p>
     * List of contributors.
     * </p>
     * 
     * @return
     */
    List<Contributor> contributors();
}
