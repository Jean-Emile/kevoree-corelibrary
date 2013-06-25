package org.kevoree.library.javase.basicGossiper.group

import org.kevoree.ContainerRoot
import org.kevoree.framework.KevoreeXmiHelper
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import org.kevoree.api.service.core.handler.KevoreeModelHandlerService
import org.kevoree.library.javase.basicGossiper.Serializer

class GroupSerializer (modelService: KevoreeModelHandlerService) extends Serializer {


  def serialize (data: Any): Array[Byte] = {
    try {
        stringFromModel (data.asInstanceOf[ContainerRoot])
    } catch {
      case e => {
        org.kevoree.log.Log.error ("Model cannot be serialized: ", e)
        null
      }
    }
  }

  def deserialize (data: Array[Byte]): Any = {
    try {
      modelFromString (data)
    } catch {
      case e => {
        org.kevoree.log.Log.error ("Model cannot be deserialized: ", e)
        null
      }
    }
  }

  private def modelFromString (model: Array[Byte]): ContainerRoot = {
    val stream = new ByteArrayInputStream (model)
    KevoreeXmiHelper.instance$.loadCompressedStream(stream)
  }

  private def stringFromModel (model: ContainerRoot) : Array[Byte] = {
    val out = new ByteArrayOutputStream
    KevoreeXmiHelper.instance$.saveCompressedStream(out, model)
    out.flush ()
    val bytes  = out.toByteArray
    out.close ()
    //lastSerialization = new Date(System.currentTimeMillis)
    bytes
  }
}