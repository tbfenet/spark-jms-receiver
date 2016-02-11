# JMS spark receiver

Reliable receiver for Spark streaming from any JMS 1.1 source. Use net.tbfe.spark.streaming.jms.JmsStreamUtils to create InputDStream
 
### AsynchronousJmsReceiver

Should be used if JMS provider supports and individual acknowledge mode. This is not part of the JMS 
standard but is supported by some providers. Also can be used if you don't need need reliable receiver 
with Session.AUTO_ACKNOWLEDGE mode. 

### SynchronousJmsReceiver
 
Should be used if you require reliable receiver and JMS provider does not support a individual acknowledge mode.