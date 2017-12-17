package com.jacoby6000.cloneherodb.database

import java.time.Instant

import com.jacoby6000.cloneherodb.data.{Album, Artist, Charter, File, Genre, INIEntry, SongName, UUIDFor, Year, Song => DataSong}
import com.jacoby6000.cloneherodb.database.DatabaseSongs.{Song, SongINIEntry}
import com.jacoby6000.cloneherodb.parsing.ini.parser.{INIKey, INISectionName, INIValue}

import scalaz.Maybe

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

  def updateSongByData(song: Song): F[Maybe[(UUIDFor[DataSong], Song)]]

  def insertSongINIEntry(id: UUIDFor[INIEntry], songINIEntry: SongINIEntry): F[Unit]
}
