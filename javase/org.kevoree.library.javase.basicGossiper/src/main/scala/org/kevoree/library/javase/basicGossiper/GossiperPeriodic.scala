package org.kevoree.library.javase.basicGossiper

import actors.{Actor, TIMEOUT}
import org.kevoree.library.basicGossiper.protocol.message.KevoreeMessage.Message
import org.kevoree.library.basicGossiper.protocol.gossip.Gossip.UpdatedValueNotification
import java.net.InetSocketAddress


class GossiperPeriodic(instance: GossiperComponent, timeout: Long, selector: PeerSelector, process: GossiperProcess) extends Actor {

  case class STOP()

  case class DO_GOSSIP(peer: String)

  def stop() {
    this ! STOP()
  }

  def doGossip(peer: String) {
    this ! DO_GOSSIP(peer)
  }


  /* PRIVATE PROCESS PART */
  def act() {
    loop {
      reactWithin(timeout.longValue) {
        case STOP() => stopInternal()
        case DO_GOSSIP(peer) => doGossipInternal(peer) // initialize a gossip
        case TIMEOUT => pull() // periddically ask for update on the network
      }
    }
  }

  private def stopInternal() {
    this.exit()
  }

  private def doGossipInternal(peer: String) {
    process.initGossip(peer)
  }

  private def pull() {
    val peer = selector.selectPeer(instance.getName)
    if (peer != null && !peer.equals("")) {
      org.kevoree.log.Log.debug("start pulling")
      process.initGossip(peer)
    }
  }

}