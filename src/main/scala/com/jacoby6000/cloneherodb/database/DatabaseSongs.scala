package com.jacoby6000.cloneherodb.database

import java.time.Instant

import com.jacoby6000.cloneherodb.data.{Album, Artist, Charter, File, Genre, SongName, UUIDFor, Year, Song => DataSong}
import com.jacoby6000.cloneherodb.database.DatabaseSongs.Song
import doobie._
import doobie.postgres.implicits._
import doobie.implicits._
import syntax._
import meta._
import shims._

import scalaz._, Scalaz._

object DatabaseSongs {
  case class Song(
    fileId: UUIDFor[File],
    name: SongName,
    artist: Maybe[Artist],
    album: Maybe[Album],
    genre: Maybe[Genre],
    charter: Maybe[Charter],
    year: Maybe[Year],
    lastIndexed: Instant,
    firstIndexed: Instant
  )
}

trait DatabaseSongs[F[_]] {
  def insertSong(id: UUIDFor[DataSong], song: Song): F[Unit]
  def getSongById(id: UUIDFor[DataSong]): F[Maybe[Song]]
  def getSongByFile(id: UUIDFor[File]): F[Maybe[(UUIDFor[DataSong], Song)]]

  def updateSongByData(song: Song): F[Maybe[(UUIDFor[DataSong], Song)]]
}

class DoobieDatabaseSongs extends DatabaseSongs[ConnectionIO] {

  override def insertSong(id: UUIDFor[DataSong], song: Song): ConnectionIO[Unit] =
    insertSongQuery(id, song).runUnit

  def insertSongQuery(id: UUIDFor[DataSong], song: Song): Update0 = {
    sql"""INSERT INTO songs (id, file_id, name, genre, artist, album, charter, year, last_indexed, first_indexed) VALUES (
            $id,
            ${song.fileId},
            ${song.name},
            ${song.genre},
            ${song.artist},
            ${song.album},
            ${song.charter},
            ${song.year},
            ${song.lastIndexed},
            ${song.firstIndexed}
          )
       """.update
  }

  override def getSongById(id: UUIDFor[DataSong]): ConnectionIO[Maybe[Song]] = getSongByIdQuery(id).maybe

  def getSongByIdQuery(id: UUIDFor[DataSong]): Query0[Song] =
    sql"""SELECT
            file_id,
            name,
            artist,
            album,
            genre,
            charter,
            year,
            last_indexed,
            first_indexed
          FROM
            songs
          WHERE
            id = $id""".query[Song]

  override def getSongByFile(id: UUIDFor[File]): ConnectionIO[Maybe[(UUIDFor[DataSong], Song)]] =
    getSongByFileQuery(id).maybe

  def getSongByFileQuery(id: UUIDFor[File]): Query0[(UUIDFor[DataSong], Song)] =
    sql"""SELECT
            id,
            file_id,
            name,
            artist,
            album,
            genre,
            charter,
            year,
            last_indexed,
            first_indexed
        FROM
          songs
        WHERE
          file_id = $id""".query[(UUIDFor[DataSong], Song)]

  override def updateSongByData(song: Song): ConnectionIO[Maybe[(UUIDFor[DataSong], Song)]] = {
    updateSongByDataQuery(song).run *> getSongByFile(song.fileId)
  }


  def updateSongByDataQuery(song: Song): Update0 =
    sql"""UPDATE songs SET (
            name,
            artist,
            album,
            genre,
            charter,
            year,
            last_indexed
          ) = (
            ${song.name},
            ${song.artist},
            ${song.album},
            ${song.genre},
            ${song.charter},
            ${song.year},
            ${song.lastIndexed}
          ) WHERE
            file_id = ${song.fileId}""".update

}