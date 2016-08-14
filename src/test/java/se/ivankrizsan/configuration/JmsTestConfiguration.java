package se.ivankrizsan.configuration;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.SslBrokerService;
import org.apache.activemq.usage.MemoryUsage;
import org.apache.activemq.usage.SystemUsage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

/**
 * Configuration of embedded ActiveMQ broker and related helpers used in tests.
 *
 * @author Ivan Krizsan
 */
@Configuration
public class JmsTestConfiguration {
    /* Constant(s): */

    /* Configuration parameter(s): */
    @Value("${JMS_BROKER_URL}")
    protected String mJmsBrokerUrl;
    @Value("${JMS_BROKER_TRUSTSTORE}")
    protected String mJmsBrokerTruststore;
    @Value("${JMS_BROKER_TRUSTSTORE_TYPE}")
    protected String mJmsBrokerTruststoreType;
    @Value("${JMS_BROKER_TRUSTSTORE_PASSWORD}")
    protected String mJmsBrokerTruststorePassword;
    @Value("${JMS_BROKER_KEYSTORE}")
    protected String mJmsBrokerKeystore;
    @Value("${JMS_BROKER_KEYSTORE_TYPE}")
    protected String mJmsBrokerKeystoreType;
    @Value("${JMS_BROKER_KEYSTORE_PASSWORD}")
    protected String mJmsBrokerKeystorePassword;
    @Value(("${CLIENT_JMS_BROKER_URL}"))
    protected String mClientJmsBrokerUrl;

    /**
     * JMS template for tests.
     *
     * @return JMS template.
     */
    @Bean
    public JmsTemplate jmsTestTemplate(final ConnectionFactory inConnectionFactory) {
        final JmsTemplate theJmsTestTemplate = new JmsTemplate(inConnectionFactory);
        theJmsTestTemplate.setReceiveTimeout(5000L);
        theJmsTestTemplate.afterPropertiesSet();
        return theJmsTestTemplate;
    }

    /**
     * Client JMS connection factory.
     * Depending on the client JMS broker URL, a TCP or a SSL connection factory will be created.
     *
     * @return JMS connection factory.
     */
    @Bean
    @DependsOn("embeddedBroker")
    public ConnectionFactory clientJMSConnectionFactory() {
        final ActiveMQConnectionFactory theConnectionFactory;

        if (mClientJmsBrokerUrl.startsWith("ssl")) {
            final ActiveMQSslConnectionFactory theSSLConnectionFactory =
                new ActiveMQSslConnectionFactory(mClientJmsBrokerUrl);
            try {
                theSSLConnectionFactory.setTrustStore(mJmsBrokerTruststore);
                theSSLConnectionFactory.setTrustStorePassword(mJmsBrokerTruststorePassword);
                theSSLConnectionFactory.setKeyStore(mJmsBrokerKeystore);
                theSSLConnectionFactory.setKeyStorePassword(mJmsBrokerKeystorePassword);

                theConnectionFactory = theSSLConnectionFactory;
            } catch (final Exception theException) {
                throw new Error(theException);
            }
        } else {
            theConnectionFactory = new ActiveMQConnectionFactory(mClientJmsBrokerUrl);
        }

        return theConnectionFactory;
    }

    /**
     * Embedded ActiveMQ broker for tests.
     * The embedded broker exposes only a SSL connector for clients.
     *
     * @return Embedded ActiveMQ broker.
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public BrokerService embeddedBroker() {
        SslBrokerService theActiveMqBroker;

        try {
            theActiveMqBroker = new SslBrokerService();
            theActiveMqBroker.setUseJmx(false);
            theActiveMqBroker.setPersistent(false);
            theActiveMqBroker.setUseShutdownHook(true);

            /* Add ActiveMQ SSL connector using configured keystore and truststore. */
            final KeyManager[] theKeystore = readKeystore();
            final TrustManager[] theTruststore = readTruststore();
            theActiveMqBroker.addSslConnector(mJmsBrokerUrl + "?transport.needClientAuth=true",
                theKeystore,
                theTruststore,
                null);

            /* Set memory limit in order not to use too much memory during tests. */
            final MemoryUsage theActiveMqMemoryUsage = new MemoryUsage();
            theActiveMqMemoryUsage.setPercentOfJvmHeap(20);
            final SystemUsage theActiveMqSystemUsage = new SystemUsage();
            theActiveMqSystemUsage.setMemoryUsage(theActiveMqMemoryUsage);
            theActiveMqBroker.setSystemUsage(theActiveMqSystemUsage);
        } catch (final Exception theException) {
            throw new Error("An error occurred starting test ActiveMQ broker", theException);
        }

        return theActiveMqBroker;
    }

    private KeyManager[] readKeystore() throws Exception {
        final KeyManagerFactory
            theKeyManagerFactory
            = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        final KeyStore theKeyStore = KeyStore.getInstance(mJmsBrokerKeystoreType);

        final Resource theKeystoreResource = new ClassPathResource(mJmsBrokerKeystore);
        theKeyStore.load(theKeystoreResource.getInputStream(), mJmsBrokerKeystorePassword.toCharArray());
        theKeyManagerFactory.init(theKeyStore, mJmsBrokerKeystorePassword.toCharArray());
        final KeyManager[] theKeystoreManagers = theKeyManagerFactory.getKeyManagers();
        return theKeystoreManagers;
    }

    private TrustManager[] readTruststore() throws Exception {
        final KeyStore theTruststore = KeyStore.getInstance(mJmsBrokerTruststoreType);

        final Resource theTruststoreResource = new ClassPathResource(mJmsBrokerTruststore);
        theTruststore.load(theTruststoreResource.getInputStream(), null);
        final TrustManagerFactory theTrustManagerFactory
            = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        theTrustManagerFactory.init(theTruststore);
        final TrustManager[] theTrustManagers = theTrustManagerFactory.getTrustManagers();
        return theTrustManagers;
    }
}
