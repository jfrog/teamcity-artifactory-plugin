package org.jfrog.teamcity.agent.release.vcs.perforce;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.ChangelistSummary;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Client to communicate with perforce.
 *
 * @author Shay Yaakov
 */
public class PerforceClient {
    private IServer server;
    private IClient client;

    private PerforceClient(String hostAddress, String clientId, String username, String password) throws IOException {
        createServer(hostAddress, clientId, username, password);
    }

    /**
     * Creates a new connected server instance given the server+port, and optionally user login credentials.
     */
    private void createServer(String hostAddress, String clientId, String username, String password)
            throws IOException {
        try {
            Properties props = new Properties();
            props.put("autoConnect", true);
            props.put("autoLogin", true);
            server = ServerFactory.getServer("p4java://" + hostAddress, props);
            server.connect();
            // login only after the connection
            server.setUserName(username);
            server.login(password);
            client = server.getClient(clientId);
            server.setCurrentClient(client);
        } catch (URISyntaxException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        } catch (P4JavaException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        }
    }

    public IChangelist createNewChangeList() throws IOException {
        try {
            ChangelistSummary summary = new ChangelistSummary(IChangelist.UNKNOWN, client.getName(),
                    server.getUserName(), ChangelistStatus.NEW, new Date(),
                    "Artifactory release plugin", false);
            IChangelist newChangeList = new Changelist(summary, server, false);
            newChangeList = client.createChangelist(newChangeList);
            newChangeList.update();
            return newChangeList;
        } catch (P4JavaException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        }
    }

    public void editFile(int changeListId, File file) throws IOException {
        try {
            List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(file.getAbsolutePath());
            client.editFiles(fileSpecs, false, false, changeListId, null);
        } catch (P4JavaException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        }
    }

    public void commitWorkingCopy(IChangelist changelist, String commitMessage) throws IOException {
        try {
            changelist.setDescription(commitMessage);
            changelist.getFiles(true);
            SubmitOptions submitOptions = new SubmitOptions("-f revertunchanged");
            changelist.submit(submitOptions);
        } catch (P4JavaException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        }
    }

    public void createLabel(String labelName, String description, String changeListId) throws IOException {
        ViewMap<ILabelMapping> viewMapping = new ViewMap<ILabelMapping>();
        ClientView clientView = client.getClientView();
        if (clientView != null) {
            for (IClientViewMapping clientViewMapping : clientView) {
                ILabelMapping mapping = new Label.LabelMapping();
                mapping.setLeft(clientViewMapping.getDepotSpec());
                mapping.setViewMapping(clientViewMapping.getDepotSpec());
                viewMapping.addEntry(mapping);
            }
        }
        Date now = new Date();
        ILabel label = new Label(labelName, client.getOwnerName(), now, now, description, "@" + changeListId, false,
                viewMapping);
        try {
            server.createLabel(label);
        } catch (P4JavaException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        }
    }

    public void deleteLabel(String labelName) throws IOException {
        try {
            server.deleteLabel(labelName, false);
        } catch (P4JavaException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        }
    }

    public void revertWorkingCopy(IChangelist changelist) throws IOException {
        try {
            client.revertFiles(changelist.getFiles(true), false, changelist.getId(), false, false);
            deleteChangeList(changelist);
        } catch (P4JavaException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        }
    }

    public IChangelist getDefaultChangeList() throws IOException {
        try {
            return server.getChangelist(IChangelist.DEFAULT);
        } catch (P4JavaException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        }
    }

    public void deleteChangeList(IChangelist changeList) throws IOException {
        try {
            if (changeList.getId() != IChangelist.DEFAULT) {
                server.deletePendingChangelist(changeList.getId());
            }
        } catch (P4JavaException e) {
            throw new IOException("Perforce execution failed: '" + e.getMessage() + "'", e);
        }
    }

    public static class Builder {
        /**
         * The Perforce server address in the format of hostname[:port]
         */
        private String hostAddress;
        private String username;
        private String password;
        private String clientId;

        public Builder() {
        }

        public PerforceClient build() throws IOException {
            if (StringUtils.isBlank(clientId)) {
                throw new IllegalStateException("Client clientId cannot be empty");
            }
            if (StringUtils.isBlank(hostAddress)) {
                throw new IllegalStateException("Hostname cannot be empty");
            }
            return new PerforceClient(hostAddress, clientId, username, password);
        }

        /**
         * @param hostAddress The Perforce server address in the format of hostname[:port]
         */
        public Builder hostAddress(String hostAddress) {
            this.hostAddress = hostAddress;
            return this;
        }

        public Builder client(String client) {
            this.clientId = client;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }
    }
}
