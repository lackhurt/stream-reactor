package com.datamountaineer.streamreactor.connect.azure.documentdb.sink

import com.datamountaineer.streamreactor.connect.azure.documentdb.config.DocumentDbConfig
import com.microsoft.azure.documentdb._
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

import scala.collection.JavaConversions._

class DocumentDbSinkConnectorTest extends WordSpec with Matchers with MockitoSugar {
  private val connection = "https://accountName.documents.azure.com:443/"

  "DocumentDbSinkConnector" should {
    "return one task config when one route is provided" in {
      val map = Map(
        DocumentDbConfig.DATABASE_CONFIG -> "database1",
        DocumentDbConfig.CONNECTION_CONFIG -> connection,
        DocumentDbConfig.MASTER_KEY_CONFIG -> "secret",
        DocumentDbConfig.KCQL_CONFIG -> "INSERT INTO collection1 SELECT * FROM topic1"
      )

      val documentClient = mock[DocumentClient]
      val dbResource: ResourceResponse[Database] = mock[ResourceResponse[Database]]
      when(dbResource.getResource).thenReturn(mock[Database])

      val collResource = mock[ResourceResponse[DocumentCollection]]
      when(collResource.getResource).thenReturn(mock[DocumentCollection])

      when(documentClient.readDatabase(mockEq("dbs/database1"), mockEq(null)))
        .thenReturn(dbResource)
      when(documentClient.readCollection(mockEq("dbs/database1/colls/collection1"), any(classOf[RequestOptions])))
        .thenReturn(collResource)

      val connector = new DocumentDbSinkConnector((s) => documentClient)
      connector.start(map)
      connector.taskConfigs(3).length shouldBe 1
    }

    "return one task when multiple routes are provided but maxTasks is 1" in {
      val map = Map(
        DocumentDbConfig.DATABASE_CONFIG -> "database1",
        DocumentDbConfig.CONNECTION_CONFIG -> connection,
        DocumentDbConfig.MASTER_KEY_CONFIG -> "secret",
        DocumentDbConfig.KCQL_CONFIG -> "INSERT INTO collection1 SELECT * FROM topic1; INSERT INTO coll2 SELECT * FROM topicA"
      )

      val documentClient = mock[DocumentClient]
      val dbResource = mock[ResourceResponse[Database]]
      when(dbResource.getResource).thenReturn(mock[Database])

      when(documentClient.readDatabase(mockEq("dbs/database1"), mockEq(null)))
        .thenReturn(dbResource)

      Seq("dbs/database1/colls/collection1",
        "dbs/database1/colls/coll2").foreach { c =>
        val resource = mock[ResourceResponse[DocumentCollection]]
        when(resource.getResource).thenReturn(mock[DocumentCollection])

        when(documentClient.readCollection(mockEq(c), any(classOf[RequestOptions])))
          .thenReturn(resource)
      }
      val connector = new DocumentDbSinkConnector((s) => documentClient)

      connector.start(map)
      connector.taskConfigs(1).length shouldBe 1
    }

    "return 2 configs when 3 routes are provided and maxTasks is 2" in {
      val map = Map(
        DocumentDbConfig.DATABASE_CONFIG -> "database1",
        DocumentDbConfig.CONNECTION_CONFIG -> connection,
        DocumentDbConfig.MASTER_KEY_CONFIG -> "secret",
        DocumentDbConfig.KCQL_CONFIG -> "INSERT INTO collection1 SELECT * FROM topic1;INSERT INTO coll2 SELECT * FROM topicA;INSERT INTO coll3 SELECT * FROM topicB"
      )

      val documentClient = mock[DocumentClient]
      val dbResource = mock[ResourceResponse[Database]]
      when(dbResource.getResource).thenReturn(mock[Database])

      Seq("dbs/database1/colls/collection1",
        "dbs/database1/colls/coll2",
        "dbs/database1/colls/coll3").foreach { c =>
        val resource = mock[ResourceResponse[DocumentCollection]]
        when(resource.getResource).thenReturn(mock[DocumentCollection])

        when(documentClient.readCollection(mockEq(c), any(classOf[RequestOptions])))
          .thenReturn(resource)
      }

      when(documentClient.readDatabase(mockEq("dbs/database1"), mockEq(null)))
        .thenReturn(dbResource)

      val connector = new DocumentDbSinkConnector((s) => documentClient)

      connector.start(map)
      val tasksConfigs = connector.taskConfigs(2)
      tasksConfigs.length shouldBe 2
      tasksConfigs(0).get(DocumentDbConfig.KCQL_CONFIG) shouldBe "INSERT INTO collection1 SELECT * FROM topic1;INSERT INTO coll2 SELECT * FROM topicA"
      tasksConfigs(1).get(DocumentDbConfig.KCQL_CONFIG) shouldBe "INSERT INTO coll3 SELECT * FROM topicB"
    }

    "return 3 configs when 3 routes are provided and maxTasks is 3" in {
      val map = Map(
        DocumentDbConfig.DATABASE_CONFIG -> "database1",
        DocumentDbConfig.CONNECTION_CONFIG -> connection,
        DocumentDbConfig.MASTER_KEY_CONFIG -> "secret",
        DocumentDbConfig.KCQL_CONFIG -> "INSERT INTO collection1 SELECT * FROM topic1;INSERT INTO coll2 SELECT * FROM topicA;INSERT INTO coll3 SELECT * FROM topicB"
      )

      val documentClient = mock[DocumentClient]
      val dbResource = mock[ResourceResponse[Database]]
      when(dbResource.getResource).thenReturn(mock[Database])

      Seq("dbs/database1/colls/collection1",
        "dbs/database1/colls/coll2",
        "dbs/database1/colls/coll3").foreach { c =>
        val resource = mock[ResourceResponse[DocumentCollection]]
        when(resource.getResource).thenReturn(mock[DocumentCollection])

        when(documentClient.readCollection(mockEq(c), any(classOf[RequestOptions])))
          .thenReturn(resource)
      }

      when(documentClient.readDatabase(mockEq("dbs/database1"), mockEq(null)))
        .thenReturn(dbResource)

      val connector = new DocumentDbSinkConnector((s) => documentClient)

      connector.start(map)
      val tasksConfigs = connector.taskConfigs(3)
      tasksConfigs.length shouldBe 3
      tasksConfigs(0).get(DocumentDbConfig.KCQL_CONFIG) shouldBe "INSERT INTO collection1 SELECT * FROM topic1"
      tasksConfigs(1).get(DocumentDbConfig.KCQL_CONFIG) shouldBe "INSERT INTO coll2 SELECT * FROM topicA"
      tasksConfigs(2).get(DocumentDbConfig.KCQL_CONFIG) shouldBe "INSERT INTO coll3 SELECT * FROM topicB"
    }

    "return 2 configs when 4 routes are provided and maxTasks is 2" in {
      val map = Map(
        DocumentDbConfig.DATABASE_CONFIG -> "database1",
        DocumentDbConfig.CONNECTION_CONFIG -> connection,
        DocumentDbConfig.MASTER_KEY_CONFIG -> "secret",
        DocumentDbConfig.KCQL_CONFIG -> "INSERT INTO collection1 SELECT * FROM topic1;INSERT INTO coll2 SELECT * FROM topicA;INSERT INTO coll3 SELECT * FROM topicB;INSERT INTO coll4 SELECT * FROM topicC"
      )

      val documentClient = mock[DocumentClient]
      val dbResource = mock[ResourceResponse[Database]]
      when(dbResource.getResource).thenReturn(mock[Database])

      Seq("dbs/database1/colls/collection1",
        "dbs/database1/colls/coll2",
        "dbs/database1/colls/coll3",
        "dbs/database1/colls/coll4").foreach { c =>
        val resource = mock[ResourceResponse[DocumentCollection]]
        when(resource.getResource).thenReturn(mock[DocumentCollection])

        when(documentClient.readCollection(mockEq(c), any(classOf[RequestOptions])))
          .thenReturn(resource)
      }

      when(documentClient.readDatabase(mockEq("dbs/database1"), mockEq(null)))
        .thenReturn(dbResource)

      val connector = new DocumentDbSinkConnector((s) => documentClient)

      connector.start(map)
      val tasksConfigs = connector.taskConfigs(2)
      tasksConfigs.length shouldBe 2
      tasksConfigs(0).get(DocumentDbConfig.KCQL_CONFIG) shouldBe "INSERT INTO collection1 SELECT * FROM topic1;INSERT INTO coll2 SELECT * FROM topicA"
      tasksConfigs(1).get(DocumentDbConfig.KCQL_CONFIG) shouldBe "INSERT INTO coll3 SELECT * FROM topicB;INSERT INTO coll4 SELECT * FROM topicC"
    }


  }
}
