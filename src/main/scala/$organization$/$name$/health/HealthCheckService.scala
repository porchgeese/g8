package $organization$.$name$.health

import cats.effect.IO
import com.itv.bucky.AmqpClient
import doobie.util.transactor.Transactor
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import doobie.implicits._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class Unhealthy(amqp: Boolean, db: Boolean)
object Unhealthy {
  implicit val decoder: Decoder[Unhealthy] = deriveDecoder[Unhealthy]
  implicit val encoder: Encoder[Unhealthy] = deriveEncoder[Unhealthy]
}

trait HealthCheckService {
  def health: IO[Either[Unhealthy, Unit]]
}

object HealthCheckService extends StrictLogging {

  def apply(amqp: AmqpClient[IO], db: Transactor[IO]): HealthCheckService = new HealthCheckService {
    def health: IO[Either[Unhealthy, Unit]] =
      for {
        _              <- IO(logger.info("Running healthcheck"))
        amqpHealth     <- amqp.isConnectionOpen
        _              <- IO(logger.info(s"Amqp health: {}", amqpHealth))
        databaseHealth <- sql"SELECT 1".query[Int].unique.transact(db).attempt
        _              <- IO(logger.info(s"Database health: {}", databaseHealth))
      } yield {
        if (amqpHealth && databaseHealth.isRight) {
          ().asRight
        } else {
          Unhealthy(amqpHealth, databaseHealth.isRight).asLeft
        }
      }
  }
}
