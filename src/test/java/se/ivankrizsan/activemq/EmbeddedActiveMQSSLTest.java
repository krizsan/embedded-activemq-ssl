package se.ivankrizsan.activemq;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.ivankrizsan.configuration.JmsTestConfiguration;
import se.ivankrizsan.configuration.ReadTestPropertiesConfiguration;

import javax.jms.Message;
import javax.jms.TextMessage;

/**
 * Tests connecting to the embedded ActiveMQ broker using SSL connections.
 *
 * @author Ivan Krizsan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ReadTestPropertiesConfiguration.class, JmsTestConfiguration.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EmbeddedActiveMQSSLTest {
    /** Class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedActiveMQSSLTest.class);

    /* Constant(s): */
    protected final static String TEST_QUEUE = "testQueue";
    protected final static String TEST_MESSAGE_STRING = "This is a text message!";

    /* Instance variable(s): */
    @Autowired
    protected JmsTemplate mJmsTestTemplate;

    /**
     * Tests sending and receiving a message to/from the broker.
     * Expected result:
     * It should be possible to send and receive one message to/from the JMS broker.
     * The payload of the received message should be the same as the payload of the sent message.
     *
     * @throws Exception If error occurs. Indicates test failure.
     */
    @Test
    public void testSendAndReceiver() throws Exception {
        LOGGER.debug("About to send JMS message");
        mJmsTestTemplate.send(TEST_QUEUE,
            (inSession) -> {
                final TextMessage theTextMessage = new ActiveMQTextMessage();
                theTextMessage.setText(TEST_MESSAGE_STRING);
                return theTextMessage;
            });
        LOGGER.debug("JMS message sent");

        LOGGER.debug("About to receive JMS message");
        final Message theMessage = mJmsTestTemplate.receive(TEST_QUEUE);
        LOGGER.debug("JMS message received");

        Assert.assertNotNull("There should be a message on the queue", theMessage);
        Assert.assertTrue("The message should be a text message", theMessage instanceof  TextMessage);
        final TextMessage theTextMessage = (TextMessage)theMessage;
        LOGGER.debug("Contents of received message: {}", theTextMessage.getText());
        Assert.assertEquals("Message contents should match", TEST_MESSAGE_STRING, theTextMessage.getText());
    }
}
