package ${package}

object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkRuntime.session("${artifactId}")

    val df = spark.range(0, 5).toDF("id")
    df.show(false)

    spark.stop()
  }
}
