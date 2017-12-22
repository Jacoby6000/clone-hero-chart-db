package com.jacoby6000.cloneherodb.http.clients

import cats.effect.Effect
import com.jacoby6000.cloneherodb.data.{File, FilePath}
import com.jacoby6000.cloneherodb.filesystem.FileSystem
import org.http4s.client.Client
import fs2.Stream

class GoogleDrive[F: Effect](client: Client[F], oauth: GoogleOAuth[F]) extends FileSystem[F] {

  Stream

  override def childrenOf(filePath: FilePath) = ???

  override def parentOf(filePath: FilePath) = ???

  override def fileAt(filePath: FilePath) = ???

  override def textContents(file: File) = ???
}
