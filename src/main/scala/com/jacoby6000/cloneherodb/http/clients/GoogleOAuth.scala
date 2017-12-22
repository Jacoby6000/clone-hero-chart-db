package com.jacoby6000.cloneherodb.http.clients


import argonaut._
import Argonaut._
import cats.effect.Effect
import enumeratum.{ArgonautEnum, Enum, EnumEntry}
import enumeratum.values.{StringArgonautEnum, StringEnum, StringEnumEntry}
import fs2.Stream
import java.time.Instant

import org.http4s.{EntityDecoder, Header, Headers, Method, Request, Uri}
import org.http4s.client.Client

import scala.concurrent.duration._
import scala.collection.immutable.List
import tsec.signature.core.SigAlgoTag
import tsec.signature.imports.{JCASignerPure, RSASignature, SHA256withRSA, SigPrivateKey, SignatureKeyError}
import tsec.common._

import scalaz._
import Scalaz._
import shims._

object GoogleOAuth {
  val decodeString: DecodeJson[String] = implicitly
  val encodeString: EncodeJson[String] = implicitly

  case class ProjectId(value: String) extends AnyVal
  object ProjectId {
    implicit val projectIdDecodeJson: DecodeJson[ProjectId] = decodeString.map(ProjectId(_))
  }

  case class PrivateKeyId(value: String) extends AnyVal
  object PrivateKeyId {
    implicit val privateKeyIdDecodeJson: DecodeJson[PrivateKeyId] = decodeString.map(PrivateKeyId(_))
  }

  case class EmailAddress(value: String) extends AnyVal
  object EmailAddress {
    implicit val emailAddressDecodeJson: DecodeJson[EmailAddress] =
      decodeString.map(EmailAddress(_))

    implicit val emailAddressEncodeJson: EncodeJson[EmailAddress] =
      encodeString.contramap[EmailAddress](_.value)
  }

  case class ClientId(value: String) extends AnyVal
  object ClientId {
    implicit val clientIdDecodeJson: DecodeJson[ClientId] = decodeString.map(ClientId(_))
  }

  case class AuthUri(value: String) extends AnyVal
  object AuthUri {
    implicit val authUriDecodeJson: DecodeJson[AuthUri] = decodeString.map(AuthUri(_))
  }

  case class TokenUri(value: String) extends AnyVal
  object TokenUri {
    implicit val tokenUriDecodeJson: DecodeJson[TokenUri] = decodeString.map(TokenUri(_))
  }

  case class X509CertUrl(value: String) extends AnyVal
  object X509CertUrl {
    implicit val x509CertUrlDecodeJson: DecodeJson[X509CertUrl] = decodeString.map(X509CertUrl(_))
  }

  sealed abstract class GoogleApiAccountType(val value: String) extends StringEnumEntry
  object GoogleApiAccountType extends StringEnum[GoogleApiAccountType] with StringArgonautEnum[GoogleApiAccountType] {
    case object ServiceAccount extends GoogleApiAccountType("service_account")

    val values = findValues
  }

  implicit def sigPrivateRSAKeyArgonautDecoder[A: SigAlgoTag](implicit algo: RSASignature[A]): DecodeJson[SignatureKeyError \/ SigPrivateKey[A]] =
    decodeString.map { str =>
      val rawKey =
        str
          .replaceAllLiterally("-----BEGIN PRIVATE KEY-----\n", "")
          .replaceAllLiterally("\n-----END PRIVATE KEY-----\n", "")
          .utf8Bytes

      algo.buildPrivateKey(rawKey).disjunction
     }

  case class ServiceKeyData(
    accountType: GoogleApiAccountType,
    projectId: ProjectId,
    privateKeyId: PrivateKeyId,
    privateKey: SigPrivateKey[SHA256withRSA],
    clientEmail: EmailAddress,
    clientId: ClientId,
    authUri: AuthUri,
    tokenUri: TokenUri,
    authProviderX509CertUrl: X509CertUrl,
    clientX509CertUrl: X509CertUrl
  )

