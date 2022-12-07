/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.server.global;

import jetbrains.buildServer.BuildTypeDescriptor;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.CustomDataStorageKeys;

public class ArtifactoryServerListener extends BuildServerAdapter {

    private SBuildServer server;
    private ServerConfigPersistenceManager configPersistenceManager;

    public ArtifactoryServerListener(@NotNull final EventDispatcher<BuildServerListener> dispatcher,
                                     @NotNull final SBuildServer server, @NotNull ServerPaths serverPaths) {
        this.server = server;

        dispatcher.addListener(this);

        configPersistenceManager = new ServerConfigPersistenceManager(serverPaths);
    }

    @Override
    public void serverStartup() {
        Loggers.SERVER.info("Plugin '" + ConstantValues.NAME + "'. Is running on server version " +
                server.getFullServerVersion()
                + ".");
    }

    @Override
    public void buildFinished(SRunningBuild build) {
        super.buildFinished(build);

        SBuildType type = build.getBuildType();
        if (type != null) {
            CustomDataStorage checkConfigChanges =
                    type.getCustomDataStorage(CustomDataStorageKeys.CHECKOUT_CONFIGURATION_CHANGE_HISTORY);
            String oldCheckoutConfigurationName = checkConfigChanges.getValue(type.getBuildTypeId());
            if (StringUtils.isNotBlank(oldCheckoutConfigurationName)) {
                type.setCheckoutType(BuildTypeDescriptor.CheckoutType.valueOf(oldCheckoutConfigurationName));
                checkConfigChanges.putValue(type.getBuildTypeId(), null);
            }
        }
    }

    public ServerConfigPersistenceManager getConfigModel() {
        return configPersistenceManager;
    }
}