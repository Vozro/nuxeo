/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api.impl.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestStringBlob extends NXRuntimeTestCase {

    @Test
    public void testGetDigestOnDemand() {
        StringBlob blob = new StringBlob("string to test digest");
        // Before digest was computed, blob doesn't have a digest algorithm
        assertNull(blob.digest);
        assertNull(blob.getDigestAlgorithm());
        assertEquals("ee6367f657870e1664b2f7af4ece568c", blob.getDigest());
        assertEquals("MD5", blob.getDigestAlgorithm());
    }

}
