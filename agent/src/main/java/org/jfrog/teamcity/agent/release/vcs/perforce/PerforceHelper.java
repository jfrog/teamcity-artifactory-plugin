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
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Helper class to communicate with perforce
 *
 * @author Shay Yaakov
 */
public class PerforceHelper {
    private IServer server;
    private IClient client;
    private String perforceUser;
    private String perforcePass;
    private String perforceWorkspace;
    private IChangelist changelist;
    private int changeListNum;
    private boolean labelCreated;

    public PerforceHelper(Map<String, String> perforceProperties) {
        if (perforceProperties.containsKey("user")) {
            perforceWorkspace = perforceProperties.get("client");
            perforceUser = perforceProperties.get("user");
            perforcePass = perforceProperties.get("secure:passwd");
        }

        // todo: take from env
        this.server = newServer("localhost:1666");
        createNewChangeList();
    }

    private void createNewChangeList() {
        IChangelist newChangeList = new Changelist(IChangelist.UNKNOWN, perforceWorkspace,
                server.getUserName(), ChangelistStatus.NEW, new Date(), "temp changelist", false, (Server) server);
        try {
            changelist = client.createChangelist(newChangeList);
            changelist.update();
            changeListNum = changelist.getId();
        } catch (ConnectionException e) {
            throw new UnsupportedOperationException("Implement me");
        } catch (RequestException e) {
            throw new UnsupportedOperationException("Implement me");
        } catch (AccessException e) {
            throw new UnsupportedOperationException("Implement me");
        }
    }

    /**
     * Returns a new connected server instance given the server+port, and optionally user login credentials.
     * Server is logged into if user credentials are supplied.
     */
    private IServer newServer(String p4Url) {
        Properties props = new Properties();
        props.put("autoConnect", false);
        props.put("autoLogin", false);
        IServer server = null;
        try {
            server = ServerFactory.getServer("p4java://" + p4Url, props);
            server.connect();
            if (perforceUser != null) {
                server.setUserName(perforceUser);
                if (perforcePass != null) {
                    server.login(perforcePass);
                }
            }
            client = server.getClient(perforceWorkspace);
            server.setCurrentClient(client);
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ConnectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchObjectException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ConfigException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ResourceException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (RequestException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return server;
    }

    public void editFiles(File file, boolean releaseVersion) {
        try {
            List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(file.getAbsolutePath());
            if (releaseVersion) {
                client.editFiles(fileSpecs, false, false, changeListNum, null);
            } else {
                // Use the default changelist
                client.editFiles(fileSpecs, false, false, 0, null);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (RequestException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void commitWorkingCopy(String commitMsg) {
        /*changelist = server.getChangelist(IChangelist.DEFAULT);
        changelist.setDescription(commitMsg);
        changelist.getFiles(true);
        //ReopenFilesOptions reopenFilesOptions = new ReopenFilesOptions();
        //reopenFilesOptions.setChangelistId(changelist.getId());
        //client.reopenFiles(files, reopenFilesOptions);
        SubmitOptions submitOptions = new SubmitOptions("-f revertunchanged");
        changelist.submit(submitOptions);*/
        try {
            if (labelCreated) {
                changelist = server.getChangelist(IChangelist.DEFAULT);
            }
            changelist.setDescription(commitMsg);
            changelist.getFiles(true);
            SubmitOptions submitOptions = new SubmitOptions("-f revertunchanged");
            changelist.submit(submitOptions);
        } catch (ConnectionException e) {
            throw new UnsupportedOperationException("Implement me");
        } catch (RequestException e) {
            throw new UnsupportedOperationException("Implement me");
        } catch (AccessException e) {
            throw new UnsupportedOperationException("Implement me");
        } catch (P4JavaException e) {
            throw new UnsupportedOperationException("Implement me");
        }
    }

    public void createLabel(String labelName, String description) {
        final Date now = new Date();
        final ViewMap<ILabelMapping> viewMapping = new ViewMap<ILabelMapping>();
        ClientView clientView = client.getClientView();
        if (clientView != null) {
            for (IClientViewMapping clientViewMapping : clientView) {
                final ILabelMapping mapping = new Label.LabelMapping();
                mapping.setLeft(clientViewMapping.getDepotSpec());
                mapping.setViewMapping(clientViewMapping.getDepotSpec());
                viewMapping.addEntry(mapping);
            }
        }
        final ILabel label = new Label(labelName, perforceUser, now, now, description, "@" + changeListNum, false,
                viewMapping);
        try {
            server.createLabel(label);
            labelCreated = true;
        } catch (ConnectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (RequestException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void deleteLabel(String labelName) {
        try {
            server.deleteLabel(labelName, true);
        } catch (ConnectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (RequestException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void revertWorkingCopy() {
        try {
            client.revertFiles(changelist.getFiles(true), false, 0, false, false);
            server.deletePendingChangelist(changelist.getId());
        } catch (ConnectionException e) {
            throw new UnsupportedOperationException("Implement me");
        } catch (AccessException e) {
            throw new UnsupportedOperationException("Implement me");
        } catch (RequestException e) {
            throw new UnsupportedOperationException("Implement me");
        }
    }
}
