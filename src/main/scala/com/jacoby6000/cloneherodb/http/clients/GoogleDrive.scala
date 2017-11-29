package com.jacoby6000.cloneherodb.http.clients

import cats.effect.Effect
import org.http4s.client.Client
import com.jacoby6000.cloneherodb.data._

object GoogleDrive {
  case class File(name: DirectoryName)
}

class GoogleDrive[F[_]: Effect](client: Client[F], ) {
  def listFiles(apiKey: GoogleIdFor[GoogleDrive.File]): F[List[WithId[GoogleApiKey, GoogleDrive.File]]] =
}