  case class Scopes(scopes: List[Scope]) extends AnyVal
  object Scopes {
    implicit val scopesEncodeJson: EncodeJson[Scopes] =
      implicitly[EncodeJson[String]].contramap[Scopes](_.scopes.mkString(" "))
  }

  case class Scope(value: String) extends AnyVal

  case class OAuthTargetDescriptor(value: String) extends AnyVal
  object OAuthTargetDescriptor {
    implicit val oAuthTargetDescriptorEncodeJson: EncodeJson[OAuthTargetDescriptor] =
      encodeString.contramap[OAuthTargetDescriptor](_.value)
  }

  case class GoogleJwtClaims(iss: EmailAddress, aud: OAuthTargetDescriptor, scopes: Scopes, exp: Instant, iat: Instant)
  object GoogleJwtClaims {
    implicit val googleJwtClaimsEncodeJson: EncodeJson[GoogleJwtClaims] = {
      implicit val instantEncodeJson: EncodeJson[Instant] = EncodeJson.of[Long].contramap[Instant](_.toEpochMilli() / 1000)

      jencode5L((GoogleJwtClaims.unapply _) andThen (_.get))("iss", "aud", "scope", "exp", "iat")
    }
  }

  case class OAuthHeader(alg: String, typ: String)
  object OAuthHeader {
    implicit val oAuthHeaderEncodeJson: EncodeJson[OAuthHeader] =
      jencode2L((OAuthHeader.unapply _) andThen (_.get))("alg", "typ")
  }

  sealed trait OAuthError
  case class DurationTooLong(duration: FiniteDuration) extends OAuthError

  sealed trait TokenType extends EnumEntry
  object TokenType extends Enum[TokenType] with ArgonautEnum[TokenType] {
    object Bearer extends TokenType

    val values = findValues
  }

  case class OAuthToken(token: String, tokenType: TokenType, scopes: List[Scope], issuer: EmailAddress, expires: Instant)

  case class OAuthResponse(accessToken: String, tokenType: TokenType, expiresIn: Long)
  object OAuthResponse {
    implicit val oAuthResponseDecoder: DecodeJson[OAuthResponse] =
      jdecode3L(OAuthResponse.apply _)("access_token", "token_type", "expires_in")
  }

}

import GoogleOAuth._

class GoogleOAuth[F[_]](
  client: Client[F],
  targetDescriptor: OAuthTargetDescriptor,
  signer: JCASignerPure[F, SHA256withRSA],
  key: SigPrivateKey[SHA256withRSA])(
  implicit F: Effect[F]
) {

  import org.http4s.argonaut._

  implicit val decoder: EntityDecoder[F, OAuthResponse] = jsonOf[F, OAuthResponse]

  def requestToken(issuer: EmailAddress, scopes: List[Scope], duration: FiniteDuration): F[OAuthError \/ OAuthToken] =
    if(duration > 1.hour) F.pure(DurationTooLong(duration).left) else {
      val now = Instant.now()
      val claims = GoogleJwtClaims(issuer, targetDescriptor, Scopes(scopes), now.plusMillis(duration.toMillis), now)
      val header = OAuthHeader("RS256", "JWT")

      val encryptedClaims = signer.sign(claims.asJson.nospaces.utf8Bytes, key).map(_.toB64UrlString)
      val encryptedHeader = signer.sign(header.asJson.nospaces.utf8Bytes, key).map(_.toB64UrlString)

      val encryptedClaimsWithHeader = (encryptedHeader |@| encryptedClaims)(_ + "." + _)

      for {
        payload <- encryptedClaimsWithHeader
        signature <- signer.sign(payload.utf8Bytes, key)
        jwt = payload + "." + signature.toB64UrlString

        request = Request(
          method = Method.POST,
          headers = Headers(Header("Content-Type", "application/x-www-form-urlencoded")),
          uri = Uri(path = targetDescriptor.value),
          body = Stream(s"grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=$jwt".utf8Bytes: _*).covary[F]
        )
        response <- client.expect[OAuthResponse](request)
      } yield OAuthToken(response.accessToken, response.tokenType, scopes, issuer, claims.exp).right
    }


}
