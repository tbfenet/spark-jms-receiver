# JMS spark receiver

Reliable receiver for Spark streaming from any JMS 1.1 source. Use net.tbfe.spark.streaming.jms.JmsStreamUtils to create InputDStream

### AsynchronousJmsReceiver

Should be used if JMS provider supports and individual acknowledge mode. This is not part of the JMS
standard but is supported by some providers. Also can be used if you don't need need reliable receiver
with Session.AUTO_ACKNOWLEDGE mode.

### SynchronousJmsReceiver

Should be used if you require reliable receiver and JMS provider does not support a individual acknowledge mode.

## Steps for connecting

1. Setup Queue/Topic and connection factory in JNDI properties. This is JMS provider specific

1. Create instance of JndiMessageConsumerFactory.

1. Create JmsDestinationInfo which has and for Queue and an implementation for Topic

1. Use one of the methods of JmsStreamUtils to create InputDStream 

If not using JNDI then and implementation of MessageConsumerFactory needs to be created
