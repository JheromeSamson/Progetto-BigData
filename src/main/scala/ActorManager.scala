
import bean.{Actor, GitHubData}
import org.apache.spark
import org.apache.spark.{SparkConf, SparkContext, sql}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{Dataset, Row, SQLContext}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class ActorManager(val rdd : RDD[GitHubData], val SQLContext: SQLContext) {

  def Actor(): Unit = {


    var dataframe : sql.DataFrame = ActorManagerRddToDataFrame()

    val pairRdd : RDD[(String, Actor)] = ActorManagerRddToPairRdd()

    var rddActor : RDD[Actor] = ActorManagerRddToRdd()

    //var dataset : Dataset[Actor] = ActorManagerRddToDataSet(dataframe)

    //NumeroEventPresente(pairRdd)

    //NumeroEventDivisiPerTypeActor()

   // NumeroEventDivisiPerTypeActorRepoSecondi()

    MassimoEventPerSecondo()
  }


  def ActorManagerRddToDataFrame(): sql.DataFrame = {

    val schema_actor = ScalaReflection.schemaFor[Actor].dataType.asInstanceOf[StructType]

    /**  GETTING RDD */

    //getting actor rdd
    //val actor_rdd_from_JSON : Dataset[Row] = SQLContext.read.json("download\\2018-03-01-0.json").select("actor").rdd

    val actor_rdd : RDD[Actor] = rdd.map(x => x.actor)

    val actors_rows: RDD[Row] = actor_rdd.map(x => {
      Row(x.id, x.login, x.display_login, x.gravatar_id, x.url, x.avatar_url)
    })

    val df = SQLContext.createDataFrame(actors_rows, schema_actor)

    //df.show()

    return  df
  }

  def ActorManagerRddToPairRdd(): RDD[(String, Actor)] = {
    return rdd.map(x => (x.id, x.actor))
  }

  def ActorManagerRddToRdd() : RDD[Actor] = {
    return rdd.map(x => x.actor)
  }
/*
  def ActorManagerRddToDataSet(dataFrameActor : sql.DataFrame) :  Dataset[Actor] = {
    dataset = dataFrameActor.as[Actor]
    return dataset
  }
*/
  //Contare il numero di «event» (con «event» intendo l’oggeto principale del json parsato) per ogni «actor»;
  def NumeroEventPresente(rddActor: RDD[(String, Actor)]) : Unit = {
    println("Numero Di event " + rddActor.count())

    println("Numero di Attori " + rddActor.groupByKey().count())

    println("Numero di Event per Autore  " )


    rddActor.groupByKey().map(f =>(f._1, f._2.size)).foreach(x => println(x._2))

  }

  //Contare il numero di «event», divisi per «type» e «actor»;
  def NumeroEventDivisiPerTypeActor() : Unit = {
    val idEventRDD = rdd.map(x => ((x.`type`, x.actor), 1L)).reduceByKey((contatore1, contatore2) => contatore1 + contatore2)

    idEventRDD.foreach(x => println(x))
  }

  //Contare il numero di «event», divisi per «type», «actor», «repo»;
  def NumeroEventDivisiPerTypeActorRepo() : Unit  ={
    val idEventRDD  = rdd.map(x=> ((x.`type`, x.actor, x.repo), 1L)).reduceByKey((contatore1, contatore2) => contatore1 + contatore2)
    idEventRDD.foreach(x => println(x))
  }

  // Contare il numero di «event», divisi per «type», «actor», «repo» e secondo (il secondo è inteso come per tutto quel secondo. Quindi bisogna trasformare il timestamp in modo da avere solo il valore dei secondi, poi raggruppiamo solo su questo campo.);
  def NumeroEventDivisiPerTypeActorRepoSecondi() : RDD[((String,Actor,String,Int), Long)] =  {

    return rdd.map(x=> {
      ((x.`type`, x.actor, x.repo, new DateTime(x.created_at.getTime).getSecondOfMinute), 1L)
    }).reduceByKey((contatore1, contatore2) => contatore1 + contatore2)

  }

  //Trovare il massimoo numero di «event» per secondo

  def MassimoEventPerSecondo() : Unit = {
     val rdd= NumeroEventDivisiPerTypeActorRepoSecondi()

     val max = rdd.reduce((value1, value2)=> {
         if (value1._2 < value2._2) value2 else value1
       })

    println(max)
  }

  //Trovare il minimo numero di «event» per secondo
  def MinimoEventPerSecondo() : Unit = {
    val rdd= NumeroEventDivisiPerTypeActorRepoSecondi()

    val min = rdd.reduce((value1, value2)=> {
      if (value1._2 > value2._2) value2 else value1
    })
    println(min)
  }

  //Trovare il massimo numero di «event» per «actor»;
  def MassimoEventPerAttore() : Unit = {
    val idEventRDD = rdd.map(x => ((x.`type`, x.actor), 1L)).reduceByKey((contatore1, contatore2) => contatore1 + contatore2)

    val max = idEventRDD.reduce((value1, value2)=> {
      if (value1._2 > value2._2) value1 else value2
    })
  }
  //Trovare il minimo numero di «event» per «actor»;
  def MinimoEventPerAttore() : Unit = {
    val idEventRDD = rdd.map(x => ((x.`type`, x.actor), 1L)).reduceByKey((contatore1, contatore2) => contatore1 + contatore2)

    val min = idEventRDD.reduce((value1, value2)=> {
      if (value1._2 > value2._2) value2 else value1
    })
  }



/*
  def createDataFrameStruct(): StructType = StructType(Seq(
    StructField("id", StringType, true),
    StructField("login" ,StringType , true),
    StructField("display_login", StringType, true),
    StructField("gravatar_id", StringType, true),
    StructField("url", StringType, true),
    StructField("avatar_url", StringType, true)
  ))



*/
}
