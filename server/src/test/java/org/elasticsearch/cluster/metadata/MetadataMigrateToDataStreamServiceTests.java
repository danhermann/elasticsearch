/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.cluster.metadata;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.containsString;

public class MetadataMigrateToDataStreamServiceTests  extends ESTestCase {

    public void testValidateRequestWithNonexistentAlias() {
        ClusterState cs = ClusterState.EMPTY_STATE;
        String nonExistentAlias = "nonexistent_alias";
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> MetadataMigrateToDataStreamService
            .validateRequest(cs, new MetadataMigrateToDataStreamService.MigrateToDataStreamClusterStateUpdateRequest(
                nonExistentAlias, TimeValue.ZERO, TimeValue.ZERO)));
        assertThat(e.getMessage(), containsString("alias [" + nonExistentAlias + "] does not exist"));
    }

    public void testValidateRequestWithFilteredAlias() {
        String filteredAliasName = "nonexistent_alias";
        AliasMetadata filteredAlias = AliasMetadata.builder(filteredAliasName).filter("{\"term\":{\"user.id\":\"kimchy\"}}").build();
        ClusterState cs = ClusterState.builder(new ClusterName("dummy")).metadata(
            Metadata.builder().put(IndexMetadata.builder("foo")
                .settings(Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT))
                .putAlias(filteredAlias)
                .numberOfShards(1)
                .numberOfReplicas(0))
        ).build();
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> MetadataMigrateToDataStreamService
            .validateRequest(cs, new MetadataMigrateToDataStreamService.MigrateToDataStreamClusterStateUpdateRequest(
                filteredAliasName, TimeValue.ZERO, TimeValue.ZERO)));
        assertThat(e.getMessage(), containsString("alias [" + filteredAliasName + "] may not have custom filtering or routing"));
    }

    public void testValidateRequestWithAliasWithRouting() {
        String routedAliasName = "nonexistent_alias";
        AliasMetadata aliasWithRouting = AliasMetadata.builder(routedAliasName).routing("foo").build();
        ClusterState cs = ClusterState.builder(new ClusterName("dummy")).metadata(
            Metadata.builder().put(IndexMetadata.builder("foo")
                .settings(Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT))
                .putAlias(aliasWithRouting)
                .numberOfShards(1)
                .numberOfReplicas(0))
        ).build();
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> MetadataMigrateToDataStreamService
            .validateRequest(cs, new MetadataMigrateToDataStreamService.MigrateToDataStreamClusterStateUpdateRequest(
                routedAliasName, TimeValue.ZERO, TimeValue.ZERO)));
        assertThat(e.getMessage(), containsString("alias [" + routedAliasName + "] may not have custom filtering or routing"));
    }

    public void testValidateRequest() {
        String aliasName = "alias";
        AliasMetadata alias1 = AliasMetadata.builder(aliasName).build();
        AliasMetadata alias2 = AliasMetadata.builder(aliasName + "2").build();
        ClusterState cs = ClusterState.builder(new ClusterName("dummy")).metadata(
            Metadata.builder()
                .put(IndexMetadata.builder("foo1")
                    .settings(Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT))
                    .putAlias(alias1)
                    .numberOfShards(1)
                    .numberOfReplicas(0))
                .put(IndexMetadata.builder("foo2")
                    .settings(Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT))
                    .putAlias(alias1)
                    .numberOfShards(1)
                    .numberOfReplicas(0))
                .put(IndexMetadata.builder("foo3")
                    .settings(Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT))
                    .putAlias(alias1)
                    .numberOfShards(1)
                    .numberOfReplicas(0))
                .put(IndexMetadata.builder("foo4")
                    .settings(Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT))
                    .putAlias(alias1)
                    .numberOfShards(1)
                    .numberOfReplicas(0))
        ).build();
        MetadataMigrateToDataStreamService.validateRequest(cs,
            new MetadataMigrateToDataStreamService.MigrateToDataStreamClusterStateUpdateRequest(aliasName, TimeValue.ZERO, TimeValue.ZERO));
    }
}
