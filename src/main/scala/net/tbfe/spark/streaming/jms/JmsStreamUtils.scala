/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tbfe.spark.streaming.jms

import java.util.Properties
import javax.jms._
import javax.naming.{Context, InitialContext}
import org.apache.spark.streaming.jms.{ PublicLogging => Logging }
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.jms._
import org.apache.spark.streaming.receiver.{BlockGeneratorListener, Receiver}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.collection.JavaConverters._


object JmsStreamUtils {

  /**
    * Reliable Receiver to use for a Jms provider that does not support an individual acknowledgment
    * mode.
    *
    * @param consumerFactory  Implementation specific factory for building MessageConsumer.
    *                         Use JndiMessageConsumerFactory to setup via JNDI
    * @param messageConverter Function to map from Message type to T. Return None to filter out
    *                         message
    * @param batchSize        How meany messages to read off JMS source before submitting to
    *                         streaming. Every batch is a new task so reasonably high to avoid
    *                         excessive task creation.
    * @param maxWait          Max time to wait for messages before submitting a batch to streaming.
    * @param maxBatchAge      Max age of a batch before it is submitting. Used to cater for the case
    *                         of a slow trickle of messages
    * @param storageLevel
    * @tparam T
    */

  def createSynchronousJmsQueueStream[T: ClassTag](ssc: StreamingContext,
                                                   consumerFactory: MessageConsumerFactory,
                                                   messageConverter: (Message) => Option[T],
                                                   batchSize: Int = 1000,
                                                   maxWait: Duration = 1.second,
                                                   maxBatchAge: Duration = 10.seconds,
                                                   storageLevel: StorageLevel =
                                        StorageLevel.MEMORY_AND_DISK_SER_2
                                       ): ReceiverInputDStream[T] = {

    ssc.receiverStream(new SynchronousJmsReceiver[T](consumerFactory,
      messageConverter,
      batchSize,
      maxWait,
      maxBatchAge,
      storageLevel))

  }

  /**
    * Jms receiver that support asynchronous acknowledgement. If used with an individual
    * acknowledgement mode can be considered "Reliable". Individual acknowledgement mode is not
    * currently part of JMS spec but is supported by some vendors such as ActiveMQ and
    * Solace
    *
    * @param consumerFactory     Implementation specific factory for building MessageConsumer.
    *                            Use JndiMessageConsumerFactory to setup via JNDI
    * @param messageConverter    Function to map from Message type to T. Return None to filter out
    *                            message
    * @param acknowledgementMode Should either be Session.AUTO_ACKNOWLEDGE or a JMS providers code
∏    *                            for individual acknowledgement. If set to Session.AUTO_ACKNOWLEDGE
    *                            then this receiver is not "Reliable"
    * @param storageLevel
    * @tparam T
    */
  def createAsynchronousJmsQueueStream[T: ClassTag](ssc: StreamingContext,
                                                    consumerFactory: MessageConsumerFactory,
                                                    messageConverter: (Message) => Option[T],
                                                    acknowledgementMode: Int,
                                                    storageLevel: StorageLevel =
                                                    StorageLevel.MEMORY_AND_DISK_SER_2
                                                   ): ReceiverInputDStream[T] = {

    ssc.receiverStream(new AsynchronousJmsReceiver[T](consumerFactory,
      messageConverter,
      acknowledgementMode,
      storageLevel
    ))

  }
}

/**
  * Either topic or queue
  */
sealed trait JmsDestinationInfo {

}

/**
  *
  * @param queueName
  */
case class QueueJmsDestinationInfo(queueName: String) extends JmsDestinationInfo

/**
  *
  * @param topicName
  * @param subscriptionName
  */
case class DurableTopicJmsDestinationInfo(topicName: String,
  subscriptionName: String) extends JmsDestinationInfo



/**
 * Build Jms objects from JNDI
 *
 * @param jndiProperties        Implementation specific. JNDI properties with setup for connection
 *                              factory and destinations.
 * @param destinationInfo       Queue or Topic destination info.
 * @param connectionFactoryName Name of connection factory connfigured in JNDI
 * @param messageSelector       Message selector. Use Empty string for no message filter.
 */
case class JndiMessageConsumerFactory(jndiProperties: Properties,
  destinationInfo: JmsDestinationInfo,
  connectionFactoryName: String = "ConnectionFactory",
  messageSelector: String = "")
  extends MessageConsumerFactory with Logging {

  @volatile
  @transient
  var initialContext: InitialContext = _

  override def makeConsumer(session: Session): MessageConsumer = {
    destinationInfo match {
      case DurableTopicJmsDestinationInfo(topicName, subName) =>
        val dest = initialContext.lookup(topicName).asInstanceOf[Topic]
        session.createDurableSubscriber(dest,
          subName,
          messageSelector,
          false)
      case QueueJmsDestinationInfo(qName: String) =>
        val dest = initialContext.lookup(qName).asInstanceOf[Destination]
        session.createConsumer(dest, messageSelector)
    }
  }

  override def makeConnection: Connection = {
    if (initialContext == null) {
      initialContext = new InitialContext(jndiProperties)
    }
    val connectionFactory = initialContext
      .lookup(connectionFactoryName).asInstanceOf[ConnectionFactory]

    val props = jndiProperties.asScala
    val username = props.get(Context.SECURITY_PRINCIPAL)
    val password = props.get(Context.SECURITY_CREDENTIALS)

    val createConnection: Connection = (username, password) match {
      case (Some(username), Some(password)) =>
        connectionFactory.createConnection(username, password)
      case _ =>
        connectionFactory.createConnection()
    }
    createConnection
  }

}
