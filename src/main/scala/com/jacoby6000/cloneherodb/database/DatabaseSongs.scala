package com.jacoby6000.cloneherodb.database

import java.time.Instant

import com.jacoby6000.cloneherodb.data.{Album, Artist, Charter, File, Genre, INIEntry, SongName, UUIDFor, Song => DataSong}
import com.jacoby6000.cloneherodb.database.DatabaseSongs.{Song, SongINIEntry}
import com.jacoby6000.cloneherodb.parsing.ini.parser.{INIKey, INISectionName, INIValue}

import scalaz.Maybe

object DatabaseSongs {
  case class Song(
    fileId: Maybe[UUIDFor[File]],
    name: SongName,
    artist: Artist,
    album: Album,
    genre: Genre,
    charter: Charter,
    lastIndexed: Instant,
    firstIndexed: Instant
  )

  case class SongINIEntry(
    songId: UUIDFor[Song],
    section: INISectionName,
    key: INIKey,
    value: INIValue,
    lastIndexed: Instant,
    firstIndexed: Instant
  )
}

trait DatabaseSongs[F[_]] {
  def insertSong(id: UUIDFor[DataSong], song: Song): F[Unit]
  def getSongById(id: UUIDFor[DataSong]): F[Maybe[Song]]
  def getSongByFile(id: UUIDFor[File]): F[Maybe[Song]]

  def insertSongINIEntry(id: UUIDFor[INIEntry], songINIEntry: SongINIEntry): F[Unit]
}
