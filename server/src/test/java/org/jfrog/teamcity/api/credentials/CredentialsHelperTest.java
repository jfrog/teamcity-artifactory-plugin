package org.jfrog.teamcity.api.credentials;

import org.jfrog.teamcity.api.ServerConfigBean;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Noam Y. Tenne
 */
@Test
public class CredentialsHelperTest {

    @Test(dataProvider = "PreferredDeployingCredentials")
    public void testGetPreferredDeployingCredentials(ServerConfigBean serverConfigBean, boolean overrideDeployerCredentials,
                                                     String username, String password, CredentialsBean expectedCredentials) {
        CredentialsBean generated = CredentialsHelper.getPreferredResolvingCredentials(serverConfigBean, overrideDeployerCredentials, username, password);
        assertEquals(generated.getUsername(), expectedCredentials.getUsername(), "Unexpected username");
        assertEquals(generated.getPassword(), expectedCredentials.getPassword(), "Unexpected password");
    }

    @DataProvider(name = "PreferredDeployingCredentials")
    public static Object[][] testPreferredDeployingCredentials() {
        return new Object[][]{
                // Structure: {createServerConfig(useDifferentResolverCreds, addResolver, addDeployer), overrideDeployerCredentials, username, password, expectedCredentials}

                // Case of missing serverConfigBean use provided creds
                {null, false, "foo", "bar", new CredentialsBean("foo", "bar")},
                // overrideDeployerCredentials - use provided creds
                {createServerConfig(true, false, true), true, "foo", "bar", new CredentialsBean("foo", "bar")},
                // missing deployer creds - use provided creds
                {createServerConfig(true, false, false), false, "foo", "bar", new CredentialsBean("foo", "bar")},
                // use deployer creds
                {createServerConfig(true, false, true), false, "foo", "bar", new CredentialsBean("foo", "bar")}
        };
    }

    @Test(dataProvider = "PreferredResolvingCredentialsData")
    public void testGetPreferredResolvingCredentials(ServerConfigBean serverConfigBean, boolean overrideDeployerCredentials,
                                                     String username, String password, CredentialsBean expectedCredentials) {
        CredentialsBean generated = CredentialsHelper.getPreferredResolvingCredentials(serverConfigBean, overrideDeployerCredentials, username, password);
        assertEquals(generated.getUsername(), expectedCredentials.getUsername(), "Unexpected username");
        assertEquals(generated.getPassword(), expectedCredentials.getPassword(), "Unexpected password");
    }

    @DataProvider(name = "PreferredResolvingCredentialsData")
    public static Object[][] preferredResolvingCredentialsData() {
        return new Object[][]{
                // Structure: {createServerConfig(useDifferentResolverCreds, addResolver, addDeployer), overrideDeployerCredentials, username, password, expectedCredentials}

                // Case of missing serverConfigBean
                {null, false, "foo", "bar", new CredentialsBean("foo", "bar")},
                // overrideDeployerCredentials
                {createServerConfig(true, true, true), true, "foo", "bar", new CredentialsBean("foo", "bar")},
                // useDifferentResolverCreds and credentials provided
                {createServerConfig(true, true, true), false, "foo", "bar", new CredentialsBean("resolver", "resPass")},
                // useDifferentResolverCreds and credentials missing
                {createServerConfig(true, false, true), false, "foo", "bar", new CredentialsBean("foo", "bar")},
                // use Deployer credentials
                {createServerConfig(false, true, true), false, "foo", "bar", new CredentialsBean("deployer", "depPass")},
                // use Deployer credentials but Deployer credentials are missing (useDifferentResolverCreds = false)
                {createServerConfig(false, true, false), false, "foo", "bar", new CredentialsBean("foo", "bar")},
                // fallback to provided credentials
                {createServerConfig(false, true, false), false, "foo", "bar", new CredentialsBean("foo", "bar")},
                // fallback to provided credentials
                {createServerConfig(false, false, false), false, "foo", "bar", new CredentialsBean("foo", "bar")}
        };
    }

    private static ServerConfigBean createServerConfig(boolean useDifferentResolverCreds, boolean addResolver, boolean addDeployer) {
        ServerConfigBean serverConfig = new ServerConfigBean();
        serverConfig.setUseDifferentResolverCredentials(useDifferentResolverCreds);
        if (addResolver) {
            serverConfig.setDefaultResolverCredentials(new CredentialsBean("resolver", "resPass"));
        }
        if (addDeployer) {
            serverConfig.setDefaultDeployerCredentials(new CredentialsBean("deployer", "depPass"));
        }
        return serverConfig;
    }

    @Test(dataProvider = "PopulateCredentialsData")
    public void testPopulateCredentials(CredentialsBean credentials, String username, String password, CredentialsBean expectedCredentials) {
        CredentialsBean generated = CredentialsHelper.populateCredentials(credentials, username, password);
        assertEquals(generated.getUsername(), expectedCredentials.getUsername(), "Unexpected username");
        assertEquals(generated.getPassword(), expectedCredentials.getPassword(), "Unexpected password");
    }

    @DataProvider(name = "PopulateCredentialsData")
    public static Object[][] populateCredentialsData() {
        return new Object[][]{
                {new CredentialsBean(""), "foo", "bar", new CredentialsBean("foo", "bar")},
                {new CredentialsBean("user", "password"), "foo", "bar", new CredentialsBean("user", "password")},
                {new CredentialsBean("user"), "foo", "bar", new CredentialsBean("user", "bar")},
                {new CredentialsBean("", "password"), "foo", "bar", new CredentialsBean("foo", "password")},
                {new CredentialsBean("", ""), "foo", "bar", new CredentialsBean("foo", "bar")},
                {null, "foo", "bar", new CredentialsBean("foo", "bar")},
                {null, "", "", new CredentialsBean("", "")}
        };
    }
}