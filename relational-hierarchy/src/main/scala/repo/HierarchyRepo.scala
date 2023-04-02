package repo

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import db.HierarchyDBTransactor
import zio._
import zio.interop.catz._
import helper.Helper.duration
import skunk.data.LTree

import scala.util.Random

trait HierarchyRepo {

  def hello: Task[Unit]

  def loadAdjacencyList: Task[Unit]

  def testInsertLtree: Task[Unit]
}

object HierarchyRepo {
  private type Dependencies = HierarchyDBTransactor

  val live: URLayer[Dependencies, HierarchyRepo] =
    ZLayer.fromFunction(HierarchyRepoImpl.apply _)
}

final case class HierarchyRepoImpl(hierarchy: HierarchyDBTransactor) extends HierarchyRepo {

  private val tag = "[HierarchyRepo]"

  private case class AdjacencyListUnit(
      id: Int,
      position: String,
      name: String,
      parent: Option[Int]
  )

  private case class RangeIds(l: Int, r: Int)

  case class LTreeUnit(
      id: Int,
      position: String,
      name: String,
      path: LTree
  )

  def hello: Task[Unit] = ZIO.logInfo(s"see src/main/resources/sql_queries.sql")

  override def loadAdjacencyList: Task[Unit] = duration(tag)(insertArmy())

  override def testInsertLtree: Task[Unit] = {
    val ltreeUnit = LTreeUnit(1000001, "praporshik", "praporshik1", LTree.fromString("praporshik").toOption.get)
    insertLtree(ltreeUnit)
  }

  private val defenceMinistersRange   = RangeIds(0, 1)
  private val colonelGeneralsRange    = RangeIds(1, 11)
  private val lieutenantGeneralsRange = RangeIds(11, 111)
  private val colonelsRange           = RangeIds(111, 1111)
  private val lieutenantColonelsRange = RangeIds(1111, 3111)
  private val majorsRange             = RangeIds(3111, 13111)
  private val captainsRange           = RangeIds(13111, 33111)
  private val lieutenantsRange        = RangeIds(33111, 83111)
  private val sergeantsRange          = RangeIds(83111, 183111)
  private val soldiersRange           = RangeIds(183111, 1000000)

  private val defenceMinisters   = List(AdjacencyListUnit(1, "defenseMinister", "defenseMinister", None))
  private val colonelGenerals    = buildPosition("colonelGeneral", colonelGeneralsRange, defenceMinistersRange)
  private val lieutenantGenerals = buildPosition("lieutenantGeneral", lieutenantGeneralsRange, colonelGeneralsRange)
  private val colonels           = buildPosition("colonel", colonelsRange, lieutenantGeneralsRange)
  private val lieutenantColonels = buildPosition("lieutenantColonel", lieutenantColonelsRange, colonelsRange)
  private val majors             = buildPosition("major", majorsRange, lieutenantColonelsRange)
  private val captains           = buildPosition("captain", captainsRange, majorsRange)
  private val lieutenants        = buildPosition("lieutenant", lieutenantsRange, captainsRange)
  private val sergeants          = buildPosition("sergeant", sergeantsRange, lieutenantsRange)
  private val soldiers           = buildPosition("soldier", soldiersRange, sergeantsRange)

  private def buildPosition(name: String, range: RangeIds, parentRange: RangeIds): List[AdjacencyListUnit] =
    List.range(range.l + 1, range.r + 1).map { id =>
      AdjacencyListUnit(id, name, s"${name}_${id - range.l}", Random.between(parentRange.l + 1, parentRange.r + 1).some)
    }

  private def insertArmy(): Task[Unit] = {
    val army = List(
      defenceMinisters,
      colonelGenerals,
      lieutenantGenerals,
      colonels,
      lieutenantColonels,
      majors,
      captains,
      lieutenants,
      sergeants,
      soldiers
    ).flatten
    val sql  = "insert into adjacency_list_hierarchy (id, position, name, parent) values (?, ?, ?, ?)"
    Update[AdjacencyListUnit](sql)
      .updateMany(army)
      .transact(hierarchy.xa)
      .void
  }

  implicit val ltreeMeta: Meta[LTree] = Meta[String].imap(LTree.fromString(_).toOption.get)(_.toString())

  implicit val writer: Write[LTreeUnit] =
    Write[(Int, String, String, LTree)].contramap(c => (c.id, c.position, c.name, c.path))

  private def insertLtree(ltreeUnit: LTreeUnit): Task[Unit] = {
    val sql = sql"""insert into ltree_hierarchy values (${Fragments.values(ltreeUnit)})"""
    sql.update.run.transact(hierarchy.xa).void
  }
}
