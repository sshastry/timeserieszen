package com.timeserieszen.wal_handlers

import com.timeserieszen._

import org.scalacheck._
import scalaz._
import Scalaz._
import scalaz.stream._
import scalacheck.ScalazProperties._
import Arbitrary.arbitrary
import Prop._

object TextWALHandlerSpec extends Properties("TextWALHandler") {
  implicit def arbitrarySeriesIdent = Arbitrary( Gen.alphaStr.map(x => "a" + x.replace(" ", "")).map(SeriesIdent))

  implicit object ArbitraryApplicative extends Applicative[Gen] {
    def point[A](a: =>A) = Gen.oneOf(Seq(a))
    def ap[A,B](ga: =>Gen[A])(gf: =>Gen[A=>B]) = for {
      a <- ga
      f <- gf
    } yield (f(a))
  }

  implicit val ArbitraryDataPoint = Arbitrary(for {
    ts <- arbitrary[Long]
    numVals <- Gen.chooseNum(1,10)
    idents <- Gen.containerOfN[List,SeriesIdent](numVals, arbitrary[SeriesIdent])
    identsCleaned = idents.toSet.toList
    values <- identsCleaned.map( _ => arbitrary[Double]).sequence
  } yield DataPoint[Double](ts, identsCleaned, values))

  property("to and from file") = forAllNoShrink(arbitrary[Seq[DataPoint[Double]]])((m: Seq[DataPoint[Double]]) => {
    val f = java.io.File.createTempFile("test_text_wal_handler", ".dat")
    f.deleteOnExit()
    val wal = new TextWALHandler(f)

    if (m.size > 0) {
      //Dump to file
      Process.emitAll(m).to(wal.writer).run.run
      require(f.exists(), "file does not exist")
      //Read from file
      val result = wal.reader.runLog.run
      result == m
    } else { //Nothing will be written for no data coming in
      true
    }
  })

}
